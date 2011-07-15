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

import com.edmunds.etm.loadbalancer.api.LoadBalancerConfig;
import com.google.common.collect.Sets;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Set;

/**
 * Implementation of an in memory address pool.
 * <p/>
 * TODO Replace this with a ZooKeeper implementation.
 */
public class Inet4AddressPool {
    private static final Logger logger = Logger.getLogger(Inet4AddressPool.class);

    private final Set<OrderedInet4Address> issuedAddresses;
    private final OrderedInet4Address minAddress;
    private final OrderedInet4Address maxAddress;

    private OrderedInet4Address lastIssuedAddress;

    public Inet4AddressPool(LoadBalancerConfig loadBalancerConfig) {
        this(loadBalancerConfig.getIpPoolStart(), loadBalancerConfig.getIpPoolEnd());
    }

    /**
     * Creates a pool of address from the min address to the max address (inclusive of both addresses).
     *
     * @param minAddress the first address available for use.
     * @param maxAddress the last address available for use.
     */
    public Inet4AddressPool(String minAddress, String maxAddress) {
        this(new OrderedInet4Address(minAddress), new OrderedInet4Address(maxAddress));
    }

    /**
     * Creates a pool of address from the min address to the max address (inclusive of both addresses).
     *
     * @param minAddress the first address available for use.
     * @param maxAddress the last address available for use.
     */
    public Inet4AddressPool(OrderedInet4Address minAddress, OrderedInet4Address maxAddress) {
        // Technically the pool could have exactly one entry (minAddress == maxAddress).
        Validate.notNull(minAddress);
        Validate.notNull(maxAddress);
        Validate.isTrue(minAddress.compareTo(maxAddress) <= 0);

        this.issuedAddresses = Sets.newHashSet();
        this.minAddress = minAddress;
        this.maxAddress = maxAddress;
        this.lastIssuedAddress = maxAddress;
    }

    /**
     * Tests if the given address has already been allocated.
     *
     * @param candidate the candidate to test.
     * @return true if the address has been allocated.
     */
    public synchronized boolean isAddressAllocated(OrderedInet4Address candidate) {
        return issuedAddresses.contains(candidate);
    }

    /**
     * Tests an address to see if it falls inside the range managed by this pool.
     *
     * @param candidate the candidate address to be tested
     * @return true if the address is in the range.
     */
    public boolean isAddressInRange(OrderedInet4Address candidate) {
        return minAddress.compareTo(candidate) <= 0 && maxAddress.compareTo(candidate) >= 0;
    }

    /**
     * Resets the internal pool to the list of address that have been allocated.
     *
     * @param addresses the list of allocated addresses (typically loaded from a load balancer)
     */
    public synchronized void setAllocatedAddresses(Collection<OrderedInet4Address> addresses) {
        logger.debug("Setting allocated IP addresses");
        issuedAddresses.clear();

        for (OrderedInet4Address candidate : addresses) {
            if (isAddressInRange(candidate)) {
                logger.debug("Address previously allocated: " + candidate.toString());
                issuedAddresses.add(candidate);
            }
        }
    }

    /**
     * Issues an address from the pool.
     *
     * @return the issued address.
     */
    public synchronized OrderedInet4Address issueAddress() {

        OrderedInet4Address candidate = lastIssuedAddress;

        int wrap = 0;
        while (wrap < 2) {
            candidate = candidate.getNextAddress();

            if (!isAddressInRange(candidate)) {
                wrap++;
                candidate = minAddress;
            }

            if (!isAddressAllocated(candidate)) {
                issuedAddresses.add(candidate);
                lastIssuedAddress = candidate;
                return candidate;
            }
        }
        return null;
    }

    /**
     * Releases an address back to the pool to be re-used.
     *
     * @param address the address being released.
     * @throws IllegalArgumentException if the address is not currently in the list of issues addresses.
     */
    public synchronized void releaseAddress(String address) throws IllegalArgumentException {
        releaseAddress(new OrderedInet4Address(address));
    }

    /**
     * Releases an address back to the pool to be re-used.
     *
     * @param address the address being released.
     */
    public synchronized void releaseAddress(OrderedInet4Address address) {
        boolean removed = issuedAddresses.remove(address);

        if (!removed) {
            logger.warn("Attempt to remove an address that is not in the pool: " + address);
        }
    }
}
