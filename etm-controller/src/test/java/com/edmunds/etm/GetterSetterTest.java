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
package com.edmunds.etm;

import com.edmunds.autotest.AutoTestGetterSetter;
import org.testng.annotations.Test;

/**
 * Tests the setters and getters of all the beans.
 * <p/>
 * Copyright (C) 2008 Edmunds.com
 *
 * @author David Trott
 */
@Test
public class GetterSetterTest {

    @Test
    public void testAll() {
        new AutoTestGetterSetter(null, "com.edmunds.etm.apache").validateAll();
        new AutoTestGetterSetter(null, "com.edmunds.etm.loadbalancer").validateAll();
        new AutoTestGetterSetter(null, "com.edmunds.etm.management").validateAll();
        new AutoTestGetterSetter(null, "com.edmunds.etm.rules").validateAll();
        new AutoTestGetterSetter(null, "com.edmunds.etm.runtime").validateAll();
        new AutoTestGetterSetter(null, "com.edmunds.etm.system").validateAll();
    }
}
