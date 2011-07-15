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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.edmunds.etm.management.api.ManagementLoadBalancerState.ACTIVE;
import static com.edmunds.etm.management.api.ManagementLoadBalancerState.CREATE_REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

@Test
public class ManagementVipTest {
    private static final Collection<ManagementPoolMember> EMPTY_POOL = Collections.emptyList();

    private static final ManagementPoolMember c1 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.c", 123));
    private static final ManagementPoolMember c2 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.c", 456));
    private static final ManagementPoolMember d1 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.d", 123));
    private static final ManagementPoolMember d2 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.d", 456));

    private static final ManagementPoolMember e1a = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.e", 123));
    private static final ManagementPoolMember e1b = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.e", 123));
    private static final ManagementPoolMember e1c = new ManagementPoolMember(CREATE_REQUEST, new HostAddress("a.b.e", 123));

    private ManagementVip vip1;
    private ManagementVip vip2;
    private ManagementVip vip3;

    @BeforeMethod
    public void setup() {
        final MavenModule mavenModule = new MavenModule("com.edmunds", "artifact-1", "1.0.0");

        final List<ManagementPoolMember> candidates1 = Lists.newArrayList();
        candidates1.add(d2);
        candidates1.add(c2);
        candidates1.add(e1a); // Modified
        candidates1.add(c1);
        candidates1.add(d1);

        final List<ManagementPoolMember> candidates2 = Lists.newArrayList();
        candidates2.add(d2);
        candidates2.add(c2);
        candidates2.add(e1b); // Modified
        candidates2.add(c1);
        candidates2.add(d1);

        final List<ManagementPoolMember> candidates3 = Lists.newArrayList();
        candidates3.add(d2);
        candidates3.add(c2);
        candidates3.add(e1c); // Modified
        candidates3.add(c1);
        candidates3.add(d1);

        final String context = "/";
        final List<String> rules = new ArrayList<String>();

        vip1 = new ManagementVip(ACTIVE, mavenModule, null, candidates1, context, rules, null);
        vip2 = new ManagementVip(ACTIVE, mavenModule, null, candidates2, context, rules, null);
        vip3 = new ManagementVip(ACTIVE, mavenModule, null, candidates3, context, rules, null);
    }

    @Test
    public void testMemberOrdering() {
        final List<ManagementPoolMember> members = Lists.newArrayList(vip1.getPoolMembers().values());

        assertEquals(members.get(0), c1);
        assertEquals(members.get(1), c2);
        assertEquals(members.get(2), d1);
        assertEquals(members.get(3), d2);
        assertEquals(members.get(4), e1a);
    }

    @Test
    public void testMemberEquality() {
        assertEquals(vip1.getPoolMembers(), vip2.getPoolMembers());
    }

    @Test
    public void testMemberInequality() {
        assertFalse(vip1.getPoolMembers().equals(vip3.getPoolMembers()));
    }

    @Test
    public void testVipEquality() {
        assertEquals(vip1, vip2);
    }

    @Test
    public void testVipInequality() {
        assertFalse(vip1.equals(vip3));
    }

    @Test
    public void compareToTestEquality() {
        final HostAddress address1 = new HostAddress("localhost", 8080);
        final HostAddress address2 = new HostAddress("localhost", 8080);

        final MavenModule mavenModule1 = new MavenModule("com.edmunds", "artifact-a", "1.0.0");
        final MavenModule mavenModule2 = new MavenModule("com.edmunds", "artifact-a", "1.0.0");
        final String context = "/";
        final List<String> rules = new ArrayList<String>();
        final HttpMonitor monitor = new HttpMonitor("/status.html", "OK");

        vip1 = new ManagementVip(ACTIVE, mavenModule1, address1, EMPTY_POOL, context, rules, monitor);
        vip2 = new ManagementVip(ACTIVE, mavenModule2, address2, EMPTY_POOL, context, rules, monitor);
        assertEquals(vip1.compareTo(vip2), 0);
    }

    @Test
    public void compareToTestInequality() {
        final HostAddress address1 = new HostAddress("localhost", 8080);
        final HostAddress address2 = new HostAddress("localhost", 8080);

        final MavenModule mavenModule1 = new MavenModule("com.edmunds", "artifact-a", "1.0.0");
        final MavenModule mavenModule2 = new MavenModule("com.edmunds", "artifact-a", "2.0.0"); // Changed Version
        final String context = "/";
        final List<String> rules = new ArrayList<String>();
        final HttpMonitor monitor = new HttpMonitor("/status.html", "OK");

        vip1 = new ManagementVip(ACTIVE, mavenModule1, address1, EMPTY_POOL, context, rules, monitor);
        vip2 = new ManagementVip(ACTIVE, mavenModule2, address2, EMPTY_POOL, context, rules, monitor);
        assertEquals(vip1.compareTo(vip2), -1);
    }
}
