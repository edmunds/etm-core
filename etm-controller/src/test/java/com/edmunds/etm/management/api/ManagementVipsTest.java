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
import org.easymock.IMocksControl;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createControl;
import static org.testng.Assert.fail;

@Test
public class ManagementVipsTest {
    private IMocksControl control;
    private ManagementVip vipComplete;
    private ManagementVip vipMaven;
    private HostAddress address1;
    private MavenModule module1;
    private MavenModule module2;

    @BeforeClass
    public void setupClass() {
    }

    @BeforeMethod
    public void setup() {
        control = createControl();

        final Set<ManagementPoolMember> emptyPool = Collections.emptySet();
        final String rootContext = "/";
        final List<String> rules = Lists.newArrayList("/rule");
        final HttpMonitor monitor = new HttpMonitor("/status.html", "OK");

        address1 = new HostAddress("1.2.3.4", 7000);
        module1 = new MavenModule("com.edmunds", "app1", "1.0.0");
        module2 = new MavenModule("com.edmunds", "app2", "1.0.0");
        vipComplete = new ManagementVip(ManagementLoadBalancerState.ACTIVE, module1, address1, emptyPool, rootContext, rules, monitor);
        vipMaven = new ManagementVip(ManagementLoadBalancerState.ACTIVE, module2, null, emptyPool, rootContext, rules, monitor);
    }

    @Test
    public void constructorTestMavenMaven() {
        expectOk(ManagementVipType.MAVEN_ONLY, vipMaven);
    }

    @Test
    public void constructorTestMavenComplete() {
        expectOk(ManagementVipType.MAVEN_ONLY, vipComplete);
    }

    @Test
    public void constructorTestCompleteMaven() {
        expectIllegalArgument(ManagementVipType.COMPLETE, vipMaven);
    }

    @Test
    public void constructorTestCompleteComplete() {
        expectOk(ManagementVipType.COMPLETE, vipComplete);
    }

    @Test
    public void constructorTestMavenCompleteMaven() {
        expectOk(ManagementVipType.MAVEN_ONLY, vipComplete, vipMaven);
    }

    private void expectIllegalArgument(ManagementVipType type, ManagementVip... vip) {
        try {
            new ManagementVips(type, Lists.newArrayList(vip));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    private void expectOk(ManagementVipType type, ManagementVip... vip) {
        new ManagementVips(type, Lists.newArrayList(vip));
    }
}
