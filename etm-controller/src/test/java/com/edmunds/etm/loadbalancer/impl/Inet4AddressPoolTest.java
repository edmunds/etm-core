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
import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class Inet4AddressPoolTest {

    @Test
    public void configConstructorTest() {
        final LoadBalancerConfig config = new TestLoadBalancerConfig();

        final Inet4AddressPool pool = new Inet4AddressPool(config);

        final OrderedInet4Address a4 = pool.issueAddress();
        final OrderedInet4Address a5 = pool.issueAddress();
        final OrderedInet4Address a6 = pool.issueAddress();
        final OrderedInet4Address aNull = pool.issueAddress();

        assertEquals(a4.toString(), "1.2.3.4");
        assertEquals(a5.toString(), "1.2.3.5");
        assertEquals(a6.toString(), "1.2.3.6");
        assertNull(aNull);
    }

    @Test
    public void issueAddressTestFullPool() {
        final Inet4AddressPool pool = new Inet4AddressPool("1.2.3.4", "1.2.3.6");

        final OrderedInet4Address a4 = pool.issueAddress();
        final OrderedInet4Address a5 = pool.issueAddress();
        final OrderedInet4Address a6 = pool.issueAddress();
        final OrderedInet4Address aNull = pool.issueAddress();

        assertEquals(a4.toString(), "1.2.3.4");
        assertEquals(a5.toString(), "1.2.3.5");
        assertEquals(a6.toString(), "1.2.3.6");
        assertNull(aNull);
    }

    @Test
    public void issueAddressTestReIssue() {
        final Inet4AddressPool pool = new Inet4AddressPool("1.2.3.4", "1.2.3.6");

        final OrderedInet4Address a4 = pool.issueAddress();
        final OrderedInet4Address a5 = pool.issueAddress();
        final OrderedInet4Address a6 = pool.issueAddress();

        pool.releaseAddress("1.2.3.5");

        final OrderedInet4Address b5 = pool.issueAddress();
        final OrderedInet4Address aNull = pool.issueAddress();

        assertEquals(a4.toString(), "1.2.3.4");
        assertEquals(a5.toString(), "1.2.3.5");
        assertEquals(a6.toString(), "1.2.3.6");
        assertEquals(b5.toString(), "1.2.3.5");
        assertNull(aNull);
    }

    @Test
    public void issueAddressIssueAndRelease() {
        final Inet4AddressPool pool = new Inet4AddressPool("1.2.3.4", "1.2.3.6");

        final OrderedInet4Address a4 = pool.issueAddress();
        pool.releaseAddress("1.2.3.4");

        final OrderedInet4Address a5 = pool.issueAddress();
        pool.releaseAddress("1.2.3.5");

        final OrderedInet4Address a6 = pool.issueAddress();
        pool.releaseAddress(a6);

        final OrderedInet4Address b4 = pool.issueAddress();
        final OrderedInet4Address b5 = pool.issueAddress();
        final OrderedInet4Address b6 = pool.issueAddress();
        final OrderedInet4Address aNull = pool.issueAddress();

        assertEquals(a4.toString(), "1.2.3.4");
        assertEquals(a5.toString(), "1.2.3.5");
        assertEquals(a6.toString(), "1.2.3.6");
        assertEquals(b4.toString(), "1.2.3.4");
        assertEquals(b5.toString(), "1.2.3.5");
        assertEquals(b6.toString(), "1.2.3.6");
        assertNull(aNull);
    }

    @Test
    public void releaseAddressTestOutsideRange() {
        final Inet4AddressPool pool = new Inet4AddressPool("1.2.3.4", "1.2.3.6");

        pool.releaseAddress("4.3.2.1");
    }

    @Test
    public void setAllocatedAddressesTest() {
        final Inet4AddressPool pool = new Inet4AddressPool("1.2.3.4", "1.2.3.6");

        final Collection<OrderedInet4Address> addresses = Lists.newArrayList();

        addresses.add(new OrderedInet4Address("4.3.2.1"));
        addresses.add(new OrderedInet4Address("1.2.3.4"));

        pool.setAllocatedAddresses(addresses);

        final OrderedInet4Address address = pool.issueAddress();
        assertEquals(address.toString(), "1.2.3.5");
    }
}
