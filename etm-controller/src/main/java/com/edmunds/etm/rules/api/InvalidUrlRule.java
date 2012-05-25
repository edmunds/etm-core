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

import com.edmunds.etm.management.api.MavenModule;
import org.apache.commons.lang.Validate;

/**
 * A raw URL rule that could not be turned into a valid {@link UrlRule} object.
 *
 * @author Ryan Holmes
 */
public class InvalidUrlRule {

    private MavenModule mavenModule;
    private String rule;

    public InvalidUrlRule(MavenModule mavenModule, String rule) {
        Validate.notNull(mavenModule, "Maven module is null");
        Validate.notNull(rule, "Rule is empty");
        this.mavenModule = mavenModule;
        this.rule = rule;
    }

    public MavenModule getMavenModule() {
        return mavenModule;
    }

    public String getRule() {
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvalidUrlRule)) {
            return false;
        }

        InvalidUrlRule that = (InvalidUrlRule) o;

        if (!mavenModule.equals(that.mavenModule)) {
            return false;
        }
        if (!rule.equals(that.rule)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mavenModule.hashCode();
        result = 31 * result + rule.hashCode();
        return result;
    }
}
