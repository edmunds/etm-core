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

import com.edmunds.etm.rules.api.BlockedUrlRule;
import com.edmunds.etm.rules.api.InvalidUrlRule;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.impl.WebConfigurationManager;
import org.apache.click.Context;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Displays active, blocked and invalid URL rules.
 *
 * @author Ryan Holmes
 */
@Component
public class UrlRulesPage extends BorderPage {

    private final WebConfigurationManager webConfigurationManager;

    @Autowired
    public UrlRulesPage(WebConfigurationManager webConfigurationManager) {
        this.webConfigurationManager = webConfigurationManager;

        addControl(buildActiveRulesTable());
        addControl(buildBlockedRulesTable());
        addControl(buildInvalidRulesTable());
    }

    @Override
    public String getTitle() {
        return "URL Rules";
    }

    protected WebConfigurationManager getWebConfigurationManager() {
        return webConfigurationManager;
    }

    private Table buildActiveRulesTable() {
        Table table = new Table("activeRulesTable");
        table.setClass(Table.CLASS_ITS);

        Column ruleColumn = new Column("rule");
        ruleColumn.setSortable(true);
        table.addColumn(ruleColumn);

        Column mavenModuleColumn = new Column("mavenModule", "Application");
        mavenModuleColumn.setSortable(true);
        table.addColumn(mavenModuleColumn);

        Column vipColumn = new Column("vipAddress");
        vipColumn.setSortable(true);
        table.addColumn(vipColumn);

        table.setDataProvider(new DataProvider<UrlRule>() {
            @Override
            public Iterable<UrlRule> getData() {
                return getWebConfigurationManager().getActiveRules();
            }
        });

        return table;
    }


    private Table buildBlockedRulesTable() {
        Table table = new Table("blockedRulesTable");
        table.setClass(Table.CLASS_ITS);

        Column ruleColumn = new Column("rule");
        ruleColumn.setSortable(true);
        table.addColumn(ruleColumn);

        Column mavenModuleColumn = new Column("mavenModule", "Application");
        mavenModuleColumn.setSortable(true);
        table.addColumn(mavenModuleColumn);

        Column vipColumn = new Column("vipAddress");
        vipColumn.setSortable(true);
        table.addColumn(vipColumn);

        Column blockingRulesColumn = new Column("blockingRules", "Blocking Rules");
        blockingRulesColumn.setDecorator(new Decorator() {
            @Override
            public String render(Object object, Context context) {
                BlockedUrlRule rule = (BlockedUrlRule) object;
                return StringUtils.join(rule.getBlockingRules(), "<br />");
            }
        });
        table.addColumn(blockingRulesColumn);

        table.setDataProvider(new DataProvider<BlockedUrlRule>() {
            @Override
            public Iterable<BlockedUrlRule> getData() {
                return getWebConfigurationManager().getBlockedRules();
            }
        });

        return table;
    }

    private Table buildInvalidRulesTable() {
        Table table = new Table("invalidRulesTable");
        table.setClass(Table.CLASS_ITS);

        Column ruleColumn = new Column("rule");
        ruleColumn.setSortable(true);
        table.addColumn(ruleColumn);

        Column mavenModuleColumn = new Column("mavenModule", "Application");
        mavenModuleColumn.setSortable(true);
        table.addColumn(mavenModuleColumn);

        table.setDataProvider(new DataProvider<InvalidUrlRule>() {
            @Override
            public Iterable<InvalidUrlRule> getData() {
                return getWebConfigurationManager().getInvalidRules();
            }
        });

        return table;

    }
}
