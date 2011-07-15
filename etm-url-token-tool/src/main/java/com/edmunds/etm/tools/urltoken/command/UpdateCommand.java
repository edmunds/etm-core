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
import com.edmunds.etm.common.impl.TokenNotFoundException;
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
 * Updates an existing URL token.
 *
 * @author Ryan Holmes
 */
@Component
public class UpdateCommand implements Command {

    private final UrlTokenRepository urlTokenRepository;
    private final OutputWriter outputWriter;
    private final OptionParser parser;
    private final OptionSpec<File> valuesFileOption;
    private final OptionSpec replaceOption;

    @Autowired
    public UpdateCommand(UrlTokenRepository urlTokenRepository, OutputWriter outputWriter) {
        this.urlTokenRepository = urlTokenRepository;
        this.outputWriter = outputWriter;
        this.parser = new OptionParser();
        this.valuesFileOption = parser.accepts("f", "values file path (one value per line)")
            .withRequiredArg().ofType(File.class);
        this.replaceOption = parser.accepts("r", "replace values instead of appending");
    }

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public String getDescription() {
        return "Update the values of a URL token.\nNew values are appended by default.";
    }

    @Override
    public String getUsage() {
        return "update TOKENNAME";
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
            outputWriter.println(e.getMessage());
            return;
        }

        UrlToken token = urlTokenRepository.getToken(tokenName);
        if(token == null) {
            outputWriter.println(String.format("Token does not exist: %s", tokenName));
            return;
        }

        if(options.has(replaceOption)) {
            token.setValues(values);
        } else {
            for(String value : values) {
                if(!token.getValues().contains(value)) {
                    token.addValue(value);
                }
            }
        }

        try {
            urlTokenRepository.updateToken(token);
        } catch(TokenNotFoundException e) {
            outputWriter.println(String.format("Token not found: %s", tokenName));
        }
    }
}
