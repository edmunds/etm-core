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

import org.apache.commons.lang.Validate;

import java.util.Comparator;

/**
 * Comparator that alphabetically sorts UrlRule objects.
 */
public class AlphabeticUrlRuleComparator implements Comparator<UrlRule> {

    public static final AlphabeticUrlRuleComparator INSTANCE = new AlphabeticUrlRuleComparator();

    @Override
    public int compare(UrlRule rule1, UrlRule rule2) {
        Validate.notNull(rule1, "rule1 is null");
        Validate.notNull(rule2, "rule2 is null");

        return rule1.getRule().compareTo(rule2.getRule());
    }
}
