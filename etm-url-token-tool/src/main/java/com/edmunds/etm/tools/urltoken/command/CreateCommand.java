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
import com.edmunds.etm.common.api.UrlTokenType;
import com.edmunds.etm.common.impl.TokenExistsException;
import com.edmunds.etm.common.impl.UrlTokenRepository;
import com.edmunds.etm.tools.urltoken.util.OptionUtils;
import com.edmunds.etm.tools.urltoken.util.OutputWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Creates a new URL token.
 *
 * @author Ryan Holmes
 */
@Component
public class CreateCommand implements Command {

    private final UrlTokenRepository urlTokenRepository;
    private final OutputWriter outputWriter;
    private final OptionParser parser;
    private final OptionSpec<String> tokenTypeOption;
    private final OptionSpec<File> valuesFileOption;

    @Autowired
    public CreateCommand(UrlTokenRepository urlTokenRepository, OutputWriter outputWriter) {
        this.urlTokenRepository = urlTokenRepository;
        this.outputWriter = outputWriter;
        this.parser = new OptionParser();
        this.tokenTypeOption = parser.accepts("t", "token type, 'fixed' (default) or 'regex'")
            .withRequiredArg().ofType(String.class);
        this.valuesFileOption = parser.accepts("f", "values file path (one value per line)")
            .withRequiredArg().ofType(File.class);
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a new URL token.";
    }

    @Override
    public String getUsage() {
        return "create TOKENNAME";
    }

    @Override
    public OptionParser getOptionParser() {
        return parser;
    }

    @Override
    public void execute(String[] args) {

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch(OptionException e) {
            outputWriter.println(e.getMessage());
            return;
        }

        // Token type
        UrlTokenType tokenType;
        tokenType = OptionUtils.parseType(options, tokenTypeOption, UrlTokenType.FIXED);

        // Token name
        String tokenName = OptionUtils.firstNonOptionArgument(options);
        if(tokenName == null) {
            outputWriter.printHelp(this);
            return;
        }

        // Token values
        List<String> values;
        try {
            values = OptionUtils.parseValues(options, valuesFileOption);
        } catch(IOException e) {
            System.out.println(e.getMessage());
            return;
        }

        UrlToken token = UrlToken.newUrlToken(tokenType, tokenName, values);
        try {
            urlTokenRepository.createToken(token);
        } catch(TokenExistsException e) {
            outputWriter.println(String.format("Token already exists: %s", tokenName));
        }
    }
}
