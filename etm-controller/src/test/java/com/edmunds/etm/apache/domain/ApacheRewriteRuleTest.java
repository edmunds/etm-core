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
package com.edmunds.etm.apache.domain;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test for {@link RewriteRule}
 * <p/>
 * Copyright (C) 2010 Edmunds.com
 * <p/>
 * Date: Apr 8, 2010:5:53:43 PM
 *
 * @author Dmytro Seredenko
 */
@Test
public class ApacheRewriteRuleTest {
    public void testGetPattern() {
        String pattern = "pattern";
        RewriteRule rule = new ApacheRewriteRule(pattern, "a", "b");
        assertEquals(rule.getPattern(), pattern);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPattern() {
        new ApacheRewriteRule(null, "a");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyPattern() {
        new ApacheRewriteRule("", "a");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetNullSubstitutionn() {
        new ApacheRewriteRule("a", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetEmptySubstitutionn() {
        new ApacheRewriteRule("a", "");
    }

    public void testGetSubstitution() {
        String substitution = "substitution";
        RewriteRule rule = new ApacheRewriteRule("a", substitution);
        assertEquals(rule.getSubstitution(), substitution);
    }

    public void testGetEmptyOptions() {
        RewriteRule rule = new ApacheRewriteRule("a", "b");
        assertEquals(rule.getOptions().length, 0);
    }

    public void testGetOptions() {
        RewriteRule rule = new ApacheRewriteRule("a", "b", "0", "1");
        assertEquals(rule.getOptions().length, 2);
        assertEquals(rule.getOptions()[0], "0");
        assertEquals(rule.getOptions()[1], "1");
    }

    @Test(dataProvider = "rulesProvider")
    public void testBuild(String pattern, String substitution, String[] options, String rule) {
        assertEquals(new ApacheRewriteRule(pattern, substitution, options).build(), rule);
    }

    @DataProvider(name = "rulesProvider")
    public Object[][] createRules() {
        return new Object[][]{
                {"^[a-z]$", "edmunds.com:8080", new String[]{"P"}, "RewriteRule ^[a-z]$ http://edmunds.com:8080$0 [P]"},
                {"^[a-z]$", "edmunds.com:8080", null, "RewriteRule ^[a-z]$ http://edmunds.com:8080$0"},
                {"^[a-z]$", "edmunds.com:8080", new String[]{}, "RewriteRule ^[a-z]$ http://edmunds.com:8080$0"},
                {"^[a-z]$", "edmunds.com:8080", new String[]{"P", "QQ", "S123"},
                        "RewriteRule ^[a-z]$ http://edmunds.com:8080$0 [P,QQ,S123]"},
        };
    }
}
