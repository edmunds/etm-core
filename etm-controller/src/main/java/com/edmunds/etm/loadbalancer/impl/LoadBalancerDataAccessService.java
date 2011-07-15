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
package com.edmunds.etm.loadbalancer.impl;

import com.edmunds.etm.loadbalancer.api.AvailabilityStatus;
import com.edmunds.etm.loadbalancer.api.LoadBalancerConnection;
import com.edmunds.etm.loadbalancer.api.VirtualServerNotFoundException;
import com.edmunds.etm.runtime.api.Application;
import com.edmunds.etm.runtime.impl.ApplicationRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The LoadBalancerDataAccessService provides thread-safe access to load balancer data.
 *
 * @author Ryan Holmes
 */
@Component
public class LoadBalancerDataAccessService {

    private static final Logger logger = Logger.getLogger(LoadBalancerDataAccessService.class);

    private final LoadBalancerConnection loadBalancerConnection;
    private final ApplicationRepository applicationRepository;

    private final AvailabilityStatusCache availabilityStatusCache;

    @Autowired
    public LoadBalancerDataAccessService(LoadBalancerConnection loadBalancerConnection,
                                         ApplicationRepository applicationRepository) {
        this.loadBalancerConnection = loadBalancerConnection;
        this.applicationRepository = applicationRepository;
        this.availabilityStatusCache = new AvailabilityStatusCache();
    }

    /**
     * Gets the availability status of the specified virtual server.
     *
     * @param serverName virtual server name
     * @return availability status
     */
    public AvailabilityStatus getAvailabilityStatus(String serverName) {
        return availabilityStatusCache.getAvailabilityStatus(serverName);
    }

    /**
     * Reads the availability status for all applications from the load balancer. <p/> Calls to this method should be
     * throttled to limit the frequency of requests to the load balancer.
     *
     * @return map of AvailabilityStatus objects for all virtual servers
     */
    protected Map<String, AvailabilityStatus> readAvailabilityStatus() {

        if(logger.isDebugEnabled()) {
            logger.debug("Reading availability status from load balancer");
        }

        Map<String, AvailabilityStatus> statusMap;
        Set<Application> applications = applicationRepository.getAllApplications();
        List<String> serverNames = Lists.newArrayListWithCapacity(applications.size());

        for(Application app : applications) {
            if(app.hasVirtualServer()) {
                serverNames.add(app.getVirtualServerName());
            }
        }

        if(loadBalancerConnection.connect()) {

            // Try to get availability status in a bulk operation first
            try {
                statusMap = loadBalancerConnection.getAvailabilityStatus(serverNames);
            } catch(VirtualServerNotFoundException e) {
                // Fall back to incremental operations
                statusMap = readAvailabilityStatusIncrementally(serverNames);
            } catch(RemoteException e) {
                logger.error("Unable to read virtual server status", e);
                statusMap = Maps.newHashMap();
            }
        } else {
            logger.error("Could not connect to an active load balancer");
            statusMap = Maps.newHashMap();
        }

        return statusMap;
    }

    private Map<String, AvailabilityStatus> readAvailabilityStatusIncrementally(List<String> serverNames) {

        Map<String, AvailabilityStatus> statusMap = Maps.newHashMapWithExpectedSize(serverNames.size());

        for(String name : serverNames) {
            try {
                Map<String, AvailabilityStatus> result;
                result = loadBalancerConnection.getAvailabilityStatus(Collections.singletonList(name));
                AvailabilityStatus status = result.get(name);
                if(status != null) {
                    statusMap.put(name, status);
                }
            } catch(VirtualServerNotFoundException e) {
                logger.error(String.format("Availability status not found for server %s", name), e);
            } catch(RemoteException e) {
                logger.error("Unable to read virtual server status", e);
            }
        }
        return statusMap;
    }

    private class AvailabilityStatusCache {
        /**
         * Number of milliseconds after which the cache contents are considered expired.
         */
        private static final long CACHE_EXPIRE_MILLIS = 3000;

        /**
         * Minimum number of milliseconds between cache refreshes.
         */
        private static final long MIN_REFRESH_MILLIS = 500;

        private Map<String, AvailabilityStatus> cache;
        private long lastRefresh;
        private long expires;

        public AvailabilityStatusCache() {
            cache = Maps.newHashMap();
            expires = Long.MIN_VALUE;
            lastRefresh = Long.MIN_VALUE;
        }

        public synchronized AvailabilityStatus getAvailabilityStatus(String serverName) {

            if(isExpired() || !cache.containsKey(serverName)) {
                refreshCache();
            }

            AvailabilityStatus status = cache.get(serverName);
            return status == null ? AvailabilityStatus.NONE : status;
        }

        private boolean isExpired() {
            return expires < System.currentTimeMillis();
        }

        private void refreshCache() {
            // Throttle the refresh rate
            if(lastRefresh + MIN_REFRESH_MILLIS > System.currentTimeMillis()) {
                return;
            }
            cache = readAvailabilityStatus();
            lastRefresh = System.currentTimeMillis();
            expires = lastRefresh + CACHE_EXPIRE_MILLIS;
        }
    }
}
