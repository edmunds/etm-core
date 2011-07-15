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

import com.edmunds.etm.tools.urltoken.application.CommandLocator;
import com.edmunds.etm.tools.urltoken.util.OutputWriter;
import joptsimple.OptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Displays help for a given command.
 *
 * @author Ryan Holmes
 */
@Component
public class HelpCommand implements Command {

    private final CommandLocator commandLocator;
    private final OutputWriter outputWriter;

    @Autowired
    public HelpCommand(CommandLocator commandLocator, OutputWriter outputWriter) {
        this.commandLocator = commandLocator;
        this.outputWriter = outputWriter;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Display help for a specific command.";
    }

    @Override
    public String getUsage() {
        return "help <command>";
    }

    @Override
    public OptionParser getOptionParser() {
        return null;
    }

    @Override
    public void execute(String[] args) {
        if (args == null || args.length != 1) {
            outputWriter.printHelp(this);
            outputWriter.printAvailableCommands();
            return;
        }
        String commandName = args[0];
        Command cmd = commandLocator.get(commandName);
        if (cmd == null) {
            outputWriter.printHelp(this);
            return;
        }

        outputWriter.printHelp(cmd);
    }
}
