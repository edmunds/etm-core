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
import com.edmunds.zookeeper.util.ZooKeeperUtils;
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
import java.util.Collection;
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
    private final ProjectProperties projectProperties;
    private final ZooKeeperTreeWatcher controllerWatcher;
    private final FailoverMonitor failoverMonitor;

    private ControllerInstance localController;
    private Set<ControllerInstance> peerControllers;
    private String controllerNodePath;
    private boolean nodeCreateInProgress;
    private boolean nodeUpdatePending;

    @Autowired
    public ControllerMonitor(ZooKeeperConnection connection,
                             ControllerPaths controllerPaths,
                             ObjectSerializer objectSerializer,
                             ProjectProperties projectProperties,
                             FailoverMonitor failoverMonitor) {
        this.connection = connection;
        this.controllerPaths = controllerPaths;
        this.objectSerializer = objectSerializer;
        this.projectProperties = projectProperties;
        this.failoverMonitor = failoverMonitor;

        // Register for failover notifications
        failoverMonitor.addListener(this);

        // Initialize zookeeper tree watcher
        ZooKeeperTreeConsistentCallback cb = new ZooKeeperTreeConsistentCallback() {
            @Override
            public void treeConsistent(ZooKeeperTreeNode oldRoot, ZooKeeperTreeNode newRoot) {
                onControllersUpdated(newRoot);
            }
        };
        this.controllerWatcher = new ZooKeeperTreeWatcher(connection, 0, controllerPaths.getConnected(), cb);

        // Initialize the set of peer ETM controllers
        this.peerControllers = Sets.newHashSet();
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {
            controllerWatcher.initialize();
            createControllerNode();
        } else if (state == ZooKeeperConnectionState.EXPIRED) {
            controllerNodePath = null;
        }
    }

    @Override
    public void onFailoverStateChanged(FailoverMonitor monitor) {
        if (nodeCreateInProgress) {
            nodeUpdatePending = true;
        } else {
            updateControllerNode();
        }
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

    protected void onControllersUpdated(ZooKeeperTreeNode rootNode) {
        Collection<ZooKeeperTreeNode> childNodes = rootNode.getChildren().values();
        Set<ControllerInstance> controllers = Sets.newHashSetWithExpectedSize(childNodes.size());

        for (ZooKeeperTreeNode node : childNodes) {
            ControllerInstance instance = bytesToControllerInstance(node.getData());
            if (instance != null) {
                controllers.add(instance);
            }
        }

        // Remove the local controller
        controllers.remove(localController);

        peerControllers = controllers;
    }

    /**
     * Creates an ephemeral node to represent this controller.
     */
    protected void createControllerNode() {

        if (controllerNodePath != null || nodeCreateInProgress) {
            return;
        }
        nodeCreateInProgress = true;

        localController = createControllerInstance();
        String nodeName = getInstanceNodeName(localController);
        String nodePath = controllerPaths.getConnectedHost(nodeName);
        byte[] data = controllerInstanceToBytes(localController);

        AsyncCallback.StringCallback cb = new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, String name) {
                onControllerNodeCreated(Code.get(rc), path, name);
            }
        };

        connection.createEphemeralSequential(nodePath, data, cb, null);
    }

    protected void onControllerNodeCreated(Code rc, String path, String name) {
        if (rc == Code.OK) {
            controllerNodePath = name;
            logger.debug(String.format("Created controller node: %s", name));
        } else if (ZooKeeperUtils.isRetryableError(rc)) {
            logger.warn(String.format("Error %s while creating controller node %s, retrying", rc, path));
            createControllerNode();
        } else {
            // Unrecoverable error
            String message = String.format("Error %s while creating controller node: %s", rc, path);
            logger.error(message);
        }

        nodeCreateInProgress = false;

        if (nodeUpdatePending) {
            updateControllerNode();
        }
    }

    protected void updateControllerNode() {
        if (controllerNodePath == null) {
            return;
        }

        localController.setFailoverState(getFailoverState());
        byte[] data = controllerInstanceToBytes(localController);

        AsyncCallback.StatCallback cb = new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, Stat stat) {
                onSetInstanceNodeData(Code.get(rc), path);
            }
        };
        connection.setData(controllerNodePath, data, -1, cb, null);
    }

    protected void onSetInstanceNodeData(Code rc, String path) {
        if (rc != Code.OK) {
            logger.error(String.format("Error %s while updating controller node: %s", rc, path));
        }
        nodeUpdatePending = false;
    }

    private FailoverState getFailoverState() {
        return failoverMonitor.getFailoverState();
    }

    private ControllerInstance createControllerInstance() {
        UUID id = UUID.randomUUID();
        String ipAddress = getIpAddress();
        String version = projectProperties.getVersion();
        return new ControllerInstance(id, ipAddress, version, getFailoverState());
    }

    private static String getInstanceNodeName(ControllerInstance instance) {
        StringBuilder sb = new StringBuilder();
        sb.append(instance.getIpAddress());
        sb.append('-');
        return sb.toString();
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
}
