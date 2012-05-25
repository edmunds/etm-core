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
package com.edmunds.etm.runtime.impl;

import com.edmunds.common.configuration.api.EnvironmentConfiguration;
import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.loadbalancer.impl.LoadBalancerController;
import com.edmunds.etm.management.api.ManagementPoolMember;
import com.edmunds.etm.management.api.ManagementVip;
import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.runtime.api.Application;
import com.edmunds.etm.runtime.api.ApplicationSeries;
import com.edmunds.etm.runtime.api.ApplicationVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Repository for ETM-managed applications.
 * <p/>
 * TODO: Add persistence to accomodate failover and restart
 *
 * @author Ryan Holmes
 */
@Component
public class ApplicationRepository {

    private static final Logger logger = Logger.getLogger(ApplicationRepository.class);

    private EnvironmentConfiguration environment;

    private ImmutableMap<String, ApplicationSeries> seriesByName;

    @Autowired
    public ApplicationRepository(EnvironmentConfiguration environment) {
        this.environment = environment;
        this.seriesByName = ImmutableMap.of();
    }

    public synchronized Set<Application> getAllApplications() {

        Set<Application> applications = Sets.newHashSet();

        for (ApplicationSeries series : seriesByName.values()) {
            applications.addAll(series.getAllVersions());
        }
        return applications;
    }

    public synchronized Set<Application> getActiveApplications() {

        Set<Application> applications = Sets.newHashSetWithExpectedSize(seriesByName.size());

        for (ApplicationSeries series : seriesByName.values()) {
            final Application activeVersion = series.getActiveVersion();
            if (activeVersion != null) {
                applications.add(activeVersion);
            }
        }
        return applications;
    }

    public synchronized Set<Application> getInactiveApplications() {
        Set<Application> applications = Sets.newHashSet();
        for (ApplicationSeries series : seriesByName.values()) {
            applications.addAll(series.getInactiveVersions());
        }
        return applications;
    }

    public synchronized Application getApplicationById(String id) {
        Application match = null;
        for (Application app : getAllApplications()) {
            if (app.getId().equals(id)) {
                match = app;
                break;
            }
        }
        return match;
    }

    public synchronized ApplicationSeries getSeriesByName(String name) {
        return seriesByName.get(name);
    }

    public synchronized void addOrReplaceApplication(Application app) {
        Validate.notNull(app, "Application is null");

        ApplicationSeries series = getSeriesByName(app.getName());

        if (series == null) {
            // Create singleton series.
            series = new ApplicationSeries(app);
        } else {
            // Create new series adding in the app.
            series = series.addOrReplace(app);
        }

        // Add (or replace) the series in the map.
        Map<String, ApplicationSeries> temp = Maps.newHashMap(seriesByName);
        temp.put(series.getName(), series);

        setSeriesByName(temp);
    }

    public synchronized void removeApplication(Application app) {
        if (app == null) {
            return;
        }

        final String applicationName = app.getName();

        ApplicationSeries previousSeries = seriesByName.get(applicationName);

        if (previousSeries == null) {
            return;
        }

        ApplicationSeries series = previousSeries.remove(app);

        // if no change was made to the series return
        if (previousSeries == series) {
            return;
        }

        final HashMap<String, ApplicationSeries> temp = Maps.newHashMap(seriesByName);

        // series will be null if we just removed the last version from the series.
        if (series == null) {
            temp.remove(applicationName);
        } else {
            // The series has more than one entry in it and we need to add the replacement (smaller) series.
            temp.put(applicationName, series);
        }
        setSeriesByName(temp);
    }

    private void setSeriesByName(final Map<String, ApplicationSeries> seriesByName) {
        this.seriesByName = ImmutableMap.copyOf(seriesByName);
    }

    public synchronized void updateFromDeltaVips(ManagementVips vips) {
        Application app;
        for (ManagementVip vip : vips.getVips()) {

            switch (vip.getLoadBalancerState()) {
                case CREATE_REQUEST:
                case ACTIVE:
                    // Do not activate applications with no rules
                    // Check for vip with null rules
                    if (vip.getRules() == null) {
                        String message = String.format("Vip with null rules detected: %s", vip.getHostAddress());
                        logger.error(message);
                        continue;
                    }
                    app = createApplicationFromVip(vip);
                    addOrReplaceApplication(app);
                    break;
                case DELETE_REQUEST:
                    app = getApplicationByMavenModule(vip.getMavenModule());
                    removeApplication(app);
                    break;
                case UNKNOWN:
                    logger.warn(String.format("Unexpected LoadBalancerState: %s", vip.getLoadBalancerState()));
                    break;
                default:
                    String message = String.format("Unknown LoadBalancerState: %s", vip.getLoadBalancerState());
                    logger.error(message);
                    throw new RuntimeException(message);
            }
        }
    }

    private Application getApplicationByMavenModule(MavenModule mavenModule) {

        String name = Application.applicationName(mavenModule);
        ApplicationSeries series = seriesByName.get(name);

        if (series == null) {
            return null;
        }

        ApplicationVersion version = Application.applicationVersion(mavenModule);
        return series.getVersion(version);
    }

    private Application createApplicationFromVip(ManagementVip vip) {
        VirtualServer vs = null;
        if (vip.getHostAddress() != null) {
            Collection<ManagementPoolMember> vipMembers = vip.getPoolMembers().values();
            Set<PoolMember> members = Sets.newHashSetWithExpectedSize(vipMembers.size());
            for (ManagementPoolMember mpm : vipMembers) {
                members.add(new PoolMember(mpm.getHostAddress()));
            }
            String prefix = LoadBalancerController.VIRTUAL_SERVER_NAME_PREFIX;
            String serverName = VirtualServer.createServerName(prefix, vip.getMavenModule(), environment);
            vs = new VirtualServer(serverName, vip.getHostAddress(), members);
        }

        return new Application(vip.getMavenModule(), vip.getRules(), vip.getHttpMonitor(), vs);
    }
}
