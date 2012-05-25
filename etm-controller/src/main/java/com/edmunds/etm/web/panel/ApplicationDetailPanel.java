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

import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.management.api.HttpMonitor;
import com.edmunds.etm.runtime.api.Application;
import org.apache.click.control.Panel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays application details.
 *
 * @author Ryan Holmes
 */
public class ApplicationDetailPanel extends Panel {

    public ApplicationDetailPanel(String name) {
        super(name);
        setTemplate("/panel/application-detail-panel.htm");
    }

    public void setApplication(Application application) {
        if (application == null) {
            return;
        }
        addModel("name", application.getName());
        addModel("version", application.getVersion().toString());
        addModel("active", application.isActive());
        addModel("rules", application.getRules());

        if (application.hasVirtualServer()) {

            // add virtual server address
            addModel("virtualServerAddress", application.getVirtualServerAddress());

            // add pool members
            List<PoolMember> poolMembers = new ArrayList<PoolMember>(application.getVirtualServerPoolMembers());
            Collections.sort(poolMembers, new Comparator<PoolMember>() {
                public int compare(PoolMember m1, PoolMember m2) {
                    return m1.toString().compareTo(m2.toString());
                }
            });
            addModel("virtualServerPoolMembers", poolMembers);
        }

        // add http monitor
        HttpMonitor httpMonitor = application.getHttpMonitor();
        if (httpMonitor != null) {
            addModel("httpMonitor", httpMonitor);
        }
    }
}
