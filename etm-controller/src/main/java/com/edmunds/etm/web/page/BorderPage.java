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

import com.edmunds.etm.system.api.ControllerInstance;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.etm.system.impl.ControllerMonitor;
import com.edmunds.etm.web.panel.ControllerInfoPanel;
import com.edmunds.etm.web.panel.FailoverWarningPanel;
import com.edmunds.etm.web.util.MenuPageLink;
import org.apache.click.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides site layout and navigation.
 *
 * @author Ryan Holmes
 */
@Component
public abstract class BorderPage extends Page {

    public BorderPage() {

        addModel("title", getTitle());

        addControl(new MenuPageLink("home", HomePage.class));
        addControl(new MenuPageLink("applications", ApplicationsPage.class));
        addControl(new MenuPageLink("urlRules", "URL Rules", UrlRulesPage.class));
        addControl(new MenuPageLink("urlTokens", "URL Tokens", UrlTokensPage.class));
        addControl(new MenuPageLink("apacheRules", ApacheRulesPage.class));
        addControl(new MenuPageLink("agents", AgentsPage.class));
        addControl(new MenuPageLink("configuration", ConfigurationPage.class));
    }

    @Override
    public String getTemplate() {
        return "border-template.htm";
    }

    public abstract String getTitle();

    @Autowired
    public void setControllerMonitor(ControllerMonitor controllerMonitor) {
        ControllerInstance controller = controllerMonitor.getLocalController();
        addControl(new ControllerInfoPanel("controllerInfoPanel", controller));

        FailoverState failoverState = controller != null ? controller.getFailoverState() : null;
        addControl(new FailoverWarningPanel("failoverWarningPanel", failoverState));
    }
}
