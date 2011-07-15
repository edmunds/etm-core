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
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.Validate;

/**
 * A UrlRule that has been blocked from the active set by one or more conflicting rules.
 *
 * @author Ryan Holmes
 */
public class BlockedUrlRule {

    private UrlRule urlRule;
    private Set<UrlRule> blockingRules = new HashSet<UrlRule>();

    public BlockedUrlRule(UrlRule urlRule, Set<UrlRule> blockingRules) {
        Validate.notNull(urlRule, "Blocked rule is null");
        Validate.notNull(blockingRules, "Blocking rules are null");
        this.urlRule = urlRule;
        this.blockingRules = blockingRules;
    }

    public MavenModule getMavenModule() {
        return urlRule.getMavenModule();
    }

    public String getRule() {
        return urlRule.getRule();
    }

    public String getVipAddress() {
        return urlRule.getVipAddress();
    }

    public Set<UrlRule> getBlockingRules() {
        return blockingRules;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof BlockedUrlRule)) {
            return false;
        }

        BlockedUrlRule that = (BlockedUrlRule) o;

        return urlRule.equals(that.urlRule);
    }

    @Override
    public int hashCode() {
        return urlRule.hashCode();
    }
}
