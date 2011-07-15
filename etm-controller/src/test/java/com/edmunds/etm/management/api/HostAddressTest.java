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
package com.edmunds.etm.management.api;

import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Test
public class HostAddressTest {

    // Check the sort order is working correctly.
    public void compareToTest() {
        List<HostAddress> addresses = Lists.newArrayList();

        final HostAddress c1 = new HostAddress("a.b.c", 123);
        final HostAddress c2 = new HostAddress("a.b.c", 456);
        final HostAddress d1 = new HostAddress("a.b.d", 123);
        final HostAddress d2 = new HostAddress("a.b.d", 456);
        final HostAddress e1 = new HostAddress("a.b.e", 123);

        addresses.add(d2);
        addresses.add(c2);
        addresses.add(e1);
        addresses.add(c1);
        addresses.add(d1);

        Collections.sort(addresses);

        assertEquals(addresses.get(0), c1);
        assertEquals(addresses.get(1), c2);
        assertEquals(addresses.get(2), d1);
        assertEquals(addresses.get(3), d2);
        assertEquals(addresses.get(4), e1);
    }
}
