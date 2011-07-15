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
package com.edmunds.etm.rules.api;

import com.edmunds.etm.common.api.FixedUrlToken;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests a UrlRuleSet
 */
@Test
public class UrlRuleSetTest {
    private MavenModule testApp1;
    private MavenModule testApp2;
    private UrlRuleSet urlRuleSet;

    private UrlTokenResolver tokenResolver;

    @BeforeClass
    public void setup() {
        this.testApp1 = new MavenModule("com.edmunds", "test-app1", "1.0");
        this.testApp2 = new MavenModule("com.edmunds", "test-app2", "1.0");
        UrlTokenDictionary dictionary = new UrlTokenDictionary();
        dictionary.add(new FixedUrlToken("make", "ford", "volvo"));
        dictionary.add(new FixedUrlToken("model", "ford", "volvo"));
        dictionary.add(new FixedUrlToken("state", "california", "nevada"));
        this.tokenResolver = dictionary;
        urlRuleSet = new UrlRuleSet(buildRules(testApp1,
            "/",
            "/[make]/**",
            "/app/**",
            "/app/delta/index.html"
        ));
    }

    /**
     * Delta is last because the only rules that are block it are from the same application.
     */
    @Test
    public void mergeRuleTestSimple() {
        UrlRuleSet newRuleSet = mergeRules(
            "/app/a*");

        assertRuleorder(newRuleSet,
            "/",
            "/[make]/**",
            "/app/a*",
            "/app/**",
            "/app/delta/index.html");
    }

    /**
     * Inserting a rule from a different application that is relevant re-orders everything.
     */
    @Test
    public void mergeRuleTestInsert() {
        UrlRuleSet newRuleSet = mergeRules(
            "/app/a*",
            "/app/delta/*"
        );

        assertRuleorder(newRuleSet,
            "/",
            "/[make]/**",
            "/app/a*",
            "/app/delta/index.html",
            "/app/delta/*",
            "/app/**");
    }

    /**
     * Inserting a compete string that matches a token.
     */
    @Test
    public void mergeRuleTestToken() {
        UrlRuleSet newRuleSet = mergeRules(
            "/ford/index.html"
        );

        assertRuleorder(newRuleSet,
            "/",
            "/app/**",
            "/app/delta/index.html",
            "/ford/index.html",
            "/[make]/**");
    }

    /**
     * Adding an identical rule.
     */
    @Test
    public void mergeRuleTestIdentical() {
        UrlRuleSet newRuleSet = mergeRules(
            "/app/**"
        );

        assertNull(newRuleSet);
    }

    /**
     * Adding an identical rule.
     */
    @Test
    public void mergeRuleTestOverlap() {
        UrlRuleSet newRuleSet = mergeRules(
            "/[model]/**"
        );

        assertNull(newRuleSet);
    }

    @Test
    public void mergeRuleTestDeepCopy() {
        UrlRuleSet ruleSet1 = mergeRules("/app/a*");

        UrlRuleSet ruleSet2 = ruleSet1.mergeRules(buildRules(testApp2, "/app/b*"));

        assertRuleorder(ruleSet1,
            "/",
            "/[make]/**",
            "/app/a*",
            "/app/**",
            "/app/delta/index.html");

        assertRuleorder(ruleSet2,
            "/",
            "/[make]/**",
            "/app/a*",
            "/app/b*",
            "/app/**",
            "/app/delta/index.html");
    }

    @Test
    public void deleteRulesTest() {
        UrlRuleSet newRuleSet = mergeRules(
            "/app/a*",
            "/app/delta/*"
        );

        assertRuleorder(newRuleSet,
            "/",
            "/[make]/**",
            "/app/a*",
            "/app/delta/index.html",
            "/app/delta/*",
            "/app/**");

        newRuleSet.deleteRules(testApp2);

        assertRuleorder(newRuleSet,
            "/",
            "/[make]/**",
            "/app/**",
            "/app/delta/index.html");

        UrlRuleSet reAddRuleSet = newRuleSet.mergeRules(buildRules(testApp2,
            "/app/a*",
            "/app/delta/*"));

        assertRuleorder(reAddRuleSet,
            "/",
            "/[make]/**",
            "/app/a*",
            "/app/delta/index.html",
            "/app/delta/*",
            "/app/**");
    }

    private List<UrlRule> buildRules(MavenModule mavenModule, String... rules) {
        List<UrlRule> rulesList = Lists.newArrayList();

        for(String rule : rules) {
            rulesList.add(new UrlRule(tokenResolver, mavenModule, "localhost:80", rule));
        }

        return rulesList;
    }

    private UrlRuleSet mergeRules(String... rules) {
        return urlRuleSet.mergeRules(buildRules(testApp2, rules));
    }

    private void assertRuleorder(UrlRuleSet newRuleSet, String... rules) {
        assertNotNull(newRuleSet);
        Set<UrlRule> orderedRules = newRuleSet.orderRules();

        assertEquals(orderedRules.size(), rules.length);
        Iterator<UrlRule> iterator = orderedRules.iterator();

        int index = 0;
        for(String rule : rules) {
            assertEquals(iterator.next().getRule(), rule, "Index: " + index++);
        }
    }
}
