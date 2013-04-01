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

    private static final long DEFAULT_RULE_SET_DEPLOYMENT_TIMEOUT = 5000;
    private static final Logger logger = Logger.getLogger(AgentMonitor.class);

    private final ZooKeeperTreeWatcher agentWatcher;
    private final ObjectSerializer objectSerializer;

    private Set<AgentInstance> connectedAgents;
    private long ruleSetDeploymentTimeout;

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
        this.ruleSetDeploymentTimeout = DEFAULT_RULE_SET_DEPLOYMENT_TIMEOUT;
    }

    /**
     * Gets the set of all connected agents.
     *
     * @return set of all connected agents
     */
    public Set<AgentInstance> getConnectedAgents() {
        return connectedAgents;
    }

    /**
     * Gets the rule set deployment timeout in milliseconds.
     *
     * @return milliseconds to wait for a rule set to be deployed to a single proxy server
     */
    public long getRuleSetDeploymentTimeout() {
        return ruleSetDeploymentTimeout;
    }

    /**
     * Set the rule set deployment timeout in milliseconds.
     *
     * @param ruleSetDeploymentTimeout milliseconds to wait for deployment to a single proxy server
     */
    public void setRuleSetDeploymentTimeout(long ruleSetDeploymentTimeout) {
        this.ruleSetDeploymentTimeout = ruleSetDeploymentTimeout;
    }

    /**
     * Waits for the specified rule set to be deployed to all web proxy agents.
     *
     * @param ruleSetDigests digest of the rule set to wait for
     * @return true when the rule set has been deployed, false when the timeout has been exceeded
     */
    public boolean waitForRuleSetDeployment(Set<String> ruleSetDigests) {
        long timeout = ruleSetDeploymentTimeout * getConnectedAgents().size();
        long startTime = System.currentTimeMillis();
        boolean timeoutExpired;
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("Interruped while waiting for rule set deployment", e);
            }
            long currentTime = System.currentTimeMillis();
            timeoutExpired = (currentTime - startTime) < timeout;
        } while (!isRuleSetDeployed(ruleSetDigests) && !timeoutExpired);
        return !timeoutExpired;
    }

    /**
     * Indicates whether the specified rule set has been deployed to all web proxy agents.
     *
     * @param ruleSetDigests digest of the rule set to check
     * @return true if the rule set is deployed to all agents, false otherwise
     */
    public boolean isRuleSetDeployed(Set<String> ruleSetDigests) {
        if (ruleSetDigests == null) {
            return false;
        }

        for (AgentInstance agent : getConnectedAgents()) {
            String agentDigest = agent.getActiveRuleSetDigest();
            if (!ruleSetDigests.contains(agentDigest)) {
                return false;
            }
        }

        return true;
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
