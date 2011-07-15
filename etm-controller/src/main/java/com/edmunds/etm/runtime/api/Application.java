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
package com.edmunds.etm.runtime.api;

import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.HttpMonitor;
import com.edmunds.etm.management.api.MavenModule;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.CompareToBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An Application represents a client application managed by ETM.
 * <p/>
 * The details of each {@code ApplicationVersion} is contained in its associated {@link
 * com.edmunds.etm.management.api.ManagementVip} (ip address and port, pool member hosts, URL rules, etc.).
 *
 * @author Ryan Holmes
 */
public class Application implements Comparable<Application>, Serializable {

    /**
     * Creates an application name from the given maven module.
     *
     * @param mavenModule maven module
     * @return application name extracted from the maven module
     */
    public static String applicationName(MavenModule mavenModule) {
        Validate.notNull(mavenModule, "Maven module is null");
        return mavenModule.getGroupId() + ":" + mavenModule.getArtifactId();
    }

    /**
     * Creates a version number from the given maven module.
     *
     * @param mavenModule maven module
     * @return version extracted from the maven module
     */
    public static ApplicationVersion applicationVersion(MavenModule mavenModule) {
        Validate.notNull(mavenModule, "Maven module is null");
        return new ApplicationVersion(mavenModule.getVersion());
    }

    private final MavenModule mavenModule;
    private final String name;
    private final ApplicationVersion version;
    private final List<String> rules;
    private final HttpMonitor httpMonitor;
    private final VirtualServer virtualServer;
    private final boolean active;

    /**
     * Constructs a new {@code Application} with the specified parameters.
     *
     * @param mavenModule maven module
     * @param rules       raw url rules
     * @param httpMonitor http monitor
     */
    public Application(MavenModule mavenModule, List<String> rules, HttpMonitor httpMonitor) {
        this(mavenModule, rules, httpMonitor, null);
    }

    /**
     * Constructs a new {@code Application} with the specified parameters.
     *
     * @param mavenModule   maven module
     * @param rules         raw url rules
     * @param httpMonitor   http monitor
     * @param virtualServer virtual server
     */
    public Application(MavenModule mavenModule,
                       List<String> rules,
                       HttpMonitor httpMonitor,
                       VirtualServer virtualServer) {
        Validate.notNull(mavenModule, "Maven module is null");
        Validate.notNull(rules, "Application rules are null");
        this.mavenModule = mavenModule;
        this.name = applicationName(mavenModule);
        this.version = applicationVersion(mavenModule);
        this.rules = rules;
        this.httpMonitor = httpMonitor;
        this.virtualServer = virtualServer;
        this.active = false;
    }

    public Application(Application application, boolean active) {
        this.mavenModule = application.getMavenModule();
        this.name = application.getName();
        this.version = application.getVersion();
        this.rules = application.getRules();
        this.httpMonitor = application.getHttpMonitor();
        this.virtualServer = application.virtualServer;
        this.active = active;
    }

    /**
     * Gets the unique identifier for this application.
     *
     * @return unique application identifier
     */
    public String getId() {
        return mavenModule.toString();
    }

    /**
     * Gets the maven module.
     *
     * @return maven module
     */
    public MavenModule getMavenModule() {
        return mavenModule;
    }

    /**
     * Gets the application name.
     *
     * @return name of the application
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the application version number.
     *
     * @return application version number
     */
    public ApplicationVersion getVersion() {
        return version;
    }

    /**
     * Gets the raw URL rules.
     *
     * @return raw URL rules
     */
    public List<String> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Gets the HTTP monitor.
     *
     * @return http monitor
     */
    public HttpMonitor getHttpMonitor() {
        return httpMonitor;
    }

    /**
     * Returns true if a virtual server has been defined for this application.
     *
     * @return true if a virtual server has been defined, false otherwise
     */
    public boolean hasVirtualServer() {
        return virtualServer != null;
    }

    /**
     * Gets the name of this application's virtual server.
     *
     * @return virtual server name, or null if none is defined
     */
    public String getVirtualServerName() {
        return virtualServer != null ? virtualServer.getName() : null;
    }

    /**
     * Gets the HostAddress of this application's virtual server.
     *
     * @return virtual server host address, or null if none is defined
     */
    public HostAddress getVirtualServerAddress() {
        return virtualServer != null ? virtualServer.getHostAddress() : null;
    }

    /**
     * Gets the pool members in this application's virtual server.
     *
     * @return virtual server pool members, or null if none are defined
     */
    public Set<PoolMember> getVirtualServerPoolMembers() {
        return virtualServer != null ? virtualServer.getPoolMembers() : null;
    }

    /**
     * Gets the number of pool members in this application's virtual server.
     *
     * @return virtual server pool size, or zero if none are defined
     */
    public int getVirtualServerPoolSize() {
        return virtualServer != null ? virtualServer.getPoolSize() : 0;
    }

    /**
     * Gets the number of pool members in this application's virtual server.
     *
     * @return virtual server pool size
     */
    public int getPoolSize() {
        if (virtualServer == null) {
            return 0;
        }

        return virtualServer.getPoolMembers().size();
    }

    /**
     * Indicates whether this application is currently active.
     * <p/>
     * Typically, only one version of a given application is active at a time. An active application receives web
     * traffic based on its URL rules.
     *
     * @return true if active, false if inactive
     */
    public boolean isActive() {
        return active;
    }

    @Override
    public int compareTo(Application o) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(this.name, o.name);
        builder.append(this.version, o.version);

        return builder.toComparison();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Application)) {
            return false;
        }

        Application that = (Application) o;

        if (!name.equals(that.name)) {
            return false;
        }
        return version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Application");
        sb.append("{name='").append(name).append('\'');
        sb.append(", version=").append(version);
        sb.append('}');
        return sb.toString();
    }
}
