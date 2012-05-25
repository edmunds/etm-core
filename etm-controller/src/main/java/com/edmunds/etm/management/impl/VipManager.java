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
import com.edmunds.etm.common.thrift.ManagementVipDto;
import com.edmunds.etm.management.api.ManagementPoolMember;
import com.edmunds.etm.management.api.ManagementVip;
import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.util.VipDeltaCalculator;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static com.edmunds.zookeeper.util.ZooKeeperUtils.isRetryableError;

/**
 * Stores load balancer vips in zookeeper.
 * <p/>
 * Vip nodes are stored at the zookeeper path /etm/loadBalancer/VERSION/ENVIRONMENT/vips.
 *
 * @author Ryan Holmes
 */
@Component
public class VipManager {

    private static final Logger logger = Logger.getLogger(VipManager.class);

    private final ZooKeeperConnection connection;
    private final ControllerPaths controllerPaths;
    private final ObjectSerializer objectSerializer;
    private final VipMonitor vipMonitor;
    private final VipDeltaCalculator vipDeltaCalculator;

    @Autowired
    public VipManager(ZooKeeperConnection connection,
                      ControllerPaths controllerPaths,
                      ObjectSerializer objectSerializer,
                      VipMonitor vipMonitor,
                      VipDeltaCalculator vipDeltaCalculator) {
        this.connection = connection;
        this.controllerPaths = controllerPaths;
        this.objectSerializer = objectSerializer;
        this.vipMonitor = vipMonitor;
        this.vipDeltaCalculator = vipDeltaCalculator;
    }

    /**
     * Saves the specified vips.
     *
     * @param activeVips the active set of vips
     */
    public synchronized void persistActiveVips(ManagementVips activeVips) {
        final ManagementVips persistentVips = vipMonitor.getPersistentVips();
        final ManagementVips delta = vipDeltaCalculator.deltaLoadBalancer(persistentVips, activeVips);

        for (ManagementVip vip : delta.getVips()) {

            switch (vip.getLoadBalancerState()) {
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

        if (!vip.hasChanges()) {
            return;
        }

        // Update vip pool members
        Collection<ManagementPoolMember> deltaMembers = vip.getPoolMembers().values();
        Set<ManagementPoolMember> updatedMembers = Sets.newHashSetWithExpectedSize(deltaMembers.size());
        for (ManagementPoolMember poolMember : deltaMembers) {
            switch (poolMember.getLoadBalancerState()) {
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
        } catch (IOException e) {
            String message = "Vip serialization failed";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void createPersistentNode(String path, final byte[] data) {
        AsyncCallback.StringCallback cb = new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int rc, String p, Object ctx, String name) {
                onPersistentNodeCreated(KeeperException.Code.get(rc), p, data);
            }
        };
        connection.createPersistent(path, data, cb, null);
    }

    private void onPersistentNodeCreated(KeeperException.Code rc, String path, byte[] data) {
        if (rc == KeeperException.Code.OK) {
            logger.debug(String.format("Created load balancer node: %s", path));
        } else if (rc == KeeperException.Code.NODEEXISTS) {
            logger.warn(String.format("Attempted to create existing node %s, updating instead", path));
            setNodeData(path, data);
        } else if (isRetryableError(rc)) {
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
                onNodeDeleted(KeeperException.Code.get(rc), path);
            }
        };
        connection.delete(path, -1, cb, null);
    }

    private void onNodeDeleted(KeeperException.Code rc, String path) {
        if (rc == KeeperException.Code.OK) {
            logger.debug(String.format("Deleted node: %s", path));
        } else if (rc == KeeperException.Code.NONODE) {
            logger.warn(String.format("Node already deleted: %s", path));
        } else if (isRetryableError(rc)) {
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
                onNodeDataSet(KeeperException.Code.get(rc), p, data);
            }
        };
        connection.setData(path, data, -1, cb, null);
    }

    private void onNodeDataSet(KeeperException.Code rc, String path, byte[] data) {

        if (rc == KeeperException.Code.OK) {
            logger.debug(String.format("Data set for node: %s", path));
        } else if (rc == KeeperException.Code.NONODE) {
            logger.warn(String.format("Attempted to update nonexistent node %s, creating instead", path));
        } else if (isRetryableError(rc)) {
            logger.warn(String.format("Error %s while setting data for node %s, retrying", rc, path));
            setNodeData(path, data);
        } else {
            // Unrecoverable error
            String message = String.format("Error %s while setting data for node: %s", rc, path);
            logger.error(message);
            // TODO: Flag error in central error reporter
        }
    }
}
