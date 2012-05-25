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

import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.util.VipsBuilder;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeConsistentCallback;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeNode;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeWatcher;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.edmunds.zookeeper.connection.ZooKeeperConnectionState.INITIALIZED;

/**
 * This class monitors the load balancer vips persisted in zookeeper.
 * <p/>
 * Vip nodes are stored at the zookeeper path /etm/loadBalancer/VERSION/ENVIRONMENT/vips.
 */
@Component
public class VipMonitor implements ZooKeeperConnectionListener {

    private static final Logger logger = Logger.getLogger(VipMonitor.class);

    private final ZooKeeperConnection connection;
    private final ControllerPaths controllerPaths;
    private final ObjectSerializer objectSerializer;
    private final ZooKeeperTreeWatcher watcher;
    private final Collection<VipMonitorCallback> vipMonitorCallbacks;

    /**
     * The current root of the vips tree.
     */
    private ZooKeeperTreeNode rootNode;

    /**
     * Vips persisted in ZooKeeper.
     */
    private ManagementVips persistentVips;

    /**
     * Auto-wire constructor.
     *
     * @param connection       the ZooKeeper connection
     * @param controllerPaths  the ZooKeeper paths used by the controller
     * @param objectSerializer the object serializer
     */
    @Autowired
    public VipMonitor(ZooKeeperConnection connection,
                      ControllerPaths controllerPaths,
                      ObjectSerializer objectSerializer) {
        this.connection = connection;
        this.controllerPaths = controllerPaths;
        this.objectSerializer = objectSerializer;
        this.watcher = createWatcher();
        this.vipMonitorCallbacks = Sets.newHashSet();
    }

    /**
     * Adds a vip monitor callback to receive notification of changes to persistent vips.
     *
     * @param callback vip monitor callback
     */
    public void addCallback(VipMonitorCallback callback) {
        vipMonitorCallbacks.add(callback);
    }

    /**
     * Should be called when a connect event is received by the default watcher to start up the TreeWatcher.
     *
     * @param state the connection state
     */
    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {

        if (logger.isDebugEnabled()) {
            logger.debug("Connection state changed: " + state);
        }

        if (state == INITIALIZED) {
            watcher.initialize();
        }
    }

    /**
     * Gets the current set of persistent vips.
     *
     * @return persistent vips
     */
    public synchronized ManagementVips getPersistentVips() {
        return persistentVips;
    }

    /**
     * Sets the persistent vips.
     *
     * @param vips persistent vips
     */
    protected synchronized void setPersistentVips(ManagementVips vips) {
        persistentVips = vips;
    }

    /**
     * Creates a new zookeeper tree watcher on the vips root node.
     *
     * @return new vips tree watcher
     */
    private ZooKeeperTreeWatcher createWatcher() {
        final String rootPath = controllerPaths.getVips();
        return new ZooKeeperTreeWatcher(connection, 0, rootPath, new ZooKeeperTreeConsistentCallback() {
            @Override
            public void treeConsistent(ZooKeeperTreeNode oldRoot, ZooKeeperTreeNode newRoot) {
                setRootNode(newRoot);
            }
        });
    }

    /**
     * Called each time  vips tree changes.
     *
     * @param rootNode the root of the tree.
     */
    private void setRootNode(ZooKeeperTreeNode rootNode) {
        this.rootNode = rootNode;
        generateVips();
    }

    private void generateVips() {
        logger.debug("generateVips Called");
        if (rootNode == null) {
            return;
        }

        logger.debug("Building the vips");
        final ManagementVips currentVips = new VipsBuilder(rootNode, objectSerializer).generateVips();

        ManagementVips vips = getPersistentVips();
        if (vips == null || !vips.equals(currentVips)) {
            setPersistentVips(currentVips);
            processCallbacks();
        }
    }

    private void processCallbacks() {
        logger.debug("processCallbacks Called");

        for (VipMonitorCallback callback : vipMonitorCallbacks) {
            callback.onPersistentVipsUpdated(this);
        }
    }
}
