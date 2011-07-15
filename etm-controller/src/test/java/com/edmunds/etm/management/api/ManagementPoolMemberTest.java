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
import static com.edmunds.etm.management.api.ManagementLoadBalancerState.*;

import static org.testng.Assert.assertEquals;

@Test
public class ManagementPoolMemberTest {

    // Check the sort order is working correctly.
    public void compareToTest() {
        List<ManagementPoolMember> members = Lists.newArrayList();

        final ManagementPoolMember cNull = new ManagementPoolMember(CREATE_REQUEST, null);
        final ManagementPoolMember c1 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.c", 123));
        final ManagementPoolMember c2 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.c", 456));
        final ManagementPoolMember d1 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.d", 123));
        final ManagementPoolMember d2 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.d", 456));
        final ManagementPoolMember e1 = new ManagementPoolMember(ACTIVE, new HostAddress("a.b.e", 123));
        final ManagementPoolMember e2 = new ManagementPoolMember(DELETE_REQUEST, new HostAddress("a.b.e", 123));

        members.add(e2);
        members.add(d2);
        members.add(c2);
        members.add(e1);
        members.add(c1);
        members.add(d1);
        members.add(cNull);

        Collections.sort(members);

        assertEquals(members.get(0), cNull);
        assertEquals(members.get(1), c1);
        assertEquals(members.get(2), c2);
        assertEquals(members.get(3), d1);
        assertEquals(members.get(4), d2);
        assertEquals(members.get(5), e1);
        assertEquals(members.get(6), e2);
    }
}
