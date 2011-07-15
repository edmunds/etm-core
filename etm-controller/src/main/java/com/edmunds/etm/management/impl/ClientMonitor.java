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

import com.edmunds.etm.common.api.ClientPaths;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.common.thrift.ClientConfigDto;
import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.HttpMonitor;
import com.edmunds.etm.management.api.ManagementPoolMember;
import com.edmunds.etm.management.api.ManagementVip;
import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeConsistentCallback;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeNode;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeWatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.edmunds.etm.management.api.ManagementLoadBalancerState.UNKNOWN;
import static com.edmunds.etm.management.api.ManagementVipType.MAVEN_ONLY;
import static com.edmunds.zookeeper.connection.ZooKeeperConnectionState.INITIALIZED;

/**
 * The host manager tracks the state of the two EPHEMERAL nodes created by client connections. <p/> It creates {@code
 * Host} objects for each connection and calls the registered  {@code HostUpdateListener} to notify it when the nodes
 * are fully on-line and when they go offline.
 */
@Component
public class ClientMonitor implements ZooKeeperConnectionListener, InitializingBean, DisposableBean {

    private static final long CLIENT_IDLE_PERIOD_DEFAULT = 3000;
    private static final long SCHEDULER_EXECUTION_DELAY = 500;
    private static final long SCHEDULER_EXECUTION_PERIOD = 250;
    private static final long SCHEDULER_SHUTDOWN_TIMEOUT = 3000;

    private static final Logger logger = Logger.getLogger(ClientMonitor.class);

    private final ZooKeeperTreeWatcher watcher;
    private final Collection<ClientMonitorCallback> clientMonitorCallbacks;
    private final ObjectSerializer objectSerializer;

    private ScheduledExecutorService callbackScheduler;
    private long clientIdlePeriod;
    private ManagementVips clientVips;

