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

import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.HttpMonitor;
import com.edmunds.etm.management.api.ManagementPoolMember;
import com.edmunds.etm.management.api.MavenModule;
import com.google.common.collect.Lists;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static com.edmunds.etm.management.api.ManagementLoadBalancerState.ACTIVE;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for the {@link com.edmunds.etm.runtime.api.Application} class.
 *
 * @author Ryan Holmes
 */
@Test
public class ApplicationTest {

    private ManagementPoolMember m1 = new ManagementPoolMember(ACTIVE, new HostAddress("1.1.1.1", 80));
    private ManagementPoolMember m2 = new ManagementPoolMember(ACTIVE, new HostAddress("1.1.1.2", 80));
    private ManagementPoolMember m3 = new ManagementPoolMember(ACTIVE, new HostAddress("1.1.1.3", 80));
    private ManagementPoolMember m4 = new ManagementPoolMember(ACTIVE, new HostAddress("1.1.1.4", 80));

    private MavenModule module1;
    private MavenModule module2;
    private List<String> rules1;
    private List<String> rules2;
    HttpMonitor monitor;

    @BeforeClass
    public void setup() {

        module1 = new MavenModule("group", "app1", "1.0.0");
        module2 = new MavenModule("group", "app2", "1.0.0");
        rules1 = Lists.newArrayList("/app1/*");
        rules2 = Lists.newArrayList("/app2/*");
        monitor = new HttpMonitor("/status.html", "OK");
    }

    @Test
    public void constructValid() {
        new Application(module1, rules1, null);
        new Application(module2, rules2, monitor);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void constructInvalid1() {
        new Application(null, null, null);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void constructInvalid2() {
        new Application(module1, null, null);
    }

    @Test
    public void testEquality() {
        Application app1 = new Application(module1, rules1, null);
        Application app2 = new Application(module1, rules1, monitor);
        assertTrue(app1.equals(app2));

        Application app3 = new Application(module1, rules2, null);
        assertTrue(app1.equals(app3));
    }

    @Test
    public void checkInequality() {
        Application app1 = new Application(module1, rules1, null);
        Application app2 = new Application(module2, rules2, null);
        assertFalse(app1.equals(app2));

        Application app3 = new Application(module2, rules2, monitor);
        assertFalse(app1.equals(app3));
    }
}
