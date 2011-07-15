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

import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * AlphabeticUrlRuleComparatorTest.
 *
 * @author David Trott
 */
public class AlphabeticUrlRuleComparatorTest {

    private AlphabeticUrlRuleComparator comparator;
    private UrlRule rule1;
    private UrlRule rule2;
    private UrlRule rule3;

    @BeforeTest
    public void setup() {
        final UrlTokenDictionary dictionary = DefaultUrlTokenDictionary.newInstance();
        final MavenModule mavenModule = new MavenModule("com.edmunds", "inventory", "1.2.3");
        this.comparator = new AlphabeticUrlRuleComparator();
        this.rule1 = new UrlRule(dictionary, mavenModule, "1.2.3.4", "/*");
        this.rule2 = new UrlRule(dictionary, mavenModule, "1.2.3.4", "/*");
        this.rule3 = new UrlRule(dictionary, mavenModule, "1.2.3.4", "/a/*");
    }

    @Test
    public void compareTestNullRule1() {
        try {
            comparator.compare(null, rule2);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "rule1 is null");
        }
    }

    @Test
    public void compareTestNullRule2() {
        try {
            comparator.compare(rule1, null);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "rule2 is null");
        }
    }

    @Test
    public void compareTestEqualRules() {
        assertEquals(comparator.compare(rule1, rule2), 0);
    }

    @Test
    public void compareTestRule1AlphabeticallyBeforeRule3() {
        final int result = comparator.compare(rule1, rule3);
        assertTrue(result < 0);
    }

    @Test
    public void compareTestRule3AlphabeticallyAfterRule1() {
        final int result = comparator.compare(rule3, rule1);
        assertTrue(result > 0, "The result was: " + result);
    }

    @AfterTest
    public void cleanup() {
        this.comparator = null;
        this.rule1 = null;
        this.rule2 = null;
        this.rule3 = null;
    }
}
