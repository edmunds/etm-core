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
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Displays the current HA Proxy web server configuration.
 *
 * @author Ryan Holmes
 */
@Component
public class HaProxyRulesPage extends BorderPage {

    private final AgentConfigurationManager agentConfigurationManager;

    @Autowired
    public HaProxyRulesPage(AgentConfigurationManager agentConfigurationManager) {
        this.agentConfigurationManager = agentConfigurationManager;
    }

    @Override
    public String getTitle() {
        return "HA Proxy Rules";
    }

    @Override
    public void onRender() {
        addModel("ruleSetLines", getRuleSetLines());
        addModel("ruleSetDigest", getRuleSetDigest());
    }

    private List<String> getRuleSetLines() {
        List<String> lines = Lists.newArrayList();

        byte[] ruleSetData = agentConfigurationManager.getActiveRuleSetData("haproxy");

        InputStream inputStream = new ByteArrayInputStream(ruleSetData);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            line = reader.readLine();
            while (line != null) {
                line = line.replaceAll("\\|", " | "); // allow line breaks in long regex rules
                lines.add(StringEscapeUtils.escapeHtml(line));
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lines;
    }

    private String getRuleSetDigest() {
        String digest = agentConfigurationManager.getActiveRuleSetDigest("haproxy");
        return digest != null ? digest : "";
    }
}
