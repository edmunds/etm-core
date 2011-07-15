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

import org.testng.annotations.Test;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for the {@link ApplicationVersion} class.
 *
 * @author Ryan Holmes
 */
@Test
public class ApplicationVersionTest {

    @Test
    public void contructValid() {
        new ApplicationVersion("1.0.0");
        new ApplicationVersion("1,0,0");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void constructInvalid1() {
        new ApplicationVersion(null);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void constructInvalid2() {
        new ApplicationVersion("");
    }

    @Test
    public void testEquality() {
        ApplicationVersion v1 = new ApplicationVersion("1.0.0");
        ApplicationVersion v2 = new ApplicationVersion("1.0.0");
        ApplicationVersion v3 = new ApplicationVersion("1.0.1");

        assertTrue(v1.equals(v2));
        assertFalse(v1.equals(v3));
    }

    @Test
    public void testComparison() {
        ApplicationVersion v1 = new ApplicationVersion("1.0.0");
        ApplicationVersion v2 = new ApplicationVersion("1.0.1");
        ApplicationVersion v3 = new ApplicationVersion("1.1.0");
        ApplicationVersion v4 = new ApplicationVersion("2.0.0");

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v3) < 0);
        assertTrue(v3.compareTo(v4) < 0);
    }

}
