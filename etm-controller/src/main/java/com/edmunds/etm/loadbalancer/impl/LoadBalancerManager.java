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

import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.common.thrift.ManagementVipDto;
import com.edmunds.etm.management.api.ManagementPoolMember;
import com.edmunds.etm.management.api.ManagementVip;
import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.impl.ClientMonitor;
import com.edmunds.etm.management.impl.ClientMonitorCallback;
import com.edmunds.etm.management.impl.VipMonitor;
import com.edmunds.etm.management.impl.VipMonitorCallback;
import com.edmunds.etm.management.util.VipDeltaCalculator;
import com.edmunds.etm.system.api.FailoverListener;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.etm.system.impl.FailoverMonitor;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.edmunds.zookeeper.util.ZooKeeperUtils.isRetryableError;

/**
 * Primary class for handling the ZooKeeper state associated with the load balancer.
 *
 * @author David Trott
 * @author Ryan Holmes
 */
@Component
public class LoadBalancerManager implements ClientMonitorCallback, VipMonitorCallback,
    LoadBalancerControllerCallback, FailoverListener {

    private static final Logger logger = Logger.getLogger(LoadBalancerManager.class);


    private final ClientMonitor clientMonitor;
    private final VipMonitor vipMonitor;
    private final FailoverMonitor failoverMonitor;
    private final VipDeltaCalculator vipDeltaCalculator;
    private final ZooKeeperConnection zooKeeperConnection;
    private final ControllerPaths controllerPaths;
    private final ObjectSerializer objectSerializer;
    private final ConfigurationDeploymentExecutor deploymentExecutor;

    private LoadBalancerController loadBalancerController;

    private boolean clientVipsInitialized;
    private boolean activeVipsInitialized;
    private boolean persistentVipsInitialized;

    /**
     * Indicates whether load balancer data should be verified on the next update.
     */
    private boolean verificationRequired;


    @Autowired
    public LoadBalancerManager(
        ClientMonitor clientMonitor,
        VipMonitor vipMonitor,
        FailoverMonitor failoverMonitor,
        VipDeltaCalculator vipDeltaCalculator,
        ZooKeeperConnection zooKeeperConnection,
        ControllerPaths controllerPaths,
        ObjectSerializer objectSerializer) {

        this.clientMonitor = clientMonitor;
        this.vipMonitor = vipMonitor;
        this.failoverMonitor = failoverMonitor;
        this.vipDeltaCalculator = vipDeltaCalculator;
        this.zooKeeperConnection = zooKeeperConnection;
        this.controllerPaths = controllerPaths;
        this.objectSerializer = objectSerializer;
        this.deploymentExecutor = new ConfigurationDeploymentExecutor();
        this.verificationRequired = true;

        // Register for notifications
        clientMonitor.addCallback(this);
        vipMonitor.addCallback(this);
        failoverMonitor.addListener(this);
    }

    @Autowired
    public void setLoadBalancerController(LoadBalancerController loadBalancerController) {
        this.loadBalancerController = loadBalancerController;
        loadBalancerController.addCallback(this);
    }

    @Override
    public void onClientVipsUpdated(ClientMonitor monitor) {
        logger.debug("clientVipsUpdated() called");
        clientVipsInitialized = true;
        updateLoadBalancer();
    }

    @Override
    public void onPersistentVipsUpdated(VipMonitor monitor) {
        logger.debug("vipsUpdated() called");
        persistentVipsInitialized = true;

        // Update load balancer using persisted vips if active vips have not been initialized
        if(!activeVipsInitialized) {
            updateLoadBalancer();
        }

    }

    @Override
    public void onActiveVipsUpdated(LoadBalancerController controller) {
        logger.debug("activeVipsUpdated() called");
        activeVipsInitialized = true;
        persistActiveVips(controller.getActiveVips());
    }

    @Override
    public void onFailoverStateChanged(FailoverMonitor monitor) {
        // Check for activation
        if(monitor.getFailoverState() == FailoverState.ACTIVE) {
            updateLoadBalancer();
        } else {
            verificationRequired = true;
        }
    }

    /**
     * Updates the load balancer configuration based on the current set of client vips.
     */
    public void updateLoadBalancer() {
        boolean vipsInitialized = clientVipsInitialized && (activeVipsInitialized || persistentVipsInitialized);
        if(!vipsInitialized || getFailoverState() != FailoverState.ACTIVE) {
            return;
        }

        boolean verify;
        if(verificationRequired) {
            verify = true;
            verificationRequired = false;
        } else {
            verify = false;
        }

        deploymentExecutor.execute(new ConfigurationDeploymentTask(verify));
    }


    /**
     * Returns the delta between the set of vips configured in the load balancer and vips that represent the connected
     * client applications.
     *
     * @return delta between active and client vips
     */
    protected ManagementVips getClientDeltaVips() {
        ManagementVips activeVips = getActiveVips();
        ManagementVips currentVips = activeVips != null ? activeVips : getPersistentVips();
        return vipDeltaCalculator.deltaConnections(currentVips, getClientVips());
    }

    private ManagementVips getClientVips() {
        return clientMonitor.getClientVips();
    }

    private ManagementVips getPersistentVips() {
        return vipMonitor.getPersistentVips();
    }

    private ManagementVips getActiveVips() {
        return loadBalancerController.getActiveVips();
    }

    private FailoverState getFailoverState() {
        return failoverMonitor.getFailoverState();
    }

    private void persistActiveVips(ManagementVips vips) {
        final ManagementVips delta = vipDeltaCalculator.deltaLoadBalancer(getPersistentVips(), vips);

        for(ManagementVip vip : delta.getMavenModuleVips()) {

            switch(vip.getLoadBalancerState()) {
                case CREATE_REQUEST:
                    createVip(vip);
                    break;
                case DELETE_REQUEST:
                    deleteVip(vip);
                    break;
                case ACTIVE:
                    updateVip(vip);
                    break;
                default:
                    throw new IllegalStateException("Unexpected State: " + vip.getLoadBalancerState());
            }
        }
    }

    private String getVipNodePath(ManagementVip vip) {
        return controllerPaths.getVip(vip.getMavenModule().toString());
    }

    private void createVip(ManagementVip vip) {
        final String vipPath = getVipNodePath(vip);
        ManagementVipDto vipDto = ManagementVip.writeDto(vip);
        createPersistentNode(vipPath, vipDtoToBytes(vipDto));
    }

    private void deleteVip(ManagementVip vip) {
        final String vipPath = getVipNodePath(vip);
        deleteNode(vipPath);
    }

    private void updateVip(ManagementVip vip) {

        if(!vip.hasChanges()) {
            return;
        }

        // Update vip pool members
        Collection<ManagementPoolMember> deltaMembers = vip.getPoolMembers().values();
        Set<ManagementPoolMember> updatedMembers = Sets.newHashSetWithExpectedSize(deltaMembers.size());
        for(ManagementPoolMember poolMember : deltaMembers) {
            switch(poolMember.getLoadBalancerState()) {
                case CREATE_REQUEST:
                    updatedMembers.add(poolMember);
                    break;
                case ACTIVE:
                    updatedMembers.add(poolMember);
                    break;
                case DELETE_REQUEST:
                    break;
                default:
                    throw new IllegalStateException("Unexpected State: " + vip.getLoadBalancerState());
            }
        }

        // Write the new vip
        ManagementVip updatedVip = new ManagementVip(vip.getLoadBalancerState(), vip.getMavenModule(),
            vip.getHostAddress(), updatedMembers, vip.getRootContext(), vip.getRules(), vip.getHttpMonitor());
        final String vipPath = getVipNodePath(vip);
        ManagementVipDto vipDto = ManagementVip.writeDto(updatedVip);
        setNodeData(vipPath, vipDtoToBytes(vipDto));
    }

    private byte[] vipDtoToBytes(ManagementVipDto vipDto) {
        try {
            return objectSerializer.writeValue(vipDto);
        } catch(IOException e) {
            String message = "Vip serialization failed";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void createPersistentNode(String path, final byte[] data) {
        AsyncCallback.StringCallback cb = new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int rc, String p, Object ctx, String name) {
                onPersistentNodeCreated(Code.get(rc), p, data);
            }
        };
        zooKeeperConnection.createPersistent(path, data, cb, null);
    }

    private void onPersistentNodeCreated(Code rc, String path, byte[] data) {
        if(rc == Code.OK) {
            logger.debug(String.format("Created load balancer node: %s", path));
        } else if(rc == Code.NODEEXISTS) {
            logger.warn(String.format("Attempted to create existing node %s, updating instead", path));
            setNodeData(path, data);
        } else if(isRetryableError(rc)) {
            logger.warn(String.format("Error %s while creating node %s, retrying", rc, path));
            createPersistentNode(path, data);
        } else {
            // Unrecoverable error
            String message = String.format("Error %s while creating node: %s", rc, path);
            logger.error(message);
            // TODO: Flag error in central error reporter
        }
    }

    private void deleteNode(String path) {
        AsyncCallback.VoidCallback cb = new AsyncCallback.VoidCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx) {
                onNodeDeleted(Code.get(rc), path);
            }
        };
        zooKeeperConnection.delete(path, -1, cb, null);
    }

    private void onNodeDeleted(Code rc, String path) {
        if(rc == Code.OK) {
            logger.debug(String.format("Deleted node: %s", path));
        } else if(rc == Code.NONODE) {
            logger.warn(String.format("Node already deleted: %s", path));
        } else if(isRetryableError(rc)) {
            logger.warn(String.format("Error %s while deleting node %s, retrying", rc, path));
            deleteNode(path);
        } else {
            // Unrecoverable error
            String message = String.format("Error %s while deleting node: %s", rc, path);
            logger.error(message);
            // TODO: Flag error in central error reporter
        }
    }

    private void setNodeData(final String path, final byte[] data) {
        AsyncCallback.StatCallback cb = new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int rc, String p, Object ctx, Stat stat) {
                onNodeDataSet(Code.get(rc), p, data);
            }
        };
        zooKeeperConnection.setData(path, data, -1, cb, null);
    }

    private void onNodeDataSet(Code rc, String path, byte[] data) {

        if(rc == Code.OK) {
            logger.debug(String.format("Data set for node: %s", path));
        } else if(rc == Code.NONODE) {
            logger.warn(String.format("Attempted to update nonexistent node %s, creating instead", path));
        } else if(isRetryableError(rc)) {
            logger.warn(String.format("Error %s while setting data for node %s, retrying", rc, path));
            setNodeData(path, data);
        } else {
            // Unrecoverable error
            String message = String.format("Error %s while setting data for node: %s", rc, path);
            logger.error(message);
            // TODO: Flag error in central error reporter
        }
    }


    /**
     * Coordinates the deployment of load balancer configurations. <p/> This class ensures that only one configuration
     * deployment runs at a time
     */
    private class ConfigurationDeploymentExecutor implements Executor {
        Runnable pending;
        Runnable active;

        public synchronized void execute(final Runnable r) {
            logger.debug("Queueing new load balancer configuration");
            pending = new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            };

            if(active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            active = pending;
            if(active != null) {
                logger.debug("Starting new load balancer configuration");
                pending = null;
                new Thread(active).start();
            }
        }
    }

    /**
     * Encapsulates a configuration deployment task.
     */
    private class ConfigurationDeploymentTask implements Runnable {
        private final boolean verify;

        ConfigurationDeploymentTask(boolean verify) {
            this.verify = verify;
        }

        @Override
        public void run() {
            ManagementVips deltaVips = getClientDeltaVips();
            loadBalancerController.updateLoadBalancerConfiguration(deltaVips, verify);
        }
    }
}
