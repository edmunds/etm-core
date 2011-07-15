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
import com.edmunds.etm.common.api.RegexUrlToken;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;
import java.util.List;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.edmunds.etm.rules.api.RuleComparison.*;
import static com.edmunds.etm.rules.api.SegmentType.COMPLETE;
import static com.edmunds.etm.rules.api.SegmentType.EMPTY;
import static org.testng.Assert.*;

/**
 * Url Rule Test.
 *
 * @author David Trott
 */
@Test
public class UrlRuleTest {
    private MavenModule mavenModule;

    private UrlTokenResolver tokenResolver;

    @BeforeClass
    public void setup() {
        this.mavenModule = new MavenModule("com.edmunds", "test-app", "1.0");

        UrlTokenDictionary dictionary = new UrlTokenDictionary();
        dictionary.add(new FixedUrlToken("make", "ford", "volvo"));
        dictionary.add(new FixedUrlToken("model", "ford", "volvo"));
        dictionary.add(new FixedUrlToken("state", "california", "nevada"));
        dictionary.add(new RegexUrlToken("year", "(19\\d\\d)|(20\\d\\d)"));
        this.tokenResolver = dictionary;
    }

    @Test
    public void constructorTest() {
        final String rule = "/a/b/c/";
        final UrlRule urlRule = new UrlRule(tokenResolver, mavenModule, "localhost:80", rule);

        final List<UrlRuleSegment> segments = urlRule.getSegments();
        assertEquals(segments.size(), 4);
        assertSame(urlRule.getMavenModule(), mavenModule);
        assertSame(urlRule.getVipAddress(), "localhost:80");
        assertSame(urlRule.getRule(), rule);
    }

    public void constructorTestInvalid() {
        try {
            new UrlRule(tokenResolver, mavenModule, "localhost:80", "");
        } catch(IllegalArgumentException e) {
            assertEquals(e.getMessage(), "rule must start with a /");
        }
    }

    @Test
    public void getSegmentsTest() {
        final UrlRule urlRule = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/");
        final List<UrlRuleSegment> segments = urlRule.getSegments();
        assertEquals(segments.size(), 4);
        assertEquals(segments.get(0).getSegment(), "a");
        assertEquals(segments.get(1).getSegment(), "b");
        assertEquals(segments.get(2).getSegment(), "c");
        assertEquals(segments.get(3).getSegment(), "");

        assertEquals(segments.get(0).getSegmentType(), COMPLETE);
        assertEquals(segments.get(1).getSegmentType(), COMPLETE);
        assertEquals(segments.get(2).getSegmentType(), COMPLETE);
        assertEquals(segments.get(3).getSegmentType(), EMPTY);
    }

    @Test
    public void getLastSegmentTest() {
        final UrlRule urlRule = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/");
        final UrlRuleSegment lastSegment = urlRule.getLastSegment();

        assertEquals(lastSegment.getSegment(), "");
        assertEquals(lastSegment.getSegmentType(), EMPTY);
    }

    public void equalsTest() {
        final UrlRule a1 = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");
        final UrlRule a2 = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/b/");

        final Object nullValue = null;
        final Object stringValue = "/a/";

        assertEquals(a1, a1);
        assertEquals(a1, a2);
        assertFalse(a1.equals(b));
        assertFalse(a1.equals(nullValue));
        assertFalse(a1.equals(stringValue));
    }

    public void hashCodeTest() {
        final UrlRule a1 = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");
        final UrlRule a2 = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/b/");

        assertEquals(a1.hashCode(), a1.hashCode());
        assertEquals(a1.hashCode(), a2.hashCode());
        assertTrue(a1.hashCode() != b.hashCode());
    }

    public void toStringTest() {
        final String rule = "/a/b/c/";

        assertEquals(new UrlRule(tokenResolver, mavenModule, "localhost:80", rule).toString(), rule);
    }

    public void compareToTestIdentical() {
        final UrlRule a1 = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");
        final UrlRule a2 = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");

        assertEquals(a1.compareTo(a2), IDENTICAL);
    }

    public void compareToTestDistinctSimple() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/b/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctLength1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctLength2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctLengthStar1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/*");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctLengthStar2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/*");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctDoubleStarPostfix1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/d/e/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/**/x/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctDoubleStarPostfix2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/**/x/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/c/d/e/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctDoubleStarLength1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/c/d/a/b/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctDoubleStarLength2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/c/d/a/b/");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestDistinctFilename() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/index.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/start.html");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestPrioritySimpleHigh() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/start.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/s*.html");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestPrioritySimpleLow() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/s*.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/start.html");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestPrioritySimplePathHigh() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/start/index.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/s*/index.html");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestPrioritySimplePathLow() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/s*/index.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/start/index.html");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestOverlapComplex() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/b/**/index.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/c/**/index.html");

        assertEquals(a.compareTo(b), OVERLAP);
    }

    public void compareToTestSelectFilename1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/index.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestSelectFilename2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/index.html");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestFilenameWins1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/index.html");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/**");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestFilenameWins2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/b/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/a/**/index.html");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestMakeWins1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[make]/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/**");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestMakeWins2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[make]/**");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestSpecificMakeWins1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/ford/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[make]/**");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestSpecificMakeWins2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[make]/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/ford/**");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestSpecificNonMakeDistinct() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[make]/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/blah/**");

        assertEquals(a.compareTo(b), DISTINCT);
    }

    public void compareToTestSpecificYearWins1() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/1999/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[year]/**");

        assertEquals(a.compareTo(b), HIGH_PRIORITY);
    }

    public void compareToTestSpecificYearWins2() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[year]/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/1999/**");

        assertEquals(a.compareTo(b), LOW_PRIORITY);
    }

    public void compareToTestSpecificNonYearDistinct() {
        final UrlRule a = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/[year]/**");
        final UrlRule b = new UrlRule(tokenResolver, mavenModule, "localhost:80", "/199/**");

        assertEquals(a.compareTo(b), DISTINCT);
    }
}
