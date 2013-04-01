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
package com.edmunds.etm.rules.impl;

import com.edmunds.etm.management.api.ManagementVips;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.management.util.VipDeltaCalculator;
import com.edmunds.etm.rules.api.BlockedUrlRule;
import com.edmunds.etm.rules.api.InvalidUrlRule;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.UrlRuleSet;
import com.edmunds.etm.rules.api.UrlTokenChangeListener;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import com.edmunds.etm.runtime.api.Application;
import com.edmunds.etm.runtime.impl.ApplicationRepository;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.etm.system.impl.FailoverMonitor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the generation and deployment of web server rewrite rules.
 *
 * @author Aliaksandr Savin
 * @author Ryan Holmes
 * @author David Trott
 */
@Service
public class WebConfigurationManager implements UrlTokenChangeListener {

    private static final Logger logger = Logger.getLogger(WebConfigurationManager.class);

    private final ApplicationRepository applicationRepository;
    private final AgentConfigurationManager agentConfigurationManager;
    private final VipDeltaCalculator vipDeltaCalculator;
    private final FailoverMonitor failoverMonitor;
    private UrlTokenResolver tokenResolver;

    private List<String> previousApplicationActivationOrder;
    private Set<UrlRule> activeRules;
    private Set<BlockedUrlRule> blockedRules;
    private Set<InvalidUrlRule> invalidRules;
    private boolean tokensInitialized;
    private ManagementVips previousVips;

    /**
     * Constructor injection.
     *
     * @param applicationRepository     the application repository
     * @param agentConfigurationManager the configuration builder
     * @param vipDeltaCalculator        the vip delta logic
     * @param failoverMonitor           the failover monitor
     * @param urlTokenMonitor           the url token monitor
     */
    @Autowired
    public WebConfigurationManager(

            ApplicationRepository applicationRepository,
            AgentConfigurationManager agentConfigurationManager,
            VipDeltaCalculator vipDeltaCalculator,
            FailoverMonitor failoverMonitor,
            UrlTokenMonitor urlTokenMonitor) {

        this.applicationRepository = applicationRepository;
        this.agentConfigurationManager = agentConfigurationManager;
        this.vipDeltaCalculator = vipDeltaCalculator;
        this.failoverMonitor = failoverMonitor;

        this.activeRules = Sets.newHashSet();
        this.blockedRules = Sets.newHashSet();
        this.invalidRules = Sets.newHashSet();
        this.tokensInitialized = false;

        // Register for notifications
        urlTokenMonitor.addListener(this);
    }

    @Autowired
    public void setTokenResolver(UrlTokenResolver tokenResolver) {
        this.tokenResolver = tokenResolver;
    }

    /**
     * Updates the configuration of the web proxy tier.
     *
     * @param activeVips active application vips
     * @return digest of the new web proxy rule set
     */
    public Set<String> updateConfiguration(ManagementVips activeVips) {
        logger.debug("updateConfiguration() called");
        Validate.notNull(activeVips, "activeVips is null");

        // Get the vip delta
        ManagementVips deltaVips = vipDeltaCalculator.deltaWebTier(getPreviousVips(), activeVips);

        // Update previous vips
        setPreviousVips(activeVips);

        if (tokensInitialized && getFailoverState() == FailoverState.ACTIVE) {
            updateRules(deltaVips);
        }

        return agentConfigurationManager.getActiveRuleSetDigests();
    }

    @Override
    public void onUrlTokensChanged(UrlTokenResolver resolver) {

        if (!tokensInitialized) {
            tokensInitialized = true;
        }

        if (getPreviousVips() != null && getFailoverState() == FailoverState.ACTIVE) {
            recreateRules();
        }
    }

    /**
     * Recreates and deploys the set of web server rewrite rules.
     */
    public synchronized void recreateRules() {

        logger.debug("Recreating rules");

        previousApplicationActivationOrder = null;

        buildActiveRuleSet();
    }

    public Set<UrlRule> getActiveRules() {
        return activeRules;
    }

    public Set<BlockedUrlRule> getBlockedRules() {
        return blockedRules;
    }

    public Set<InvalidUrlRule> getInvalidRules() {
        return invalidRules;
    }

    private synchronized ManagementVips getPreviousVips() {
        return previousVips;
    }

