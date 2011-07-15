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

import com.edmunds.etm.common.api.AgentInstance;
import com.edmunds.etm.common.api.AgentPaths;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.common.thrift.AgentInstanceDto;
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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Monitors all connected agents.
 *
 * @author Ryan Holmes
 */
@Component
public class AgentMonitor implements ZooKeeperConnectionListener {

    private static final Logger logger = Logger.getLogger(AgentMonitor.class);

    private final ZooKeeperTreeWatcher agentWatcher;
    private final ObjectSerializer objectSerializer;

    private Set<AgentInstance> connectedAgents;

    @Autowired
    public AgentMonitor(ZooKeeperConnection connection,
                        AgentPaths agentPaths,
                        ObjectSerializer objectSerializer) {
        ZooKeeperTreeConsistentCallback cb = new ZooKeeperTreeConsistentCallback() {
            @Override
            public void treeConsistent(ZooKeeperTreeNode oldRootNode, ZooKeeperTreeNode newRootNode) {
                onAgentsUpdated(newRootNode);
            }
        };
        this.agentWatcher = new ZooKeeperTreeWatcher(connection, 0, agentPaths.getConnected(), cb);

        this.objectSerializer = objectSerializer;
        this.connectedAgents = Sets.newHashSet();
    }

    /**
     * Gets the set of all connected agents.
     *
     * @return set of all connected agents
     */
    public Set<AgentInstance> getConnectedAgents() {
        return connectedAgents;
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {
            agentWatcher.initialize();
        }
    }

    protected void onAgentsUpdated(ZooKeeperTreeNode rootNode) {
        Collection<ZooKeeperTreeNode> childNodes = rootNode.getChildren().values();

        Set<AgentInstance> agents = Sets.newHashSetWithExpectedSize(childNodes.size());
        for (ZooKeeperTreeNode treeNode : rootNode.getChildren().values()) {
            AgentInstance instance = bytesToAgentInstance(treeNode.getData());
            if (instance != null) {
                agents.add(instance);
            }
        }

        connectedAgents = agents;
    }

    private AgentInstance bytesToAgentInstance(byte[] data) {
        AgentInstanceDto dto = null;
        try {
            dto = objectSerializer.readValue(data, AgentInstanceDto.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize EtmControllerInstanceDto", e);
        }

        return AgentInstance.readDto(dto);
    }
}
