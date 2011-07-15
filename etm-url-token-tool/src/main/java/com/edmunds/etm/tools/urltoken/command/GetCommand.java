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
package com.edmunds.etm.tools.urltoken.command;

import com.edmunds.etm.common.api.UrlToken;
import com.edmunds.etm.common.impl.UrlTokenRepository;
import com.edmunds.etm.tools.urltoken.util.OutputWriter;
import joptsimple.OptionParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gets the details of a URL token.
 *
 * @author Ryan Holmes
 */
@Component
public class GetCommand implements Command {

    private final UrlTokenRepository urlTokenRepository;
    private final OutputWriter outputWriter;

    @Autowired
    public GetCommand(UrlTokenRepository urlTokenRepository, OutputWriter outputWriter) {
        this.urlTokenRepository = urlTokenRepository;
        this.outputWriter = outputWriter;
    }

    @Override
    public String getName() {
        return "get";
    }

    @Override
    public String getDescription() {
        return "Get the details of a URL token.";
    }

    @Override
    public String getUsage() {
        return "get TOKENNAME";
    }

    @Override
    public OptionParser getOptionParser() {
        return null;
    }

    @Override
    public void execute(String[] args) {

        if(args == null || args.length != 1) {
            outputWriter.printHelp(this);
            return;
        }

        // Token name
        String tokenName = args[0].trim();
        if(StringUtils.isBlank(tokenName)) {
            outputWriter.printHelp(this);
            return;
        }

        UrlToken token = urlTokenRepository.getToken(tokenName);
        if(token == null) {
            outputWriter.println(String.format("Token not found: %s", tokenName));
            return;
        }

        outputWriter.println("Name: " + token.getName());
        outputWriter.println("Type: " + token.getType());
        outputWriter.println("Values:");

        for(String value : token.getValues()) {
            outputWriter.println(value);
        }
    }
}