    @Autowired
    public ClientMonitor(ZooKeeperConnection connection,
                         ClientPaths clientPaths,
                         ObjectSerializer objectSerializer) {
        this.clientMonitorCallbacks = Sets.newHashSet();

        this.watcher = new ZooKeeperTreeWatcher(
            connection, 0, clientPaths.getConnected(), new ZooKeeperTreeConsistentCallback() {
                @Override
                public void treeConsistent(ZooKeeperTreeNode oldRoot, ZooKeeperTreeNode newRoot) {
                    clientTreeChanged(newRoot);
                }
            });

        this.objectSerializer = objectSerializer;
        this.clientIdlePeriod = CLIENT_IDLE_PERIOD_DEFAULT;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // Set up the callback scheduler
        callbackScheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new ScheduledCallbackTask(clientIdlePeriod);
        long delay = SCHEDULER_EXECUTION_DELAY;
        long period = SCHEDULER_EXECUTION_PERIOD;
        callbackScheduler.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * Adds a client monitor callback to receive notification of changes to client vips.
     *
     * @param callback client monitor callback
     */
    public void addCallback(ClientMonitorCallback callback) {
        clientMonitorCallbacks.add(callback);
    }

    /**
     * Gets the client idle period in milliseconds. <p/> The client idle period is an amount of time that must elapse
     * without any client application activity (e.g. applications coming online or going offline) in order to propogate
     * the most recent activity throughout the system. <p/> The default value is {@code 5000} (5 seconds).
     *
     * @return client idle period in milliseconds
     */
    public long getClientIdlePeriod() {
        return clientIdlePeriod;
    }

    /**
     * Sets the client idle period in milliseconds.
     *
     * @param clientIdlePeriod client idle period in milliseconds
     */
    public void setClientIdlePeriod(long clientIdlePeriod) {
        this.clientIdlePeriod = clientIdlePeriod;
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {

        if(logger.isDebugEnabled()) {
            logger.debug(String.format("Connection state changed: %s", state));
        }

        if(state == INITIALIZED) {
            watcher.initialize();
        }
    }

    @Override
    public void destroy() throws Exception {

        // Shut down the callback scheduler
        if(callbackScheduler != null) {
            callbackScheduler.shutdown();
            callbackScheduler.awaitTermination(SCHEDULER_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Gets the current set of client application vips.
     *
     * @return client application vips
     */
    public synchronized ManagementVips getClientVips() {
        return clientVips;
    }

    /**
     * Sets the current client application vips.
     *
     * @param vips client application vips
     */
    protected synchronized void setClientVips(ManagementVips vips) {
        clientVips = vips;
    }

    protected void clientTreeChanged(ZooKeeperTreeNode hosts) {

        if(hosts == null) {
            return;
        }

        // Build the vips object.
        ManagementVips vips = buildVips(hosts);
        setClientVips(vips);
    }

    protected void performCallbacks() {
        for(ClientMonitorCallback callback : clientMonitorCallbacks) {
            callback.onClientVipsUpdated(this);
        }
    }

    private ManagementVips buildVips(ZooKeeperTreeNode connectedNode) {
        final Map<MavenModule, VipBuilder> vipBuilders = Maps.newHashMap();

        for(ZooKeeperTreeNode node : connectedNode.getChildren().values()) {
            processConnectedNode(node, vipBuilders);
        }

        return buildVips(vipBuilders);
    }

    private void processConnectedNode(ZooKeeperTreeNode node, Map<MavenModule, VipBuilder> vipBuilders) {

        ClientConfigDto configDto;
        try {
            configDto = objectSerializer.readValue(node.getData(), ClientConfigDto.class);
        } catch(IOException e) {
            logger.error(String.format("Unable to read client node: %s", node.getPath()), e);
            return;
        }

        final HostAddress memberAddress = HostAddress.readDto(configDto.getHostAddress());
        final MavenModule mavenModule = MavenModule.readDto(configDto.getMavenModule());

        final String ipAddress = memberAddress.getHost();
        if(StringUtils.isEmpty(ipAddress) || ipAddress.startsWith("127.") ||
            "0.0.0.0".equals(ipAddress) || "255.255.255.255".equals(ipAddress)) {
            logger.error("Ignoring Client - Invalid IP (" + ipAddress + ") : " + mavenModule);
            return;
        }

        String context = configDto.getContextPath();
        context = StringUtils.isBlank(context) ? "/" : context.trim();

        List<String> rules = configDto.getUrlRules();
        rules = rules == null ? new ArrayList<String>() : rules;

        HttpMonitor httpMonitor = HttpMonitor.readDto(configDto.getHttpMonitor());

        VipBuilder vipBuilder = vipBuilders.get(mavenModule);

        if(vipBuilder == null) {
            vipBuilder = new VipBuilder(mavenModule, context, rules, httpMonitor);
            vipBuilders.put(mavenModule, vipBuilder);
        }

        vipBuilder.addPoolMember(new ManagementPoolMember(UNKNOWN, memberAddress));
    }

    private ManagementVips buildVips(Map<MavenModule, VipBuilder> vipBuilders) {
        final List<ManagementVip> vips = Lists.newArrayList();

        for(VipBuilder vipBuilder : vipBuilders.values()) {
            vips.add(vipBuilder.build());
        }

        return new ManagementVips(MAVEN_ONLY, vips);
    }

    private static class VipBuilder {
        private final MavenModule mavenModule;
        private final String context;
        private final List<String> rules;
        private final HttpMonitor httpMonitor;
        private final Set<ManagementPoolMember> members;

        public VipBuilder(MavenModule mavenModule, String context, List<String> rules, HttpMonitor httpMonitor) {
            this.mavenModule = mavenModule;
            this.context = context;
            this.rules = rules;
            this.httpMonitor = httpMonitor;
            this.members = Sets.newHashSet();
        }

        public void addPoolMember(ManagementPoolMember poolMember) {
            if(!members.add(poolMember)) {
                logger.info("Duplicate pool member detected: " + poolMember.getHostAddress());
            }
        }

        public ManagementVip build() {
            if(logger.isDebugEnabled()) {
                logger.debug("Building Vip: " + mavenModule);
            }
            return new ManagementVip(UNKNOWN, mavenModule, null, members, context, rules, httpMonitor);
        }
    }

    private class ScheduledCallbackTask implements Runnable {

        private final long idlePeriod;
        private long idleTimeout;
        private ManagementVips previousVips;
        private boolean callbackPending;

        public ScheduledCallbackTask(long idlePeriod) {
            this.idlePeriod = idlePeriod;
            resetIdleTimeout();
        }

        @Override
        public void run() {

            ManagementVips vips = getClientVips();
            if(vips == null) {
                return;
            }

            if(previousVips == null || !previousVips.equals(vips)) {
                logger.debug("Client vips updated");
                previousVips = vips;
                resetIdleTimeout();
                callbackPending = true;
            } else if(isIdlePeriodElapsed() && callbackPending) {
                // Execute the callbacks
                performCallbacks();
                resetIdleTimeout();
                callbackPending = false;
            }
        }

        private boolean isIdlePeriodElapsed() {
            return System.currentTimeMillis() > idleTimeout;
        }

        private void resetIdleTimeout() {
            idleTimeout = System.currentTimeMillis() + idlePeriod;
        }
    }
}
