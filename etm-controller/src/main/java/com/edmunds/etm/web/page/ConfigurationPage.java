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
package com.edmunds.etm.web.page;

import com.edmunds.etm.web.panel.LoadBalancerConfigurationPanel;
import com.edmunds.etm.web.panel.ZooKeeperConfigurationPanel;
import com.edmunds.zookeeper.connection.ZooKeeperConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Displays ETM configuration settings.
 *
 * @author Ryan Holmes
 */
@Component
public class ConfigurationPage extends BorderPage {

    @Autowired
    public ConfigurationPage(
            ZooKeeperConfig zooKeeperConfig, LoadBalancerConfigurationPanel loadBalancerConfigurationPanel) {

        addControl(new ZooKeeperConfigurationPanel("zooKeeperConfigurationPanel", zooKeeperConfig));
        addControl(loadBalancerConfigurationPanel);
    }

    @Override
    public String getTitle() {
        return "Configuration";
    }
}
