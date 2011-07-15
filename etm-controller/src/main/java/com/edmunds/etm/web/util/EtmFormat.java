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

import org.apache.click.util.Format;
import org.apache.commons.lang.WordUtils;

import java.util.Date;

/**
 * Custom Click {@link org.apache.click.util.Format} class.
 * <p/>
 * This class must be defined in the click.xml file as follows:
 * <pre>
 * <format classname="com.mycorp.utils.MyFormat"/>
 * </pre>
 *
 * @author Ryan Holmes
 */
public class EtmFormat extends Format {

    public static final String DATE_TIME_PATTERN = "yyyy-MMM-dd HH:mm:ss";
    public static final String DATE_TIME_MESSAGE_PATTERN = "{0,date," + DATE_TIME_PATTERN + "}";

    @Override
    public String string(Object object) {

        if (object instanceof Enum) {
            return enumeration((Enum) object);
        }
        return super.string(object);
    }

    /**
     * Formats an enum value as a string of capitilized words.
     *
     * @param value the enum to format
     * @return formatted string
     */
    public String enumeration(Enum<?> value) {
        if (value == null) {
            return getEmptyString();
        }

        String str = value.toString().replace('_', ' ');
        return str.length() > 2 ? WordUtils.capitalizeFully(str) : str;
    }

    public String dateTime(Date date) {
        return date(date, DATE_TIME_PATTERN);
    }
}
