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

import com.edmunds.etm.apache.configbuilder.ApacheConfigurationBuilder;
import com.edmunds.etm.rules.impl.WebConfigurationManager;
import com.edmunds.etm.runtime.impl.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Main ETM Controller MBean.
 *
 * @author Ryan Holmes
 */
@Component
@ManagedResource(objectName = "Edmunds:type=ETM,name=Controller", description = "ETM Controller")
public class ControllerMBean {

    private final FailoverMonitor failoverMonitor;
    private final ApplicationRepository applicationRepository;
    private final WebConfigurationManager webConfigurationManager;
    private final ApacheConfigurationBuilder apacheConfigurationBuilder;
    private final ProjectProperties projectProperties;

    @Autowired
    public ControllerMBean(FailoverMonitor failoverMonitor,
                           ApplicationRepository applicationRepository,
                           WebConfigurationManager webConfigurationManager,
                           ApacheConfigurationBuilder apacheConfigurationBuilder,
                           ProjectProperties projectProperties) {
        this.failoverMonitor = failoverMonitor;
        this.applicationRepository = applicationRepository;
        this.webConfigurationManager = webConfigurationManager;
        this.apacheConfigurationBuilder = apacheConfigurationBuilder;
        this.projectProperties = projectProperties;
    }

    @ManagedAttribute(description = "Failover state")
    public String getFailoverState() {
        return failoverMonitor.getFailoverState().toString();
    }

    @ManagedAttribute(description = "Number of active applications")
    public int getActiveApplicationCount() {
        return applicationRepository.getActiveApplications().size();
    }

    @ManagedAttribute(description = "Number of inactive applications")
    public int getInactiveApplicationCount() {
        return applicationRepository.getInactiveApplications().size();
    }

    @ManagedAttribute(description = "Number of active URL rules")
    public int getActiveUrlRuleCount() {
        return webConfigurationManager.getActiveRules().size();
    }

    @ManagedAttribute(description = "Number of blocked URL rules")
    public int getBlockedUrlRuleCount() {
        return webConfigurationManager.getBlockedRules().size();
    }

    @ManagedAttribute(description = "Number of invalid URL rules")
    public int getInvalidRuleCount() {
        return webConfigurationManager.getInvalidRules().size();
    }

    @ManagedAttribute(description = "Digest of the active Apache rule set")
    public String getApacheRuleSetDigest() {
        return apacheConfigurationBuilder.getActiveRuleSetDigest();
    }

    @ManagedAttribute(description = "ETM Controller version")
    public String getVersion() {
        return projectProperties.getVersion();
    }

    @ManagedOperation(description = "Suspends an active controller")
    public void suspend() {
        failoverMonitor.suspend();
    }

    @ManagedOperation(description = "Resumes a suspended controller")
    public void resume() {
        failoverMonitor.resume();
    }
}
