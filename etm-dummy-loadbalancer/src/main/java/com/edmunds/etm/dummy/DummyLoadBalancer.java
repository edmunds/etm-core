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
package com.edmunds.etm.dummy;

import com.edmunds.etm.loadbalancer.api.AvailabilityStatus;
import com.edmunds.etm.loadbalancer.api.LoadBalancerConnection;
import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.loadbalancer.api.PoolMemberExistsException;
import com.edmunds.etm.loadbalancer.api.PoolMemberNotFoundException;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.loadbalancer.api.VirtualServerExistsException;
import com.edmunds.etm.loadbalancer.api.VirtualServerNotFoundException;
import com.edmunds.etm.management.api.HttpMonitor;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dummy Load Balancer.
 *
 * @author David Trott
 */
@Scope("prototype")
@Component
public class DummyLoadBalancer implements LoadBalancerConnection {

    @Override
    public boolean connect() {
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Set<VirtualServer> getAllVirtualServers() throws RemoteException {
        return Sets.newHashSet();
    }

    @Override
    public VirtualServer getVirtualServer(String serverName) throws VirtualServerNotFoundException, RemoteException {
        return null;
    }

    @Override
    public boolean isVirtualServerDefined(String serverName) throws RemoteException {
        return true;
    }

    @Override
    public Map<String, AvailabilityStatus> getAvailabilityStatus(List<String> serverNames) throws
            VirtualServerNotFoundException, RemoteException {

        return Maps.newHashMap();
    }

    @Override
    public void createVirtualServer(VirtualServer server, HttpMonitor httpMonitor) throws
            VirtualServerExistsException, RemoteException {
    }

    @Override
    public void verifyVirtualServer(VirtualServer server, HttpMonitor httpMonitor) {
    }

    @Override
    public void deleteVirtualServer(VirtualServer server) throws VirtualServerNotFoundException, RemoteException {
    }

    @Override
    public void addPoolMember(String serverName, PoolMember member) throws PoolMemberExistsException, RemoteException {
    }

    @Override
    public void removePoolMember(String serverName, PoolMember member) throws PoolMemberNotFoundException, RemoteException {
    }

    @Override
    public boolean saveConfiguration() {
        return true;
    }
}
