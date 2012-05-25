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

import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

/**
 * The calss extends {@link RewriteRule} for Apache HTTP server. It provides
 * building Apache-specfic rule representation.
 * <p/>
 * Copyright (C) 2010 Edmunds.com
 * <p/>
 * Date: Apr 8, 2010:6:21:44 PM
 *
 * @author Dmytro Seredenko
 */

public class ApacheRewriteRule extends RewriteRule {
    /**
     * Proxy option.
     */
    public static final String PROXY_OPTION = "P";

    static final String APACHE_RULE_PATTERN = "RewriteRule {0} http://{1}$0 {2}";

    /**
     * Constructor allows to create representation of rewrite rule.
     * <p/>
     * E.g. {@code ^/tmv/toyota/([^/]*)$ http://127.0.0.1:8080$0 [P]}
     * <p/>
     * where: <ul> <li>{@code ^/tmv/toyota/([^/]*)$} is a pattern <li> {@code http://127.0.0.1:8080$0} is a substitution
     * <li> {@code P} is an option </ul>
     *
     * @param pattern      pattern.
     * @param substitution substitution.
     * @param options      options.
     */
    public ApacheRewriteRule(String pattern, String substitution, String... options) {
        super(pattern, substitution, options);
    }

    /**
     * The Apache specific rewrite rule will be returned. E.g. {@code ^/tmv/toyota/([^/]*)$ http://127.0.0.1:8080$0
     * [P]}
     *
     * @return Apache-like rule built from the current object.
     */
    @Override
    public String build() {
        return MessageFormat
                .format(APACHE_RULE_PATTERN, getPattern(), getSubstitution(), buildOptionsList(getOptions())).trim();
    }

    /**
     * Build options list.
     *
     * @param options options array.
     * @return list of rules in string format.
     */
    private String buildOptionsList(String[] options) {
        if (options != null && options.length > 0) {
            return "[" + StringUtils.join(options, ",") + "]";
        }
        return "";
    }
}
