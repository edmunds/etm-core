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

import com.edmunds.etm.common.impl.UrlTokenRepository;
import com.edmunds.etm.tools.urltoken.util.OutputWriter;
import java.util.List;
import joptsimple.OptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Lists all defined URL tokens.
 *
 * @author Ryan Holmes
 */
@Component
public class ListCommand implements Command {

    private final UrlTokenRepository urlTokenRepository;
    private final OutputWriter outputWriter;

    @Autowired
    public ListCommand(UrlTokenRepository urlTokenRepository, OutputWriter outputWriter) {
        this.urlTokenRepository = urlTokenRepository;
        this.outputWriter = outputWriter;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List all defined URL tokens.";
    }

    @Override
    public String getUsage() {
        return "list";
    }

    @Override
    public OptionParser getOptionParser() {
        return null;
    }

    @Override
    public void execute(String[] args) {

        List<String> tokenNames = urlTokenRepository.getTokenNames();

        for(String name : tokenNames) {
            outputWriter.println(name);
        }
    }
}
