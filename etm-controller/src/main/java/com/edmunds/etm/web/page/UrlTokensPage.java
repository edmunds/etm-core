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

import com.edmunds.etm.common.api.UrlToken;
import com.edmunds.etm.common.impl.UrlTokenRepository;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;
import com.edmunds.etm.rules.impl.UrlTokenMonitor;
import org.apache.click.Context;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays active and default URL tokens.
 *
 * @author Ryan Holmes
 */
@Component
public class UrlTokensPage extends BorderPage {

    private final UrlTokenDictionary tokenDictionary;
    private final UrlTokenRepository tokenRepository;

    @Autowired
    public UrlTokensPage(UrlTokenDictionary tokenDictionary, UrlTokenRepository tokenRepository) {
        this.tokenDictionary = tokenDictionary;
        this.tokenRepository = tokenRepository;

        addControl(buildActiveTokensTable());
        addControl(buildDefaultTokensTable());
    }

    @Override
    public String getTitle() {
        return "URL Tokens";
    }

    protected UrlTokenDictionary getTokenDictionary() {
        return tokenDictionary;
    }

    protected List<UrlToken> readDefaultTokens() {
        List<UrlToken> tokens;
        String contextPath = getContext().getServletContext().getRealPath("/");
        File file = new File(contextPath + UrlTokenMonitor.DEFAULT_TOKENS_XML_PATH);

        try {
            tokens = tokenRepository.readTokensFromFile(file);
        } catch (IOException e) {
            getContext().setFlashAttribute("error", e.getMessage());
            tokens = new ArrayList<UrlToken>();
        }

        return tokens;
    }

    private Table buildActiveTokensTable() {
        Table table = buildTokensTable("activeTokensTable");

        table.setDataProvider(new DataProvider<UrlToken>() {
            @Override
            public Iterable<UrlToken> getData() {
                return getTokenDictionary().getAll();
            }
        });

        return table;
    }

    private Table buildDefaultTokensTable() {
        Table table = buildTokensTable("defaultTokensTable");

        table.setDataProvider(new DataProvider<UrlToken>() {
            @Override
            public Iterable<UrlToken> getData() {
                return readDefaultTokens();
            }
        });

        return table;
    }

    private Table buildTokensTable(String name) {
        Table table = new Table(name);
        table.setClass(Table.CLASS_ITS);

        Column nameColumn = new Column("name");
        nameColumn.setSortable(true);
        table.addColumn(nameColumn);

        Column typeColumn = new Column("type");
        typeColumn.setSortable(true);
        table.addColumn(typeColumn);

        Column valuesColumn = new Column("values");
        valuesColumn.setDecorator(new Decorator() {
            @Override
            public String render(Object object, Context context) {
                UrlToken token = (UrlToken) object;
                return StringUtils.join(token.getValues(), ", ");
            }
        });
        table.addColumn(valuesColumn);

        return table;
    }
}
