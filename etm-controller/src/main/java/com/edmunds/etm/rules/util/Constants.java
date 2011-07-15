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
package com.edmunds.etm.rules.util;

/**
 * Constants.
 *
 * @author Aliaksandr Savin
 */
public final class Constants {

    /**
     * Private constructor for prevent class instantiation.
     */
    private Constants() {
    }

    /**
     * Slash.
     */
    public static final String SLASH = "/";

    /**
     * Asterisk.
     */
    public static final String ASTERISK = "*";

    /**
     * Double asterisk.
     */
    public static final String DOUBLE_ASTERISK = "**";

    /**
     * Year.
     */
    public static final String YEAR_WILDCARD = "[year]";

    /**
     * Zip.
     */
    public static final String ZIPCODE_WILDCARD = "[zipcode]";

    /**
     * Make.
     */
    public static final String MAKE_WILDCARD = "[make]";

    /**
     * Make.
     */
    public static final String MODEL_WILDCARD = "[model]";

    /**
     * Region.
     */
    public static final String STATE_WILDCARD = "[state]";

    /**
     * Underscore.
     */
    public static final String UNDERSCORE = "_";

    /**
     * XML File extension.
     */
    public static final String XML_FILE_EXT = ".xml";

    /**
     * Regular expression for any symbols sequence.
     */
    public static final String ANY_SYMBOLS_REGEXP = ".*";

    /**
     * Regular expression for symbols sequence without slash.
     */
    public static final String ANY_SYMBOLS_EXCEPT_SLASH_REGEXP = "[^/]*";

    /**
     * Regular expression for year.
     */
    public static final String YEAR_REGEXP = "(19|20)\\d{2}";

    /**
     * Regular expression for zip-code.
     */
    public static final String ZIPCODE_REGEXP = "\\d{5}";
}
