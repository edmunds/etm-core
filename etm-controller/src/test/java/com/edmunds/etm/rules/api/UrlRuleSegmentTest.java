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

import org.testng.annotations.Test;

import static com.edmunds.etm.rules.api.RuleComparison.DISTINCT;
import static com.edmunds.etm.rules.api.RuleComparison.HIGH_PRIORITY;
import static com.edmunds.etm.rules.api.RuleComparison.IDENTICAL;
import static com.edmunds.etm.rules.api.RuleComparison.LOW_PRIORITY;
import static com.edmunds.etm.rules.api.RuleComparison.OVERLAP;
import static com.edmunds.etm.rules.api.SegmentType.COMPLETE;
import static com.edmunds.etm.rules.api.SegmentType.DOUBLE_STAR;
import static com.edmunds.etm.rules.api.SegmentType.EMPTY;
import static com.edmunds.etm.rules.api.SegmentType.STAR;
import static com.edmunds.etm.rules.api.SegmentType.TOKEN;
import static com.edmunds.etm.rules.api.SegmentType.WILDCARD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

/**
 * Url Rule Segment Test.
 *
 * @author David Trott
 */
@Test
public class UrlRuleSegmentTest {

    @Test
    public void constructorTestNull() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, null);

        assertSame(segment.getSegmentType(), DOUBLE_STAR);
        assertNull(resolver.resolveToken(segment.getSegment()));
    }

    @Test
    public void constructorTestEmpty() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "");

        assertSame(segment.getSegmentType(), EMPTY);
        assertNull(resolver.resolveToken(segment.getSegment()));
    }

    @Test
    public void constructorTestStar() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "*");

        assertSame(segment.getSegmentType(), STAR);
        assertNull(resolver.resolveToken(segment.getSegment()));
    }

    @Test
    public void constructorTestDoubleStar() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "**");

        assertSame(segment.getSegmentType(), DOUBLE_STAR);
        assertNull(resolver.resolveToken(segment.getSegment()));
    }

    @Test
    public void constructorTestComplete() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "hello.html");

        assertSame(segment.getSegmentType(), COMPLETE);
        assertNull(resolver.resolveToken(segment.getSegment()));
    }

    @Test
    public void constructorTestWild() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "*.html");

        assertSame(segment.getSegmentType(), WILDCARD);
        assertNull(resolver.resolveToken(segment.getSegment()));
    }

    @Test
    public void constructorTestPattern() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();

        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "[year]");

        assertSame(segment.getSegmentType(), TOKEN);
        assertSame(resolver.resolveToken(segment.getSegment()), "(19|20)\\d{2}");
    }

    @Test
    public void getSegmentTest() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();

        assertEquals(new UrlRuleSegment(resolver, "hello.world").getSegment(), "hello.world");
    }

    @Test
    public void compareToTestIdentical() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), IDENTICAL);
    }

    @Test
    public void compareToTestNotIdentical() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "world.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestMeDoubleStar() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "**");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestOtherDoubleStar() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "**");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestOtherStar1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "*");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestOtherStar2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "*");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestOtherStar3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "**");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "*");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestMeStar1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "*");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestMeStar2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "*");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*.html");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestMeStar3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "*");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "**");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestOtherEmpty1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestOtherEmpty2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestOtherEmpty3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "**");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestMeEmpty1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestMeEmpty2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestMeEmpty3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "**");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestMeWild1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestMeWild2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestMeWild3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "world*");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestMeWild4() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*.js");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestMeWild5() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello2*.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestOtherWild1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*.html");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestOtherWild2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestOtherWild3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "world*");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestOtherWild4() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();

        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*.js");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestOtherWild5() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello2*.html");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestWildPostFix1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*2.html");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestWildPostFix2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "hello*1.html");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "hello*.html");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void validateFormatTestInvalidTwoStars() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        try {
            new UrlRuleSegment(resolver, "a*.*b");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Two stars detected: a*.*b");
        }
    }

    @Test
    public void validateFormatTestInvalidNoDot() {
        try {
            UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
            new UrlRuleSegment(resolver, "hello", true);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Filename must contain either a dot or a star: hello");
        }
    }

    @Test
    public void validateFormatTestInvalidIncorrectStar() {
        try {
            UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
            new UrlRuleSegment(resolver, "hello*world", true);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "File names that do not contain '.' must end with a star: hello*world");
        }
    }

    @Test
    public void validateFormatTestValidEndWithStar() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        new UrlRuleSegment(resolver, "hello*", true);
    }

    @Test
    public void toStringTest() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        assertEquals(new UrlRuleSegment(resolver, "hello*").toString(),
                "UrlRuleSegment{segment='hello*', segmentType=WILDCARD}");
    }

    @Test
    public void equalsTestSame() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final UrlRuleSegment segment = new UrlRuleSegment(resolver, "a*");
        assertEquals(segment, segment);
    }

    @Test
    public void equalsTestEqual() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        assertEquals(new UrlRuleSegment(resolver, "a*"), new UrlRuleSegment(resolver, "a*"));
    }

    @Test
    public void equalsTestDifferent() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        assertFalse(new UrlRuleSegment(resolver, "a*").equals(new UrlRuleSegment(resolver, "b*")));
    }

    @Test
    public void equalsTestDifferentClass() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        final Object obj = "a*";
        assertFalse(new UrlRuleSegment(resolver, "a*").equals(obj));
    }

    @Test
    public void hashCodeTest() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        assertEquals(new UrlRuleSegment(resolver, "a*").hashCode(), "a*".hashCode());
    }

    @Test
    public void compareToTestTwoPatterns() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "[make]");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "[model]");

        assertSame(me.compareTo(other), OVERLAP);
    }

    @Test
    public void compareToTestPatternWildcard1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "[make]");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "ford*");

        assertSame(me.compareTo(other), OVERLAP);
    }

    @Test
    public void compareToTestPatternWildcard2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "ford*");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "[make]");

        assertSame(me.compareTo(other), OVERLAP);
    }

    @Test
    public void compareToTestPatternComplete1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "1999");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "[year]");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestPatternComplete2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "[year]");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "2010");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestPatternComplete3() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "ford");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "[year]");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestPatternFixed1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "ford");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "[make]");

        assertSame(me.compareTo(other), HIGH_PRIORITY);
    }

    @Test
    public void compareToTestPatternFixed2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "[make]");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "ford");

        assertSame(me.compareTo(other), LOW_PRIORITY);
    }

    @Test
    public void compareToTestPatternFixedDistinct1() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "spoo");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "[make]");

        assertSame(me.compareTo(other), DISTINCT);
    }

    @Test
    public void compareToTestPatternFixedDistinct2() {
        UrlTokenResolver resolver = DefaultUrlTokenDictionary.newInstance();
        UrlRuleSegment me = new UrlRuleSegment(resolver, "[make]");
        UrlRuleSegment other = new UrlRuleSegment(resolver, "spoo");

        assertSame(me.compareTo(other), DISTINCT);
    }

}
