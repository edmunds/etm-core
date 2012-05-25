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
import com.edmunds.etm.system.api.FailoverListener;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.election.ZooKeeperElection;
import com.edmunds.zookeeper.election.ZooKeeperElectionListener;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Monitors the failover state of the ETM controller and notifies listeners of changes.
 *
 * @author Ryan Holmes
 */
@Component
public class FailoverMonitor implements ZooKeeperConnectionListener, ZooKeeperElectionListener {

    private static final Logger logger = Logger.getLogger(FailoverMonitor.class);

    private final ZooKeeperConnection connection;
    private final ZooKeeperElection masterElection;
    private final Set<FailoverListener> failoverListeners;

    private FailoverState failoverState;

    /**
     * Constructs a new FailoverMonitor with injected dependencies.
     *
     * @param connection      the ZooKeeper connection
     * @param controllerPaths controller paths
     */
    @Autowired
    public FailoverMonitor(ZooKeeperConnection connection, ControllerPaths controllerPaths) {
        this.connection = connection;
        this.masterElection = new ZooKeeperElection(connection, controllerPaths.getMaster());
        this.failoverListeners = Sets.newHashSet();
        this.failoverState = FailoverState.UNKNOWN;
    }

    /**
     * Adds a listener to receive notification of changes to the failover state.
     *
     * @param listener failover listener
     */
    public void addListener(FailoverListener listener) {
        failoverListeners.add(listener);
    }

    /**
     * Gets the current failover state of the ETM controller.
     *
     * @return current failover state
     */
    public synchronized FailoverState getFailoverState() {
        return this.failoverState;
    }

    /**
     * Sets the failover state of the ETM controller.
     *
     * @param failoverState current failover state
     */
    protected synchronized void setFailoverState(FailoverState failoverState) {
        this.failoverState = failoverState;
    }

    /**
     * Suspends an active controller.
     *
     * @throws IllegalStateException if not in ACTIVE state
     */
    public void suspend() {
        if (getFailoverState() != FailoverState.ACTIVE) {
            throw new IllegalStateException("Only an active controller may be suspended");
        }
        changeFailoverState(FailoverState.SUSPENDED);
    }

    /**
     * Resumes a suspended controller.
     *
     * @throws IllegalStateException if not in SUSPENDED state
     */
    public void resume() {
        if (getFailoverState() != FailoverState.SUSPENDED) {
            throw new IllegalStateException("Only a suspended controller may be resumed");
        }
        changeFailoverState(FailoverState.ACTIVE);
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {
            masterElection.enroll(this);
        }
    }

    @Override
    public void onElectionLeader(ZooKeeperElection election) {
        changeFailoverState(FailoverState.ACTIVE);
    }

    @Override
    public void onElectionWithdrawn(ZooKeeperElection election) {
        logger.warn("Unexpected withdrawal from master election, going on standby and re-enrolling");
        changeFailoverState(FailoverState.STANDBY);
        masterElection.enroll(this);
    }

    @Override
    public void onElectionError(ZooKeeperElection election, KeeperException error) {
        logger.error(String.format("Master election error, reconnecting: %s", error));
        connection.reconnect();
    }

    @PostConstruct
    protected void initialize() {
        changeFailoverState(FailoverState.STANDBY);
    }

    private void changeFailoverState(FailoverState state) {
        logger.info(String.format("Failover state changed: %s", state));
        setFailoverState(state);
        for (FailoverListener listener : failoverListeners) {
            listener.onFailoverStateChanged(this);
        }
    }
}
