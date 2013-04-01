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
package com.edmunds.etm.rules.impl;

import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.WebServerConfigurationBuilder;
import com.edmunds.etm.runtime.api.Application;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.util.ZooKeeperUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements the high level logic to co-ordinate agent updates.
 *
 * @author David Trott
 */
@Component
public class AgentConfigurationManager {
    private static final Logger logger = Logger.getLogger(AgentConfigurationManager.class);

    private Map<String, WebServerConfigurationBuilder> webServerConfigurationBuilders;
    private ControllerPaths controllerPaths;
    private ZooKeeperConnection connection;

    public AgentConfigurationManager() {
    }

    public byte[] getActiveRuleSetData() {
        return getActiveRuleSetData("apache");
    }

    public String getActiveRuleSetDigest() {
        return getActiveRuleSetDigest("apache");
    }

    public byte[] getActiveRuleSetData(String name) {
        return webServerConfigurationBuilders.get(name).getActiveRuleSetData();
    }

    public String getActiveRuleSetDigest(String name) {
        return webServerConfigurationBuilders.get(name).getActiveRuleSetDigest();
    }

    public Set<String> getActiveRuleSetDigests() {
        final Set<String> digests = Sets.newHashSet();

        for (final WebServerConfigurationBuilder builder : webServerConfigurationBuilders.values()) {
            final String digest = builder.getActiveRuleSetDigest();
            if (StringUtils.isNotBlank(digest)) {
                digests.add(digest);
            }
        }

        return digests;
    }

    /**
     * Sets the collection of builders for web servers.
     *
     * @param builders typically there are two builds (apache and ha-proxy).
     */
    @Autowired
    public void mapWebServerConfigurationBuilders(List<WebServerConfigurationBuilder> builders) {
        this.webServerConfigurationBuilders = Maps.newHashMap();
        for (final WebServerConfigurationBuilder builder : builders) {
            webServerConfigurationBuilders.put(builder.getZooKeeperNodeName(), builder);
        }
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
    public void build(Set<Application> applications, Collection<UrlRule> rules) {
        for (final WebServerConfigurationBuilder builder : webServerConfigurationBuilders.values()) {
            final String zooKeeperPath = controllerPaths.getWebConf() + "/" + builder.getZooKeeperNodeName();

            deployConfiguration(zooKeeperPath, builder.build(applications, rules));
        }
    }

    private void deployConfiguration(String nodePath, final byte[] configData) {

        AsyncCallback.StatCallback cb = new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int rc, String path, Object ctx, Stat stat) {
                onConfigurationSetData(KeeperException.Code.get(rc), path, configData);
            }
        };
        connection.setData(nodePath, configData, -1, cb, null);

        if (logger.isDebugEnabled()) {
            String message = String.format("New Apache configuration generated: \n%s", new String(configData));
            logger.debug(message);
        }
    }

    protected void onConfigurationSetData(KeeperException.Code rc, String path, byte[] data) {
        if (rc == KeeperException.Code.OK) {
            return;
        }

        if (ZooKeeperUtils.isRetryableError(rc)) {
            // Retry recoverable errors
            logger.warn(String.format("Error %s while setting node %s, retrying", rc, path));
            deployConfiguration(path, data);
        } else {
            // Log other errors
            logger.error(String.format("Error %s while setting node %s", rc, path));
        }
    }
}
