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
package com.edmunds.etm.apache.configbuilder;

import com.edmunds.etm.apache.rule.builder.ApacheRuleBuilder;
import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.api.DefaultUrlTokenDictionary;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

/**
 * Test for {@link com.edmunds.etm.rules.api.WebServerConfigurationBuilder}
 *
 * @author Aliaksandr Savin
 */
@Test
public class ApacheConfigurationBuilderTest {

    private ApacheConfigurationBuilder configurationBuilder;
    private Set<UrlRule> rules;
    private ApacheRuleBuilder ruleBuilder;
    private ControllerPaths controllerPaths;
    private ZooKeeperConnection connection;

    @BeforeClass
    public void setUp() {
        rules = new LinkedHashSet<UrlRule>();
        rules.add(getUrlRule());

        ruleBuilder = createMock(ApacheRuleBuilder.class);
        controllerPaths = createMock(ControllerPaths.class);
        connection = createMock(ZooKeeperConnection.class);
        configurationBuilder = new ApacheConfigurationBuilder();
        configurationBuilder.setRuleBuilder(ruleBuilder);
        configurationBuilder.setControllerPaths(controllerPaths);
        configurationBuilder.setConnection(connection);
    }

    public void testGenerateConfiguration() {
        expect(ruleBuilder.build("/rule/")).andReturn("/rule/").once();

        replay(ruleBuilder);

        configurationBuilder.build(rules);
        verify(ruleBuilder);
        // TODO verify the configuration.
    }

    private UrlRule getUrlRule() {
        return new UrlRule(DefaultUrlTokenDictionary.newInstance(),
                new MavenModule("com.edmunds", "test-app", "1.0.0"), "1.2.3.4:80", "/rule/");
    }
}
