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

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 *
 */
@Test
public class OrderedInet4AddressTest {

    @Test
    public void toStringTest() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.4");
        assertEquals(address.toString(), "1.2.3.4");
    }

    @Test
    public void getNextAddressTestBasic() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.4");
        assertEquals(address.getNextAddress().toString(), "1.2.3.5");
    }

    @Test
    public void getNextAddressTestNoRollover1() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.126");
        assertEquals(address.getNextAddress().toString(), "1.2.3.127");
    }

    @Test
    public void getNextAddressTestNoRollover2() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.127");
        assertEquals(address.getNextAddress().toString(), "1.2.3.128");
    }

    @Test
    public void getNextAddressTestNoRollover3() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.254");
        assertEquals(address.getNextAddress().toString(), "1.2.3.255");
    }

    @Test
    public void getNextAddressTestNoRollover4() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.200");
        assertEquals(address.getNextAddress().toString(), "1.2.3.201");
    }

    @Test
    public void getNextAddressTestRollover1() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.3.255");
        assertEquals(address.getNextAddress().toString(), "1.2.4.0");
    }

    @Test
    public void getNextAddressTestRollover2() {
        final OrderedInet4Address address = new OrderedInet4Address("1.2.255.255");
        assertEquals(address.getNextAddress().toString(), "1.3.0.0");
    }

    @Test
    public void getNextAddressTestRollover3() {
        final OrderedInet4Address address = new OrderedInet4Address("1.255.255.255");
        assertEquals(address.getNextAddress().toString(), "2.0.0.0");
    }

    @Test
    public void getNextAddressTestRollover4() {
        final OrderedInet4Address address = new OrderedInet4Address("255.255.255.255");
        assertEquals(address.getNextAddress().toString(), "0.0.0.0");
    }

    @Test
    public void compareToTestSelf() {
        final OrderedInet4Address inet = new OrderedInet4Address("1.2.3.4");
        assertTrue(inet.compareTo(inet) == 0);
    }

    @Test
    public void compareToTestSame() {
        final OrderedInet4Address a = new OrderedInet4Address("1.2.3.4");
        final OrderedInet4Address b = new OrderedInet4Address("1.2.3.4");

        assertTrue(a.compareTo(b) == 0);
    }

    @Test
    public void compareToTestSubNet() {
        final OrderedInet4Address lower = new OrderedInet4Address("1.2.3.4");
        final OrderedInet4Address higher = new OrderedInet4Address("1.2.3.5");

        assertTrue(lower.compareTo(higher) < 0);
        assertTrue(higher.compareTo(lower) > 0);
    }

    @Test
    public void compareToTestSubNetSignOffset() {
        final OrderedInet4Address lower = new OrderedInet4Address("1.2.3.100");
        final OrderedInet4Address higher = new OrderedInet4Address("1.2.3.200");

        assertTrue(lower.compareTo(higher) < 0);
        assertTrue(higher.compareTo(lower) > 0);
    }

    @Test
    public void compareToTestByteOffset() {
        final OrderedInet4Address lower = new OrderedInet4Address("1.2.100.4");
        final OrderedInet4Address higher = new OrderedInet4Address("1.2.200.4");

        assertTrue(lower.compareTo(higher) < 0);
        assertTrue(higher.compareTo(lower) > 0);
    }

    @Test
    public void compareToTestOrdering() {
        final OrderedInet4Address lower = new OrderedInet4Address("1.2.100.200");
        final OrderedInet4Address higher = new OrderedInet4Address("1.2.200.100");

        assertTrue(lower.compareTo(higher) < 0);
        assertTrue(higher.compareTo(lower) > 0);
    }

    @Test
    public void equalsTestIdentical() {
        final OrderedInet4Address a = new OrderedInet4Address("1.2.100.200");

        assertTrue(a.equals(a));
    }

    @Test
    public void equalsTestNull() {
        final OrderedInet4Address a = new OrderedInet4Address("1.2.100.200");
        final Object b = null;

        assertFalse(a.equals(b));
    }

    @Test
    public void equalsTestString() {
        final OrderedInet4Address a = new OrderedInet4Address("1.2.100.200");
        final Object b = "1.2.100.200";

        assertFalse(a.equals(b));
    }

    @Test
    public void equalsTestEqual() {
        final OrderedInet4Address a = new OrderedInet4Address("1.2.100.200");
        final OrderedInet4Address b = new OrderedInet4Address("1.2.100.200");

        assertTrue(a.equals(b));
    }

    @Test
    public void equalsTestDifferent() {
        final OrderedInet4Address lower = new OrderedInet4Address("1.2.100.200");
        final OrderedInet4Address higher = new OrderedInet4Address("1.2.200.100");

        assertFalse(lower.equals(higher));
    }

    @Test
    public void hashCodeTestSame() {
        final OrderedInet4Address a = new OrderedInet4Address("1.2.100.100");
        final OrderedInet4Address b = new OrderedInet4Address("1.2.100.100");

        assertTrue(a.hashCode() == b.hashCode());
    }

    @Test
    public void hashCodeTestDifferent() {
        final OrderedInet4Address lower = new OrderedInet4Address("1.2.100.200");
        final OrderedInet4Address higher = new OrderedInet4Address("1.2.200.100");

        assertTrue(lower.hashCode() != higher.hashCode());
    }
}
