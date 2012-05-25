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
package com.edmunds.etm.web.panel;

import com.edmunds.zookeeper.connection.ZooKeeperConfig;
import org.apache.click.control.Panel;
import org.apache.commons.lang.Validate;

import static org.apache.commons.lang.StringUtils.defaultString;

/**
 * @author Ryan Holmes
 */
public class ZooKeeperConfigurationPanel extends Panel {
    public ZooKeeperConfigurationPanel(String name, ZooKeeperConfig zooKeeperConfig) {
        super(name);
        setTemplate("/panel/zookeeper-configuration-panel.htm");

        Validate.notNull(zooKeeperConfig, "ZooKeeper config is null");
        addModel("hostName", defaultString(zooKeeperConfig.getHostName()));
        addModel("port", zooKeeperConfig.getPort());
        addModel("sessionTimeout", zooKeeperConfig.getSessionTimeout());
        addModel("pathPrefix", defaultString(zooKeeperConfig.getPathPrefix()));
        addModel("dnsRetryCount", zooKeeperConfig.getDnsRetryCount());
    }
}
