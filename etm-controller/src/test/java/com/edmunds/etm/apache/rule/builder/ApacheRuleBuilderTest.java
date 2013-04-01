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
package com.edmunds.etm.apache.rule.builder;

import com.edmunds.etm.common.api.FixedUrlToken;
import com.edmunds.etm.common.api.RegexUrlToken;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test for rule transformer. <p/> <p/> Copyright (C) 2010 Edmunds.com <p/> <p/> Date: Mar 18, 2010
 *
 * @author Aliaksandr Savin
 */
@Test
public class ApacheRuleBuilderTest {

    private MavenModule mavenModule;
    private UrlTokenResolver tokenResolver;

    @BeforeClass
    public void setUp() {
        mavenModule = new MavenModule("com.edmunds", "test-module", "1.0.0");

        UrlTokenDictionary dictionary = new UrlTokenDictionary();
        dictionary.add(new FixedUrlToken("make", "audi", "bmw", "suzuki", "toyota"));
        dictionary.add(new FixedUrlToken("state",
                "california",
                "north-carolina",
                "new-york",
                "armed-forces-europeafricacanada"));
        dictionary.add(new RegexUrlToken("zipcode", "\\d{5}"));
        dictionary.add(new RegexUrlToken("year", "(19|20)\\d{2}"));
        this.tokenResolver = dictionary;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullPattern() {
        new UrlRule(tokenResolver, mavenModule, "local", null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyPattern() {
        new UrlRule(tokenResolver, mavenModule, "local", "a");
    }

    @Test(dataProvider = "getRules")
    public void testTransformRule(String source, String transformed) {
        final UrlRule rule = new UrlRule(tokenResolver, mavenModule, "local", source);

        assertEquals(rule.toRegEx(tokenResolver), transformed);
    }

    @DataProvider
    public Object[][] getRules() {
        return new Object[][]{
                {"/rule.html", "^/rule\\.html$"},
                {"/rul*/**", "^/rul[^/]*/.*$"},
                {"/[make]", "^/(audi|bmw|suzuki|toyota)$"},
                {"/[state]", "^/(california|north-carolina|new-york|armed-forces-europeafricacanada)$"},
                {"/[zipcode]", "^/\\d{5}$"},
                {"/[year]", "^/(19|20)\\d{2}$"},
                {"/crr/[year]", "^/crr/(19|20)\\d{2}$"},
                {"/crr/[year]/", "^/crr/(19|20)\\d{2}/$"},
                {"/crr//[year]/", "^/crr//(19|20)\\d{2}/$"},
                {"/crr/[year]/*.html", "^/crr/(19|20)\\d{2}/[^/]*\\.html$"},
                {"/**/*.html", "^/.*/[^/]*\\.html$"},
                {"/**/", "^/.*/$"},
                {"/**", "^/.*$"},
                {"/*", "^/[^/]*$"}
        };
    }
}
