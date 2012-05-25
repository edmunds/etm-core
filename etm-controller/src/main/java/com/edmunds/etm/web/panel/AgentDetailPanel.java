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

import com.edmunds.etm.common.api.AgentInstance;
import com.edmunds.etm.common.api.RuleSetDeploymentEvent;
import org.apache.click.control.Panel;

/**
 * Displays agent details.
 *
 * @author Ryan Holmes
 */
public class AgentDetailPanel extends Panel {

    public AgentDetailPanel(String name) {
        super(name);
        setTemplate("/panel/agent-detail-panel.htm");
    }

    public void setAgent(AgentInstance agent) {
        if (agent == null) {
            return;
        }
        addModel("hostName", agent.getHostName());
        addModel("ipAddress", agent.getIpAddress());
        addModel("version", agent.getVersion());
        addModel("activeRuleSetDigest", agent.getActiveRuleSetDigest());

        RuleSetDeploymentEvent lastDeployment = agent.getLastDeploymentEvent();
        if (lastDeployment != null) {
            addModel("lastDeployment", lastDeployment);
        }

        RuleSetDeploymentEvent lastFailedDeployment = agent.getLastFailedDeploymentEvent();
        if (lastFailedDeployment != null) {
            addModel("lastFailedDeployment", lastFailedDeployment);
        }
    }
}
