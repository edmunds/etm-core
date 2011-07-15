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

import com.edmunds.etm.apache.configbuilder.ApacheConfigurationBuilder;
import com.edmunds.etm.common.api.AgentInstance;
import com.edmunds.etm.common.api.RuleSetDeploymentEvent;
import com.edmunds.etm.common.api.RuleSetDeploymentResult;
import com.edmunds.etm.system.impl.AgentMonitor;
import com.edmunds.etm.web.panel.AgentDetailPanel;
import com.edmunds.etm.web.util.EtmFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.click.ActionListener;
import org.apache.click.Context;
import org.apache.click.Control;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Displays the currently connected agents.
 *
 * @author Ryan Holmes
 */
@Component
public class AgentsPage extends BorderPage {

    private final AgentMonitor agentMonitor;
    private final ApacheConfigurationBuilder apacheConfigurationBuilder;
    private final ActionLink viewLink;
    private final AgentDetailPanel agentDetailPanel;

    private AgentInstance selectedAgent;

    @Autowired
    public AgentsPage(AgentMonitor agentMonitor,
                      ApacheConfigurationBuilder apacheConfigurationBuilder) {
        this.agentMonitor = agentMonitor;
        this.apacheConfigurationBuilder = apacheConfigurationBuilder;

        // View link
        viewLink = new ActionLink("view");
        viewLink.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onViewClick();
            }
        });
        addControl(viewLink);

        // Agent detail panel
        agentDetailPanel = new AgentDetailPanel("agentDetailPanel");
        addControl(agentDetailPanel);

        // Agents table
        Table agentsTable = buildAgentsTable();
        addControl(agentsTable);
    }

    @Override
    public String getTitle() {
        return "Agents";
    }

    protected AgentMonitor getAgentMonitor() {
        return agentMonitor;
    }

    protected boolean onViewClick() {
        String id = viewLink.getValue();

        AgentInstance agent = getAgentInstanceById(id);
        selectedAgent = agent;
        agentDetailPanel.setAgent(agent);

        return true;
    }

    private Table buildAgentsTable() {

        Table table = new AgentsTable("agentsTable");
        table.setClass(Table.CLASS_ITS);

        Column hostNameColumn = new Column("hostName", "Host Name");
        hostNameColumn.setSortable(true);
        hostNameColumn.setDecorator(new Decorator() {
            @Override
            public String render(Object object, Context context) {
                AgentInstance agent = (AgentInstance) object;
                return "<a href='" + viewLink.getHref(agent.getId()) + "'>" + agent.getHostName() + "</a>";
            }
        });
        table.addColumn(hostNameColumn);

        Column ipAddressColumn = new Column("ipAddress", "IP Address");
        ipAddressColumn.setSortable(true);
        table.addColumn(ipAddressColumn);

        Column ruleSetDigestColumn = new Column("activeRuleSetDigest", "Active Rule Set Digest");
        ruleSetDigestColumn.setSortable(true);
        ruleSetDigestColumn.setDecorator(new RuleSetDigestDecorator());
        table.addColumn(ruleSetDigestColumn);

        Column lastDeploymentDateColumn = new Column("lastDeploymentEvent.eventDate", "Last Deployment Date");
        lastDeploymentDateColumn.setSortable(true);
        lastDeploymentDateColumn.setFormat(EtmFormat.DATE_TIME_MESSAGE_PATTERN);
        table.addColumn(lastDeploymentDateColumn);

        Column lastResultColumn = new Column("lastDeploymentEvent.result", "Last Deployment Result");
        lastResultColumn.setSortable(true);
        lastResultColumn.setDecorator(new LastDeploymentResultDecorator());
        table.addColumn(lastResultColumn);

        table.setDataProvider(new DataProvider<AgentInstance>() {
            @Override
            public Iterable<AgentInstance> getData() {
                return getAgentMonitor().getConnectedAgents();
            }
        });

        return table;
    }

    protected AgentInstance getSelectedAgent() {
        return selectedAgent;
    }

    protected boolean ruleSetMatchesController(String ruleSetDigest) {
        return ruleSetDigest != null && ruleSetDigest.equals(apacheConfigurationBuilder.getActiveRuleSetDigest());
    }

    private AgentInstance getAgentInstanceById(String id) {
        UUID agentId;

        try {
            agentId = UUID.fromString(id);
        } catch(IllegalArgumentException e) {
            return null;
        }

        AgentInstance match = null;

        Set<AgentInstance> agents = agentMonitor.getConnectedAgents();
        for(AgentInstance agent : agents) {
            if(agent.getId().equals(agentId)) {
                match = agent;
            }
        }

        return match;
    }

    private class AgentsTable extends Table {

        public AgentsTable(String name) {
            super(name);
        }

        @Override
        protected void addRowAttributes(Map<String, String> attributes, Object row, int rowIndex) {
            if(!(row instanceof AgentInstance)) {
                return;
            }

            AgentInstance agent = (AgentInstance) row;
            if(agent.equals(getSelectedAgent())) {
                attributes.put("class", "selected");
            }
        }
    }

    private class RuleSetDigestDecorator implements Decorator {

        @Override
        public String render(Object object, Context context) {
            if(!(object instanceof AgentInstance)) {
                return "";
            }

            AgentInstance agent = (AgentInstance) object;
            String digest = agent.getActiveRuleSetDigest();
            boolean matches = ruleSetMatchesController(digest);
            String cssClass = matches ? "statusOk" : "statusWarn";

            return "<div class='" + cssClass + "'>" + digest + "</div>";
        }
    }

    private class LastDeploymentResultDecorator implements Decorator {
        @Override
        public String render(Object object, Context context) {
            if(!(object instanceof AgentInstance)) {
                return "";
            }

            AgentInstance agent = (AgentInstance) object;
            RuleSetDeploymentEvent event = agent.getLastDeploymentEvent();
            if(event == null) {
                return "";
            }

            RuleSetDeploymentResult result = event.getResult();
            if(result == null) {
                result = RuleSetDeploymentResult.UNKNOWN;
            }
            return createResultDiv(result);
        }

        private String createResultDiv(RuleSetDeploymentResult result) {
            Validate.notNull(result, "Result is null");

            String cssClass;
            switch(result) {
                case OK:
                    cssClass = "statusOk";
                    break;
                case UNKNOWN:
                    cssClass = "statusUnknown";
                    break;
                case HEALTH_CHECK_FAILED:
                case RESTART_COMMAND_FAILED:
                case ROLLBACK_FAILED:
                case SYNTAX_CHECK_FAILED:
                    cssClass = "statusError";
                    break;
                default:
                    cssClass = "statusUnknown";
            }

            String text = getFormat().string(result);
            return "<div class='" + cssClass + "'>" + text + "</div>";
        }
    }
}
