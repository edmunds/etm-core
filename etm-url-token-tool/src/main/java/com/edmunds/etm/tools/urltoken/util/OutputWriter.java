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
package com.edmunds.etm.tools.urltoken.util;

import com.edmunds.etm.tools.urltoken.application.CommandLocator;
import com.edmunds.etm.tools.urltoken.command.Command;
import joptsimple.OptionParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * Writes printed output to the appropriate stream.
 *
 * @author Ryan Holmes
 */
@Component
public class OutputWriter {

    private final PrintWriter writer;
    private final CommandLocator commandLocator;

    @Autowired
    public OutputWriter(CommandLocator commandLocator) {
        // Using the default charset is correct in this case, since we are writing to standard out.
        this.writer = new PrintWriter(new OutputStreamWriter(System.out, Charset.defaultCharset()));
        this.commandLocator = commandLocator;
    }

    public void println(String str) {
        writer.println(str);
        writer.flush();
    }

    public void printHelp(Command command) {
        writer.println(command.getName() + ": " + command.getDescription());
        writer.println("usage: " + command.getUsage());
        OptionParser parser = command.getOptionParser();
        if (parser != null) {
            writer.println();
            try {
                parser.printHelpOn(writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writer.flush();
    }

    public void printAvailableCommands() {
        writer.println();
        writer.println("Available commands:");

        for (String name : commandLocator.getNames()) {
            writer.println("\t" + name);
        }
        writer.flush();
    }
}
