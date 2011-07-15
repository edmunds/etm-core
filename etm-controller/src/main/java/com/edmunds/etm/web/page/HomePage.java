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

import com.edmunds.common.configuration.api.EnvironmentConfiguration;
import com.edmunds.etm.system.api.ControllerInstance;
import com.edmunds.etm.system.impl.ControllerMonitor;
import org.apache.click.Context;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ETM Controller home page.
 *
 * @author Ryan Holmes
 */
@Component
public class HomePage extends BorderPage {

    private final ControllerMonitor controllerMonitor;

    @Autowired
    public HomePage(EnvironmentConfiguration environmentConfiguration,
                    ControllerMonitor controllerMonitor) {

        this.controllerMonitor = controllerMonitor;

        // Local controller
        addModel("controller", controllerMonitor.getLocalController());

        // Peer controllers table
        Table peerControllersTable = buildPeerControllersTable();
        addControl(peerControllersTable);

        // Environment configuration
        addModel("environment", environmentConfiguration);
    }

    @Override
    public String getTitle() {
        return "Home";
    }

    protected ControllerMonitor getEtmControllerMonitor() {
        return controllerMonitor;
    }

    private Table buildPeerControllersTable() {
        Table table = new Table("peerControllersTable");
        table.setClass(Table.CLASS_ITS);

        Column hostNameColumn = new Column("hostName", "Host Name");
        hostNameColumn.setSortable(true);
        table.addColumn(hostNameColumn);

        Column ipAddressColumn = new Column("ipAddress", "IP Address");
        ipAddressColumn.setSortable(true);
        table.addColumn(ipAddressColumn);

        Column failoverStateColumn = new Column("failoverState");
        failoverStateColumn.setSortable(true);
        failoverStateColumn.setDecorator(new FailoverStateDecorator());
        table.addColumn(failoverStateColumn);

        table.addColumn(new Column("version"));

        table.setDataProvider(new DataProvider<ControllerInstance>() {
            @Override
            public Iterable<ControllerInstance> getData() {
                return getEtmControllerMonitor().getPeerControllers();
            }
        });

        return table;
    }

    private class FailoverStateDecorator implements Decorator {
        @Override
        public String render(Object object, Context context) {
            if (!(object instanceof ControllerInstance)) {
                return "";
            }

            ControllerInstance controller = (ControllerInstance) object;
            return getFormat().string(controller.getFailoverState());
        }
    }
}
