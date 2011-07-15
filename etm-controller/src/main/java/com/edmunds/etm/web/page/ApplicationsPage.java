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

import com.edmunds.etm.loadbalancer.api.AvailabilityStatus;
import com.edmunds.etm.loadbalancer.impl.LoadBalancerDataAccessService;
import com.edmunds.etm.runtime.api.Application;
import com.edmunds.etm.runtime.impl.ApplicationRepository;
import com.edmunds.etm.web.panel.ApplicationDetailPanel;
import com.edmunds.etm.web.util.BooleanDecorator;
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

import java.util.Map;

/**
 * Displays the list of all client applications.
 *
 * @author Ryan Holmes
 */
@Component
public class ApplicationsPage extends BorderPage {

    private final ApplicationRepository applicationRepository;
    private final LoadBalancerDataAccessService loadBalancerDataAccessService;
    private final ActionLink viewLink;
    private final ApplicationDetailPanel applicationDetailPanel;

    private Application selectedApplication;

    @Autowired
    public ApplicationsPage(ApplicationRepository applicationRepository,
                            LoadBalancerDataAccessService loadBalancerDataAccessService) {
        this.applicationRepository = applicationRepository;
        this.loadBalancerDataAccessService = loadBalancerDataAccessService;

        // View link
        viewLink = new ActionLink("view");
        viewLink.setActionListener(new ActionListener() {
            @Override
            public boolean onAction(Control source) {
                return onViewClick();
            }
        });
        addControl(viewLink);

        // Application detail panel
        applicationDetailPanel = new ApplicationDetailPanel("applicationDetailPanel");
        addControl(applicationDetailPanel);

        // Applications table
        Table applicationsTable = buildApplicationsTable();
        addControl(applicationsTable);
    }

    @Override
    public String getTitle() {
        return "Applications";
    }

    protected ApplicationRepository getApplicationRepository() {
        return applicationRepository;
    }

    protected boolean onViewClick() {
        String id = viewLink.getValue();
        Application app = applicationRepository.getApplicationById(id);

        selectedApplication = app;
        applicationDetailPanel.setApplication(app);

        return true;
    }

    protected Application getSelectedApplication() {
        return selectedApplication;
    }

    protected AvailabilityStatus getVirtualServerStatus(String serverName) {
        return loadBalancerDataAccessService.getAvailabilityStatus(serverName);
    }

    private Table buildApplicationsTable() {

        Table table = new ApplicationsTable("applicationsTable");
        table.setClass(Table.CLASS_ITS);

        Column nameColumn = new Column("name");
        nameColumn.setDecorator(new Decorator() {
            @Override
            public String render(Object object, Context context) {
                Application app = (Application) object;
                return "<a href='" + viewLink.getHref(app.getId()) + "'>" + app.getName() + "</a>";
            }
        });
        nameColumn.setSortable(true);
        table.addColumn(nameColumn);
        table.addColumn(new Column("version"));

        Column activeColumn = new Column("active");
        activeColumn.setSortable(true);
        activeColumn.setDecorator(new BooleanDecorator("active"));
        table.addColumn(activeColumn);

        Column vipAddressColumn = new Column("virtualServerAddress", "Virtual IP");
        vipAddressColumn.setSortable(true);
        table.addColumn(vipAddressColumn);

        Column poolMemberSizeColumn = new Column("virtualServerPoolSize", "Pool Size");
        poolMemberSizeColumn.setSortable(true);
        table.addColumn(poolMemberSizeColumn);

        Column vipStatusColumn = new Column("virtualServerStatus", "VIP Status");
        vipStatusColumn.setDecorator(new VirtualServerStatusDecorator());
        table.addColumn(vipStatusColumn);

        table.setDataProvider(new DataProvider<Application>() {
            @Override
            public Iterable<Application> getData() {
                return getApplicationRepository().getAllApplications();
            }
        });

        return table;
    }

    private class ApplicationsTable extends Table {

        public ApplicationsTable(String name) {
            super(name);
        }

        @Override
        protected void addRowAttributes(Map<String, String> attributes, Object row, int rowIndex) {
            if (!(row instanceof Application)) {
                return;
            }

            Application app = (Application) row;
            if (app.equals(getSelectedApplication())) {
                attributes.put("class", "selected");
            }
        }
    }

    private class VirtualServerStatusDecorator implements Decorator {
        @Override
        public String render(Object object, Context context) {
            if (!(object instanceof Application)) {
                return "";
            }

            Application app = (Application) object;
            AvailabilityStatus status = getVirtualServerStatus(app.getVirtualServerName());
            if (status == null) {
                status = AvailabilityStatus.NONE;
            }

            return createStatusDiv(status);
        }

        private String createStatusDiv(AvailabilityStatus status) {
            Validate.notNull(status, "Status is null");

            String cssClass;
            switch (status) {
                case NONE:
                    cssClass = "statusNone";
                    break;
                case AVAILABLE:
                    cssClass = "statusAvailable";
                    break;
                case UNAVAILABLE:
                    cssClass = "statusUnavailable";
                    break;
                case DISABLED:
                    cssClass = "statusDisabled";
                    break;
                case UNKNOWN:
                    cssClass = "statusUnknown";
                    break;
                default:
                    cssClass = "statusNone";
            }

            String text = getFormat().string(status);
            return "<div class='" + cssClass + "'>" + text + "</div>";
        }
    }
}
