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
package com.edmunds.etm.management.impl;

import com.edmunds.etm.common.api.AgentPaths;
import com.edmunds.etm.common.api.ClientPaths;
import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.rules.impl.UrlTokenMonitor;
import com.edmunds.etm.system.impl.AgentMonitor;
import com.edmunds.etm.system.impl.ControllerMonitor;
import com.edmunds.etm.system.impl.FailoverMonitor;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperNodeInitializer;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * The main ETM controller class, responsible for initializating the application.
 * <p/>
 * This class sets up the ZooKeeper node structure used throughout the system and connects to the ZooKeeper server.
 *
 * @author Ryan Holmes
 * @author David Trott
 */
@Component
public class EtmController implements InitializingBean, DisposableBean {

    private static final long ZOOKEEPER_CONNECTION_CLOSE_WAIT = 1000;

    private static final Logger logger = Logger.getLogger(EtmController.class);

    /**
     * The ZooKeeperConnection.
     */
    private final ZooKeeperConnection connection;
    private final ControllerPaths controllerPaths;
    private final ClientPaths clientPaths;
    private final AgentPaths agentPaths;
    private final ClientMonitor clientMonitor;
    private final VipMonitor vipMonitor;
    private final FailoverMonitor failoverMonitor;
    private final AgentMonitor agentMonitor;
    private final ControllerMonitor controllerMonitor;
    private final UrlTokenMonitor urlTokenMonitor;

    /**
     * Has the system been initialized.
     */
    private volatile boolean etmInitialized;

    /**
     * Autowired constructor.
     *
     * @param connection        the ZooKeeper connection
     * @param controllerPaths   paths for the controller
     * @param clientPaths       paths for the clients
     * @param agentPaths        paths for the agents
     * @param clientMonitor     client manager
     * @param vipMonitor        vip manager
     * @param failoverMonitor   failover monitor
     * @param agentMonitor      agent monitor
     * @param controllerMonitor etm controller monitor
     * @param urlTokenMonitor   url token monitor
     */
    @Autowired
    public EtmController(
            ZooKeeperConnection connection,
            ControllerPaths controllerPaths,
            ClientPaths clientPaths,
            AgentPaths agentPaths,
            ClientMonitor clientMonitor,
            VipMonitor vipMonitor,
            FailoverMonitor failoverMonitor,
            AgentMonitor agentMonitor,
            ControllerMonitor controllerMonitor,
            UrlTokenMonitor urlTokenMonitor) {
        this.connection = connection;
        this.controllerPaths = controllerPaths;
        this.clientPaths = clientPaths;
        this.agentPaths = agentPaths;
        this.clientMonitor = clientMonitor;
        this.vipMonitor = vipMonitor;
        this.failoverMonitor = failoverMonitor;
        this.agentMonitor = agentMonitor;
        this.controllerMonitor = controllerMonitor;
        this.urlTokenMonitor = urlTokenMonitor;
    }

    /**
     * Registers to received zookeeper connect events.
     *
     * @throws Exception is not thrown, declared to maintain interface for subclasses.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (etmInitialized) {
            final String msg = "ETM Critical Error: Double initialization detected - Shutting down the JVM";
            System.err.println(msg);
            logger.fatal(msg);
            System.exit(1);
            // We have a security manager.
            throw new Error(msg);
        }

        etmInitialized = true;
        logger.info("********************************************");
        logger.info("*** Edmunds Traffic Manager Initializing ***");
        logger.info("********************************************");

        connection.addInitializer(new ZooKeeperNodeInitializer(getPaths()));
        connection.addListener(clientMonitor);
        connection.addListener(vipMonitor);
        connection.addListener(failoverMonitor);
        connection.addListener(agentMonitor);
        connection.addListener(controllerMonitor);
        connection.addListener(urlTokenMonitor);
        connection.connect();
    }

    @Override
    public void destroy() throws Exception {

        logger.info("*** Edmunds Traffic Manager Shutting Down ***");

        // Close the ZooKeeper connection
        if (connection != null) {
            connection.close();
            Thread.sleep(ZOOKEEPER_CONNECTION_CLOSE_WAIT);
        }
    }

    /**
     * Returns the list of paths needed by the controller.
     *
     * @return the structural paths to be built.
     */
    private List<String> getPaths() {
        final List<String> list = Lists.newArrayList();

        list.addAll(controllerPaths.getStructuralPaths());
        list.addAll(clientPaths.getStructuralPaths());
        list.addAll(agentPaths.getStructuralPaths());

        return Collections.unmodifiableList(list);
    }
}
