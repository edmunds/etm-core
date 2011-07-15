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

import com.edmunds.etm.apache.domain.ApacheRewriteRule;
import com.edmunds.etm.apache.domain.RewriteRule;
import com.edmunds.etm.apache.rule.builder.ApacheRuleBuilder;
import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.WebServerConfigurationBuilder;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.util.ZooKeeperUtils;
import com.google.common.collect.Sets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Set;

/**
 * {@link com.edmunds.etm.rules.api.WebServerConfigurationBuilder} interface implementation.
 *
 * @author Aliaksandr Savin
 * @author Ryan Holmes
 */
@Component
public class ApacheConfigurationBuilder implements WebServerConfigurationBuilder {

    private static final Logger logger = Logger.getLogger(ApacheConfigurationBuilder.class);

    /**
     * RewriteEngine keyword.
     */
    private static final String REWRITE_ENGINE_KEYWORD = "RewriteEngine on";

    private ApacheRuleBuilder ruleBuilder;
    private ControllerPaths controllerPaths;
    private ZooKeeperConnection connection;
    private byte[] activeRuleSetData;
    private String activeRuleSetDigest;

    public ApacheConfigurationBuilder() {
        this.activeRuleSetData = new byte[0];
        this.activeRuleSetDigest = "";
    }

    public byte[] getActiveRuleSetData() {
        return activeRuleSetData;
    }

    public String getActiveRuleSetDigest() {
        return activeRuleSetDigest;
    }

    /**
     * Sets rule builder.
     *
     * @param ruleBuilder rule builder
     */
    @Autowired
    void setRuleBuilder(ApacheRuleBuilder ruleBuilder) {
        this.ruleBuilder = ruleBuilder;
    }

    /**
     * Sets the controller paths.
     *
     * @param controllerPaths controller paths
     */
    @Autowired
    void setControllerPaths(ControllerPaths controllerPaths) {
        this.controllerPaths = controllerPaths;
    }

    /**
     * Sets the ZooKeeper connection.
     *
     * @param connection the ZooKeeper connection
     */
    @Autowired
    void setConnection(ZooKeeperConnection connection) {
        this.connection = connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void build(Collection<UrlRule> rules) {

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));

            Set<RewriteRule> config = Sets.newLinkedHashSet();

            for (UrlRule rule : rules) {
                String transformedRule = ruleBuilder.build(rule.getRule());
                RewriteRule rewriteRule = new ApacheRewriteRule(transformedRule,
                    rule.getVipAddress(),
                    ApacheRewriteRule.PROXY_OPTION);
                config.add(rewriteRule);
            }

            bw.append(REWRITE_ENGINE_KEYWORD);
            for (RewriteRule rewriteRule : config) {
                if (rewriteRule != null) {
                    bw.newLine();
                    bw.append(rewriteRule.build());
                }
            }
            bw.newLine();
            bw.close();

            // Deploy configuration to the web tier
            byte[] configData = out.toByteArray();
            deployConfiguration(controllerPaths.getApacheConf(), configData);
        } catch (IOException e) {
            // Never happen.
            throw new RuntimeException(e);
        }
    }

    private void deployConfiguration(String nodePath, final byte[] configData) {

        AsyncCallback.StatCallback cb = new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, Stat stat) {
                onConfigurationSetData(Code.get(rc), path, configData);
            }
        };

        connection.setData(nodePath, configData, -1, cb, null);

        if (logger.isDebugEnabled()) {
            String message = String.format("New Apache configuration generated: \n%s", new String(configData));
            logger.debug(message);
        }
    }

    protected void onConfigurationSetData(Code rc, String path, byte[] data) {
        if (rc == Code.OK) {
            updateActiveRuleSet(data);
        } else if (ZooKeeperUtils.isRetryableError(rc)) {

            // Retry recoverable errors
            logger.warn(String.format("Error %s while setting node %s, retrying", rc, path));
            deployConfiguration(path, data);
        } else {
            // Log other errors
            logger.error(String.format("Error %s while setting node %s", rc, path));
        }
    }

    private void updateActiveRuleSet(byte[] data) {
        activeRuleSetData = data;
        activeRuleSetDigest = data != null ? DigestUtils.md5Hex(data) : "";
    }
}
