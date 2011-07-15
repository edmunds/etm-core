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

import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.HttpMonitor;
import com.edmunds.etm.management.api.MavenModule;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for the {@link com.edmunds.etm.runtime.api.ApplicationSeries} class.
 *
 * @author Ryan Holmes
 */
public class ApplicationSeriesTest {

    private PoolMember m1 = new PoolMember(new HostAddress("1.1.1.1", 80));
    private PoolMember m2 = new PoolMember(new HostAddress("1.1.1.2", 80));
    private PoolMember m3 = new PoolMember(new HostAddress("1.1.1.3", 80));
    private PoolMember m4 = new PoolMember(new HostAddress("1.1.1.4", 80));
    private PoolMember m5 = new PoolMember(new HostAddress("1.1.1.5", 80));
    private PoolMember m6 = new PoolMember(new HostAddress("1.1.1.6", 80));
    private PoolMember m7 = new PoolMember(new HostAddress("1.1.1.7", 80));
    private PoolMember m8 = new PoolMember(new HostAddress("1.1.1.8", 80));

    private Application app1;
    private Application app2;
    private Application app3;
    private Application app4;

    @BeforeClass
    public void setup() {

        MavenModule module_a1 = new MavenModule("group", "app_a", "1.0.0");
        MavenModule module_a2 = new MavenModule("group", "app_a", "1.0.1");
        MavenModule module_a3 = new MavenModule("group", "app_a", "1.0.2");
        MavenModule module_b = new MavenModule("group", "app_b", "1.0.0");
        List<String> rules_a = Lists.newArrayList("/app_a/*");
        List<String> rules_b = Lists.newArrayList("/app_b/*");
        HttpMonitor monitor = new HttpMonitor("/status.html", "OK");
        VirtualServer server_a1 = new VirtualServer("vs_a1",
            new HostAddress("2.1.1.1", 8080),
            Sets.newHashSet(m1, m2));
        VirtualServer server_a2 = new VirtualServer("vs_a2",
            new HostAddress("2.1.1.2", 8080),
            Sets.newHashSet(m3, m4));
        VirtualServer server_a3 = new VirtualServer("vs_a3",
            new HostAddress("2.1.1.3", 8080),
            Sets.newHashSet(m5, m6, m7));
        VirtualServer server_b = new VirtualServer("vs_b",
            new HostAddress("2.1.1.4", 8080),
            Sets.newHashSet(m8));

        // App A, 2 pool members, v. 1.0.0
        app1 = new Application(module_a1, rules_a, monitor, server_a1);

        // App A, 2 pool members, v. 1.0.1
        app2 = new Application(module_a2, rules_a, monitor, server_a2);

        // App A, 3 pool members, v. 1.0.2
        app3 = new Application(module_a3, rules_a, monitor, server_a3);

        // App B, 1 pool member, v. 1.0.0
        app4 = new Application(module_b, rules_b, monitor, server_b);
    }

    @Test
    public void constructValid() {
        ApplicationSeries series1 = new ApplicationSeries(app1.getName());
        assertEquals(series1.getName(), app1.getName());

        ApplicationSeries series2 = new ApplicationSeries("app1");
        assertEquals(series2.getName(), "app1");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void constructInvalid() {
        new ApplicationSeries("");
        new ApplicationSeries("app1");
    }

    @Test
    public void testSingleVersion() {
        ApplicationSeries series = new ApplicationSeries(app1.getName());
        series = series.addOrReplace(app1);
        assertEquals(series.getActiveVersion(), app1);

        series = series.remove(app1);
        Assert.assertNull(series);
    }

    @Test
    public void testEqualPoolSizes() {
        ApplicationSeries series = new ApplicationSeries(app1.getName());
        series = series.addOrReplace(app1);
        series = series.addOrReplace(app2);
        assertEquals(series.getActiveVersion(), app1);

        series = series.remove(app1);
        assertEquals(series.getActiveVersion(), app2);
    }

    @Test
    public void testUnequalPoolSizes() {
        ApplicationSeries series = new ApplicationSeries(app1.getName());
        series = series.addOrReplace(app1);
        series = series.addOrReplace(app3);
        assertEquals(series.getActiveVersion(), app3);

        series = series.remove(app3);
        assertEquals(series.getActiveVersion(), app1);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testNonMatchingApplication() {
        ApplicationSeries series = new ApplicationSeries(app1.getName());
        series.addOrReplace(app4);
    }
}