    private synchronized void setPreviousVips(ManagementVips vips) {
        previousVips = vips;
    }

    private FailoverState getFailoverState() {
        return failoverMonitor.getFailoverState();
    }

    private synchronized void updateRules(ManagementVips deltaVips) {

        logger.debug("Updating rules");

        // Update the application repository
        applicationRepository.updateFromDeltaVips(deltaVips);

        // Don't actually create rules if URL tokens have not been initialized
        if (!tokensInitialized) {
            return;
        }

        buildActiveRuleSet();
    }

    private void buildActiveRuleSet() {
        // Start with an empty rule set and no applications activated.
        UrlRuleSet currentRuleSet = new UrlRuleSet(Collections.<UrlRule>emptyList());
        final List<String> activatedApplications = Lists.newArrayList();

        // Also store any invalid rules
        final Set<InvalidUrlRule> ignoredRules = Sets.newHashSet();

        // Get an ordered list of applications to activate.
        List<Application> applications = getApplicationActivationOrder();

        // Add rules for active applications
        for (Application application : applications) {
            if (!application.hasVirtualServer()) {
                logger.error(
                        String.format("Active application has no virtual server: %s", application.getMavenModule()));
                continue;
            }
            UrlRuleSet updatedRuleSet = addRulesForApplication(application, currentRuleSet, ignoredRules);

            if (updatedRuleSet != null) {
                currentRuleSet = updatedRuleSet;
                activatedApplications.add(application.getName());
            }
        }

        // Deploy active rules
        activeRules = Collections.unmodifiableSet(currentRuleSet.orderRules());
        blockedRules = Collections.unmodifiableSet(currentRuleSet.getBlockedRules());
        invalidRules = Collections.unmodifiableSet(ignoredRules);
        agentConfigurationManager.build(applicationRepository.getActiveApplications(), activeRules);

        // Store the previous rule activation order.
        this.previousApplicationActivationOrder = activatedApplications;
    }

    private List<Application> getApplicationActivationOrder() {
        final Map<String, Application> applicationsByName = Maps.newHashMap();

        for (Application application : applicationRepository.getActiveApplications()) {
            applicationsByName.put(application.getName(), application);
        }

        List<Application> activationOrder = Lists.newArrayList();

        if (previousApplicationActivationOrder != null) {
            for (String prevApplication : previousApplicationActivationOrder) {
                final Application application = applicationsByName.remove(prevApplication);
                if (application != null) {
                    activationOrder.add(application);
                }
            }
        }

        // Add any remaining apps to the activation order.
        activationOrder.addAll(applicationsByName.values());
        return activationOrder;
    }

    /**
     * Adds the rules from the specified application into the active rule set.
     *
     * @param app           the application to be activated
     * @param activeRuleSet the rule set to add the rules to
     * @param ignoredRules  rules that have been ignored because they were invalid
     * @return the updated rule set or the existing rule set if the change cannot be made.
     */
    private UrlRuleSet addRulesForApplication(
            Application app, UrlRuleSet activeRuleSet, Set<InvalidUrlRule> ignoredRules) {

        final MavenModule mavenModule = app.getMavenModule();

        final Collection<String> vipRules = app.getRules();

        // Add new rules
        final List<UrlRule> urlRules = Lists.newArrayList();
        for (String rule : vipRules) {
            String vsAddress = app.getVirtualServerAddress().toString();
            try {
                UrlRule urlRule = new UrlRule(tokenResolver, mavenModule, vsAddress, rule);
                if (!activeRuleSet.contains(urlRule)) {
                    urlRules.add(urlRule);
                }
            } catch (RuntimeException e) {
                logger.warn("Ignoring rule: [" + rule + "] (" + mavenModule + ") " + e.getMessage());
                ignoredRules.add(new InvalidUrlRule(mavenModule, rule));
            }
        }

        // Check that we have at least one unique, valid rule
        if (urlRules.isEmpty()) {
            return null;
        }

        final UrlRuleSet newRuleSet = activeRuleSet.mergeRules(urlRules);

        // Check for conflicts
        if (newRuleSet == null) {
            return null;
        }

        // Check for cyclic dependencies
        if (newRuleSet.orderRules() == null) {
            return null;
        }

        // All good so activate
        return newRuleSet;
    }
}
