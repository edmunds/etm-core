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
package com.edmunds.etm.system.impl;

import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.common.thrift.ControllerInstanceDto;
import com.edmunds.etm.system.api.ControllerInstance;
import com.edmunds.etm.system.api.FailoverListener;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeConsistentCallback;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeNode;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeWatcher;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Monitors all connected ETM controllers and maintains the connected node for this controller.
 *
 * @author Ryan Holmes
 */
@Component
public class ControllerMonitor implements ZooKeeperConnectionListener, FailoverListener {

    private static final Logger logger = Logger.getLogger(ControllerMonitor.class);

    private final ZooKeeperConnection connection;
    private final ControllerPaths controllerPaths;
    private final ObjectSerializer objectSerializer;
    private final ZooKeeperTreeWatcher controllerWatcher;
    private final FailoverMonitor failoverMonitor;
    private final ControllerInstance localController;
    private final Callback callbackInstance;

    private volatile ControllerInstance storedLocalController;
    private volatile Set<ControllerInstance> peerControllers;

    @Autowired
    public ControllerMonitor(ZooKeeperConnection connection,
                             ControllerPaths controllerPaths,
                             ObjectSerializer objectSerializer,
                             ProjectProperties projectProperties,
                             FailoverMonitor failoverMonitor) {
        this.connection = connection;
        this.controllerPaths = controllerPaths;
        this.objectSerializer = objectSerializer;
        this.failoverMonitor = failoverMonitor;

        this.localController = createControllerInstance(projectProperties);
        this.callbackInstance = new Callback();
        this.peerControllers = Collections.emptySet();

        // Register for failover notifications
        failoverMonitor.addListener(this);

        // Initialize zookeeper tree watcher
        this.controllerWatcher = new ZooKeeperTreeWatcher(
                connection, 0, controllerPaths.getConnected(), callbackInstance);
    }

    /**
     * Gets the ETM controller instance that represents this controller.
     *
     * @return local ETM controller instance
     */
    public ControllerInstance getLocalController() {
        return localController;
    }

    /**
     * Gets the set of connected peer ETM controllers.
     *
     * @return peer ETM controllers
     */
    public Set<ControllerInstance> getPeerControllers() {
        return peerControllers;
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {
            controllerWatcher.initialize();
            executeOperation(Operation.create, false);
        }
    }

    @Override
    public void onFailoverStateChanged(FailoverMonitor monitor) {
        localController.setFailoverState(failoverMonitor.getFailoverState());
        synchronizeZookeeper(true);
    }

    protected void onControllersUpdated(ZooKeeperTreeNode rootNode) {
        ControllerInstance local = null;
        Set<ControllerInstance> peers = Sets.newHashSet();

        for (ZooKeeperTreeNode node : rootNode.getChildren().values()) {
            ControllerInstance instance = bytesToControllerInstance(node.getData());

            if (localController.equals(instance)) {
                local = instance;
            } else if (instance != null) {
                peers.add(instance);
            }
        }

        this.storedLocalController = local;
        this.peerControllers = peers;

        synchronizeZookeeper(true);
    }

    private void synchronizeZookeeper(boolean retry) {
        final ControllerInstance stored = this.storedLocalController;

        if (stored != null && stored.getFailoverState() != localController.getFailoverState()) {
            executeOperation(Operation.update, retry);
        }
    }

    private FailoverState getFailoverState() {
        return failoverMonitor.getFailoverState();
    }

    private ControllerInstance createControllerInstance(ProjectProperties projectProperties) {
        UUID id = UUID.randomUUID();
        String ipAddress = getIpAddress();
        String version = projectProperties.getVersion();
        return new ControllerInstance(id, ipAddress, version, getFailoverState());
    }

    private static String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            String message = "Could not get IP address for localhost";
            logger.error(message, e);
            throw new RuntimeException(e);
        }
    }

    private String getLocalControllerNodeName() {
        return localController.getIpAddress() + "-" + localController.getId().toString();
    }

    private byte[] controllerInstanceToBytes(ControllerInstance instance) {
        ControllerInstanceDto dto = ControllerInstance.writeDto(instance);
        byte[] data = new byte[0];
        try {
            data = objectSerializer.writeValue(dto);
        } catch (IOException e) {
            logger.error(String.format("Failed to serialize ControllerInstanceDto: %s", dto), e);
        }

        return data;
    }

    private ControllerInstance bytesToControllerInstance(byte[] data) {
        ControllerInstanceDto dto = null;
        try {
            dto = objectSerializer.readValue(data, ControllerInstanceDto.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize ControllerInstanceDto", e);
        }

        return ControllerInstance.readDto(dto);
    }

    private void processCallback(int rc, String path, Object retry) {
        final Code code = Code.get(rc);

        switch (code) {
            case OK:
                synchronizeZookeeper(true);
                break;
            case NONODE:
            case NODEEXISTS:
                logger.warn(String.format("Error %s while updating controller node %s, rescanning", code, path));
                if (Boolean.TRUE.equals(retry)) {
                    // Only retry once.
                    synchronizeZookeeper(false);
                }
                break;
            default:
                // Unrecoverable error
                logger.error(String.format("Error %s while updating controller node: %s", code, path));
                break;
        }
    }

    public void shutdown() {
        storedLocalController = null;
        executeOperation(Operation.delete, false);
    }

    private void executeOperation(Operation operation, boolean retry) {
        final String nodePath = controllerPaths.getConnectedHost(getLocalControllerNodeName());
        final byte[] data = controllerInstanceToBytes(localController);

        switch (operation) {
            case create:
                connection.createEphemeral(nodePath, data, callbackInstance, retry);
                break;
            case update:
                connection.setData(nodePath, data, -1, callbackInstance, retry);
                break;
            case delete:
                connection.delete(nodePath, -1, callbackInstance, retry);
                break;
            default:
                break;
        }
    }

    private enum Operation {
        create,
        update,
        delete;

    }

    private class Callback implements
            ZooKeeperTreeConsistentCallback,
            AsyncCallback.StringCallback,
            AsyncCallback.StatCallback,
            AsyncCallback.VoidCallback {

        // State read from zookeeper.
        @Override
        public void treeConsistent(ZooKeeperTreeNode oldRoot, ZooKeeperTreeNode newRoot) {
            onControllersUpdated(newRoot);
        }

        // Node created.
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            processCallback(rc, path, ctx);
        }

        // Node updated.
        @Override
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            processCallback(rc, path, ctx);
        }

        // Node deleted
        @Override
        public void processResult(int rc, String path, Object ctx) {
            processCallback(rc, path, ctx);
        }
    }
}
