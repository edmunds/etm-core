/*
 * Copyright 2011 Edmunds.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.edmunds.etm.loadbalancer.impl;

import com.edmunds.etm.management.api.ManagementLoadBalancerState;
import com.edmunds.etm.management.api.ManagementVip;
import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.impl.ClientMonitor;
import com.edmunds.etm.management.impl.ClientMonitorCallback;
import com.edmunds.etm.management.impl.VipManager;
import com.edmunds.etm.management.impl.VipMonitor;
import com.edmunds.etm.management.util.VipDeltaCalculator;
import com.edmunds.etm.rules.impl.WebConfigurationManager;
import com.edmunds.etm.system.api.FailoverListener;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.etm.system.impl.AgentMonitor;
import com.edmunds.etm.system.impl.FailoverMonitor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The LoadBalancerManager coordinates the deployment of load balancer and web proxy configuration changes.
 * It is also responsible for storing the current configuration state in ZooKeeper.
 *
 * @author David Trott
 * @author Ryan Holmes
 */
@Component
public class LoadBalancerManager
        implements ClientMonitorCallback, FailoverListener, DisposableBean {

    private static final Logger logger = Logger.getLogger(LoadBalancerManager.class);
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT = 10000;

    private final ClientMonitor clientMonitor;
    private final VipMonitor vipMonitor;
    private final VipManager vipManager;
    private final VipDeltaCalculator vipDeltaCalculator;
    private final AgentMonitor agentMonitor;
    private final FailoverMonitor failoverMonitor;

    private final ExecutorService taskExecutor;

    private LoadBalancerController loadBalancerController;
    private WebConfigurationManager webConfigurationManager;

    private ManagementVips activeVips;
    private boolean clientVipsInitialized;

    /**
     * Indicates whether load balancer data should be validated on the next update.
     */
    private boolean validationRequired;

    @Autowired
    public LoadBalancerManager(
            ClientMonitor clientMonitor,
            VipMonitor vipMonitor,
            VipManager vipManager,
            VipDeltaCalculator vipDeltaCalculator,
            AgentMonitor agentMonitor,
            FailoverMonitor failoverMonitor) {

        this.clientMonitor = clientMonitor;
        this.vipMonitor = vipMonitor;
        this.vipManager = vipManager;
        this.vipDeltaCalculator = vipDeltaCalculator;
        this.agentMonitor = agentMonitor;
        this.failoverMonitor = failoverMonitor;
        this.taskExecutor = new SingletonQueueExecutor();
        this.validationRequired = true;

        // Register for notifications
        clientMonitor.addCallback(this);
        failoverMonitor.addListener(this);
    }

    @Autowired
    public void setLoadBalancerController(LoadBalancerController loadBalancerController) {
        this.loadBalancerController = loadBalancerController;
    }

    @Autowired
    public void setWebConfigurationManager(WebConfigurationManager webConfigurationManager) {
        this.webConfigurationManager = webConfigurationManager;
    }

    @Override
    public void onClientVipsUpdated(ClientMonitor monitor) {
        logger.debug("clientVipsUpdated() called");
        clientVipsInitialized = true;
        updateLoadBalancer();
    }

    @Override
    public void onFailoverStateChanged(FailoverMonitor monitor) {
        // Check for activation
        if (monitor.getFailoverState() == FailoverState.ACTIVE) {
            updateLoadBalancer();
        } else {
            validationRequired = true;
        }
    }

    /**
     * Updates the load balancer configuration based on the current set of client vips.
     */
    public void updateLoadBalancer() {
        if (!clientVipsInitialized || getFailoverState() != FailoverState.ACTIVE) {
            return;
        }

        boolean validate;
        if (validationRequired) {
            validate = true;
            validationRequired = false;
        } else {
            validate = false;
        }

        taskExecutor.execute(new ConfigurationDeploymentTask(validate));
    }

    @Override
    public void destroy() throws Exception {

        // Shut down the callback scheduler
        if (taskExecutor != null) {
            taskExecutor.shutdown();
            taskExecutor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Gets the set of vips currently active in the load balancer.
     *
     * @return active vips
     */
    public synchronized ManagementVips getActiveVips() {
        return activeVips;
    }

    /**
     * Sets the active load balancer vips.
     *
     * @param activeVips active vips
     */
    public synchronized void setActiveVips(ManagementVips activeVips) {
        this.activeVips = activeVips;
    }

    protected ManagementVips getActiveOrPersistedVips() {
        ManagementVips vips = getActiveVips();
        if (vips == null) {
            vips = vipMonitor.getPersistentVips();
        }
        return vips;
    }

    private FailoverState getFailoverState() {
        return failoverMonitor.getFailoverState();
    }

    /**
     * Encapsulates a configuration deployment task.
     */
    private class ConfigurationDeploymentTask implements Runnable {
        private final boolean validate;

        ConfigurationDeploymentTask(boolean validate) {
            this.validate = validate;
        }

        @Override
        public void run() {

            // get the delta between active vips and online clients
            ManagementVips clientVips = clientMonitor.getClientVips();
            ManagementVips lbVips = getActiveOrPersistedVips();
            ManagementVips deltaVips = vipDeltaCalculator.deltaConnections(lbVips, clientVips);

            // extract deleted vips to apply later
            Collection<ManagementVip> deletedVips;
            deletedVips = deltaVips.getVipsWithLoadBalancerState(ManagementLoadBalancerState.DELETE_REQUEST);

            // add new vips and modify active vips on the load balancer
            lbVips = loadBalancerController.updateLoadBalancerConfiguration(deltaVips, validate, false);
            if (lbVips == null) {
                logger.error("Error updating load balancer, aborting configuration task");
                return;
            }

            // update web proxy rules including vip deletions
            ManagementVips proxyVips = lbVips.removeAll(deletedVips);
            Set<String> ruleSetDigests = webConfigurationManager.updateConfiguration(proxyVips);

            // wait for the new rule set to be deployed
            agentMonitor.waitForRuleSetDeployment(ruleSetDigests);

            // remove deleted vips from the load balancer
            lbVips = loadBalancerController.updateLoadBalancerConfiguration(lbVips, false, true);

            // update the persisted vips
            if (lbVips != null) {
                setActiveVips(lbVips);
                vipManager.persistActiveVips(lbVips);
            }
        }
    }
}
