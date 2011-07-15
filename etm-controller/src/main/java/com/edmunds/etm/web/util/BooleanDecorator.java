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
package com.edmunds.etm.web.util;

import org.apache.click.Context;
import org.apache.click.control.Decorator;
import org.apache.click.util.PropertyUtils;
import org.apache.commons.lang.Validate;

/**
 * A BooleanDecorator renders boolean values as "Yes" or "No".
 *
 * @author Ryan Holmes
 */
public class BooleanDecorator implements Decorator {
    private String propertyName;

    public BooleanDecorator(String propertyName) {
        Validate.notEmpty(propertyName, "Property name is empty");
        this.propertyName = propertyName;
    }

    @Override
    public String render(Object object, Context context) {
        Object obj = PropertyUtils.getValue(object, propertyName);

        if (!(obj instanceof Boolean)) {
            return "";
        }

        Boolean value = (Boolean) obj;
        if (value) {
            return "Yes";
        } else {
            return "No";
        }
    }
}
