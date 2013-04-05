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
package com.edmunds.etm.apache.configbuilder;

import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import com.edmunds.etm.rules.api.WebServerConfigurationBuilder;
import com.edmunds.etm.runtime.api.Application;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Collection;

/**
 * {@link com.edmunds.etm.rules.api.WebServerConfigurationBuilder} interface implementation.
 *
 * @author David Trott
 */
@Component
public class ApacheConfigurationBuilder implements WebServerConfigurationBuilder {

    /**
     * RewriteEngine keyword.
     */
    private static final String REWRITE_ENGINE_KEYWORD = "RewriteEngine on";

    private UrlTokenResolver urlTokenResolver;

    private byte[] activeRuleSetData;
    private String activeRuleSetDigest;

    public ApacheConfigurationBuilder() {
        this.activeRuleSetData = new byte[0];
        this.activeRuleSetDigest = "";
    }

    /**
     * Sets the url token resolver.
     *
     * @param urlTokenResolver the url token resolver
     */
    @Autowired
    public void setUrlTokenResolver(UrlTokenResolver urlTokenResolver) {
        this.urlTokenResolver = urlTokenResolver;
    }

    @Override
    public String getZooKeeperNodeName() {
        return "apache";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] build(Collection<Application> applications, Collection<UrlRule> rules) {
        final StringBuilder builder = new StringBuilder();

        builder.append(REWRITE_ENGINE_KEYWORD);
        builder.append("\n");

        for (UrlRule rule : rules) {
            builder.append("RewriteRule ");
            rule.toRegEx(urlTokenResolver, builder);
            builder.append(" http://").append(rule.getVipAddress()).append("$0 [P]\n");
        }

        final byte[] ruleSet = builder.toString().getBytes(Charset.forName("UTF8"));
        updateActiveRuleSet(ruleSet);
        // Deploy configuration to the web tier
        return ruleSet;
    }

    @Override
    public synchronized byte[] getActiveRuleSetData() {
        return activeRuleSetData == null ? null : activeRuleSetData.clone();
    }

    @Override
    public synchronized String getActiveRuleSetDigest() {
        return activeRuleSetDigest;
    }

    private synchronized void updateActiveRuleSet(byte[] data) {
        activeRuleSetData = data;
        activeRuleSetDigest = data != null ? DigestUtils.md5Hex(data) : "";
    }
}
