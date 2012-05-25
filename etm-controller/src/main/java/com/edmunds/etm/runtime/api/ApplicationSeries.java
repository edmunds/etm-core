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

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An ApplicationSeries keeps track of the deployed versions of a single application.
 * <p/>
 * All applications in a series have the same name and each has a unique version number. In the current implementation,
 * only one version can be active at a time. There will always be an active version as long as the series contains at
 * least one version.
 */
public class ApplicationSeries {

    private final String name;
    private final ImmutableSortedMap<ApplicationVersion, Application> applicationsByVersion;

    /**
     * Constructs a new {@code ApplicationSeries} with the specified name.
     *
     * @param name unique name of the application series
     */
    public ApplicationSeries(String name) {
        Validate.notEmpty(name, "Application series name is empty");
        this.name = name;
        this.applicationsByVersion = ImmutableSortedMap.of();
    }

    public ApplicationSeries(Application application) {
        Validate.notNull(application, "Application is null");

        this.name = application.getName();
        this.applicationsByVersion = ImmutableSortedMap.of(
                application.getVersion(), new Application(application, true));
    }

    private ApplicationSeries(String name, SortedMap<ApplicationVersion, Application> applicationsByVersion) {
        Validate.notEmpty(name, "Application series name is empty");
        this.name = name;
        this.applicationsByVersion = ImmutableSortedMap.copyOf(applicationsByVersion);
    }

    /**
     * Gets the unique name of this application series.
     *
     * @return application series name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the specified version of the application in this series.
     *
     * @param version the version number
     * @return the application of the matching version or {@code null} if not found
     */
    public Application getVersion(ApplicationVersion version) {
        return applicationsByVersion.get(version);
    }

    /**
     * Gets all application versions in this series.
     *
     * @return a collection of all application versions
     */
    public Collection<Application> getAllVersions() {
        return applicationsByVersion.values();
    }

    /**
     * Gets the active application version in this series.
     *
     * @return the active version, or {@code null} if the series is empty
     */
    public Application getActiveVersion() {

        Application activeVersion = null;

        for (Application app : applicationsByVersion.values()) {
            if (app.isActive()) {
                activeVersion = app;
                break;
            }
        }
        return activeVersion;
    }

    /**
     * Gets the inactive application versions in this series.
     *
     * @return a collection of the inactive applications
     */
    public Set<Application> getInactiveVersions() {
        Set<Application> applications = Sets.newHashSetWithExpectedSize(applicationsByVersion.size());
        for (Application app : applicationsByVersion.values()) {
            if (!app.isActive()) {
                applications.add(app);
            }
        }
        return applications;
    }

    /**
     * Adds or replaces the specified application version in this series.
     *
     * @param application the application to add or replace
     * @return the previous application of the same version, or null if there was no entry for the version
     */
    public ApplicationSeries addOrReplace(Application application) {
        Validate.notNull(application);
        if (!application.getName().equals(name)) {
            String message = String
                    .format("Application name '%s' does not match series name '%s'", application.getName(), name);
            throw new IllegalArgumentException(message);
        }

        final Application previous = applicationsByVersion.get(application.getVersion());

        // If the previous version of this app was active so should this version be.
        if (previous != null && previous.isActive()) {
            application = new Application(application, true);
        }

        // Create a mutable copy of the active versions map.
        final TreeMap<ApplicationVersion, Application> temp = Maps.newTreeMap(applicationsByVersion);

        // Add the new version.
        temp.put(application.getVersion(), application);

        // Activate if necessary
        updateActiveVersion(temp);

        // Wrap the mapping in a new application series.
        return new ApplicationSeries(name, temp);
    }

    /**
     * Removes an application version from this series.
     *
     * @param application the application to remove
     * @return the previous application of the same version, or null if there was no entry for the version
     */
    public ApplicationSeries remove(Application application) {
        final TreeMap<ApplicationVersion, Application> temp = Maps.newTreeMap(applicationsByVersion);

        Application previous = temp.remove(application.getVersion());

        // There is no such thing as an empty series (just return null);
        if (temp.isEmpty()) {
            return null;
        }

        // If previous version didn't exist there is no point making a copy.
        if (previous == null) {
            return this;
        }

        updateActiveVersion(temp);

        return new ApplicationSeries(name, temp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationSeries)) {
            return false;
        }

        ApplicationSeries that = (ApplicationSeries) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private void updateActiveVersion(SortedMap<ApplicationVersion, Application> versions) {

        // Find the version with the largest pool size
        Application majorityVersion = null;
        Application activeVersion = null;
        int maxPoolSize = Integer.MIN_VALUE;
        for (Application app : versions.values()) {
            int poolSize = app.getPoolSize();

            // The use of '>' gives us the oldest version in the case of a tie because
            // the values from the map are sorted in ascending version number order.
            if (poolSize > maxPoolSize) {
                maxPoolSize = poolSize;
                majorityVersion = app;
            }

            if (app.isActive()) {
                activeVersion = app;
            }
        }

        if (majorityVersion == null) {
            // Majority version is null only if no versions exist
            return;
        }

        // Has the active version changed?
        if (majorityVersion.equals(activeVersion)) {
            return;
        }

        versions.put(majorityVersion.getVersion(), new Application(majorityVersion, true));

        if (activeVersion != null) {
            versions.put(activeVersion.getVersion(), new Application(activeVersion, false));
        }
    }
}
