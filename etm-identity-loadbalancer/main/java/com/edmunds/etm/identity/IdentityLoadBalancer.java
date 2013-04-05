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
package com.edmunds.etm.identity;

import com.edmunds.etm.loadbalancer.api.AvailabilityStatus;
import com.edmunds.etm.loadbalancer.api.LoadBalancerConnection;
import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.loadbalancer.api.PoolMemberExistsException;
import com.edmunds.etm.loadbalancer.api.PoolMemberNotFoundException;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.loadbalancer.api.VirtualServerConfig;
import com.edmunds.etm.loadbalancer.api.VirtualServerExistsException;
import com.edmunds.etm.loadbalancer.api.VirtualServerNotFoundException;
import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.HttpMonitor;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Identity Load Balancer.
 *
 * @author David Trott
 */
@Scope("prototype")
@Component
public class IdentityLoadBalancer implements LoadBalancerConnection {

    private Map<String, VirtualServer> virtualServers = Maps.newHashMap();

    @Override
    public boolean connect() {
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public synchronized Set<VirtualServer> getAllVirtualServers() {
        return Sets.newHashSet(virtualServers.values());
    }

    @Override
    public synchronized VirtualServer getVirtualServer(final String serverName) {
        return virtualServers.get(serverName);
    }

    @Override
    public synchronized boolean isVirtualServerDefined(String serverName) throws RemoteException {
        return virtualServers.containsKey(serverName);
    }

    @Override
    public synchronized Map<String, AvailabilityStatus> getAvailabilityStatus(List<String> serverNames) throws
            VirtualServerNotFoundException, RemoteException {

        final Map<String, AvailabilityStatus> result = Maps.newHashMap();

        for (String serverName : serverNames) {
            final AvailabilityStatus status = isVirtualServerDefined(serverName) ?
                    AvailabilityStatus.AVAILABLE :
                    AvailabilityStatus.UNAVAILABLE;

            result.put(serverName, status);
        }

        return result;
    }

    @Override
    public synchronized HostAddress createVirtualServer(
            VirtualServer server, VirtualServerConfig virtualServerConfig, HttpMonitor httpMonitor) throws
            VirtualServerExistsException, RemoteException {

        final String name = server.getName();
        final HostAddress hostAddress = new HostAddress("localhost", virtualServerConfig.getPort());
        final HashSet<PoolMember> poolMembers = Sets.newHashSet(server.getPoolMembers());
        final VirtualServer serverCopy = new VirtualServer(name, hostAddress, poolMembers);

        virtualServers.put(name, serverCopy);

        return hostAddress;
    }

    @Override
    public void verifyVirtualServer(VirtualServer server, HttpMonitor httpMonitor) {
        // No-op
    }

    @Override
    public synchronized void deleteVirtualServer(VirtualServer server) throws VirtualServerNotFoundException, RemoteException {
        virtualServers.remove(server.getName());
    }

    @Override
    public synchronized void addPoolMember(String serverName, PoolMember member) throws PoolMemberExistsException, RemoteException {
        final VirtualServer virtualServer = getVirtualServer(serverName);

        if (virtualServer != null) {
            virtualServer.addPoolMember(member);
        }
    }

    @Override
    public synchronized void removePoolMember(String serverName, PoolMember member) throws PoolMemberNotFoundException, RemoteException {
        final VirtualServer virtualServer = getVirtualServer(serverName);

        if (virtualServer != null) {
            virtualServer.removePoolMember(member);
        }
    }

    @Override
    public boolean saveConfiguration() {
        // No Op
        return true;
    }
}
