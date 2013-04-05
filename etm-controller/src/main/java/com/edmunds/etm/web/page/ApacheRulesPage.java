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

import com.edmunds.etm.rules.impl.AgentConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Displays the current Apache web server configuration.
 *
 * @author Ryan Holmes
 */
@Component
public class ApacheRulesPage extends BorderPage {

    private final AgentConfigurationManager agentConfigurationManager;

    @Autowired
    public ApacheRulesPage(AgentConfigurationManager agentConfigurationManager) {
        this.agentConfigurationManager = agentConfigurationManager;
    }

    @Override
    public String getTitle() {
        return "Apache Rules";
    }

    @Override
    public void onRender() {
        addModel("ruleSetLines", agentConfigurationManager.getActiveRuleSetLines("apache"));
        addModel("ruleSetDigest", getRuleSetDigest());
    }

    private String getRuleSetDigest() {
        String digest = agentConfigurationManager.getActiveRuleSetDigest();
        return digest != null ? digest : "";
    }
}
