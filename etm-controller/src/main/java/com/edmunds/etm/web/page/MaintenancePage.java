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

import com.edmunds.etm.common.impl.UrlTokenRepository;
import com.edmunds.etm.loadbalancer.impl.LoadBalancerManager;
import com.edmunds.etm.rules.impl.UrlTokenMonitor;
import com.edmunds.etm.rules.impl.WebConfigurationManager;
import com.edmunds.etm.system.api.FailoverState;
import com.edmunds.etm.system.impl.FailoverMonitor;
import org.apache.click.ActionListener;
import org.apache.click.Control;
import org.apache.click.control.Button;
import org.apache.click.control.Form;
import org.apache.click.control.Submit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * Provides ETM maintenance operations.
 *
 * @author Ryan Holmes
 */
@Component
public class MaintenancePage extends BorderPage {

    private final FailoverMonitor failoverMonitor;
    private final UrlTokenRepository tokenRepository;
    private final LoadBalancerManager loadBalancerManager;
    private final WebConfigurationManager webConfigurationManager;

    private FailoverState failoverState;

    @Autowired
    public MaintenancePage(FailoverMonitor failoverMonitor,
                           UrlTokenRepository tokenRepository,
                           LoadBalancerManager loadBalancerManager,
                           WebConfigurationManager webConfigurationManager) {
        this.failoverMonitor = failoverMonitor;
        this.tokenRepository = tokenRepository;
        this.loadBalancerManager = loadBalancerManager;
        this.webConfigurationManager = webConfigurationManager;

        addModel("failoverState", getFailoverState());
        addControl(buildForm());
    }

    @Override
    public String getTitle() {
        return "Maintenance";
    }

    protected boolean onSuspendClick() {
        try {
            failoverMonitor.suspend();
        } catch (IllegalStateException e) {
            getContext().setFlashAttribute("error", e.getMessage());
        }

        setRedirect(MaintenancePage.class);
        return false;
    }

    protected boolean onResumeClick() {
        try {
            failoverMonitor.resume();
        } catch (IllegalStateException e) {
            getContext().setFlashAttribute("error", e.getMessage());
        }

        setRedirect(MaintenancePage.class);
        return false;
    }

    protected boolean onLoadDefaultUrlTokensClick() {

        String contextPath = getContext().getServletContext().getRealPath("/");
        File file = new File(contextPath + UrlTokenMonitor.DEFAULT_TOKENS_XML_PATH);
        try {
            tokenRepository.loadTokensFromFile(file, true);
            getContext().setFlashAttribute("info", "Default URL tokens loaded");
        } catch (IOException e) {
            getContext().setFlashAttribute("error", e.getMessage());
        }

        setRedirect(MaintenancePage.class);
        return false;
    }

    protected boolean onUpdateLoadBalancerClick() {
        loadBalancerManager.updateLoadBalancer();
        getContext().setFlashAttribute("info", "Load balancer update initiated");

        setRedirect(MaintenancePage.class);
        return false;
    }

    protected boolean onResetApacheRulesClick() {
        webConfigurationManager.recreateRules();
        getContext().setFlashAttribute("info", "Apache rule reset initiated");

        setRedirect(MaintenancePage.class);
        return false;
    }

    private FailoverState getFailoverState() {
        if (failoverState == null) {
            failoverState = failoverMonitor.getFailoverState();
        }

        return failoverState;
    }

    private Form buildForm() {
        Form form = new Form("form");
        form.setButtonAlign("right");

        form.add(buildSuspendButton());
        form.add(buildResumeButton());
        form.add(buildLoadDefaultUrlTokensButton());
        form.add(buildUpdateLoadBalancerButton());
        form.add(buildResetApacheRulesButton());

        return form;
    }

    private Button buildSuspendButton() {

        Submit button = new Submit("suspend");
        button.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onSuspendClick();
            }
        });
        button.setTitle("Suspends an active controller");
        button.setDisabled(getFailoverState() != FailoverState.ACTIVE);
        return button;
    }

    private Button buildResumeButton() {

        Submit button = new Submit("resume");
        button.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onResumeClick();
            }
        });
        button.setTitle("Resumes a suspended controller");
        button.setDisabled(getFailoverState() != FailoverState.SUSPENDED);
        return button;
    }

    private Button buildLoadDefaultUrlTokensButton() {

        Submit button = new Submit("loadDefaultTokens");
        button.setLabel("Load default tokens");
        button.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onLoadDefaultUrlTokensClick();
            }
        });
        button.setTitle("Loads the default URL tokens");
        button.setDisabled(getFailoverState() != FailoverState.ACTIVE);
        return button;
    }

    private Button buildUpdateLoadBalancerButton() {
        Submit button = new Submit("updateLoadBalancer");
        button.setLabel("Update load balancer");
        button.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onUpdateLoadBalancerClick();
            }
        });
        button.setTitle("Forces an update of the load balancer configuration");
        button.setDisabled(getFailoverState() != FailoverState.ACTIVE);
        return button;
    }

    private Button buildResetApacheRulesButton() {
        Submit button = new Submit("resetApacheRules");
        button.setLabel("Reset Apache rules");
        button.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onResetApacheRulesClick();
            }
        });
        button.setTitle("Forces recreation of the set of Apache rewrite rules");
        button.setDisabled(getFailoverState() != FailoverState.ACTIVE);
        return button;
    }
}
