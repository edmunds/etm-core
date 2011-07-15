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
package com.edmunds.etm.tools.urltoken.application;

import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.tools.urltoken.command.Command;
import com.edmunds.etm.tools.urltoken.util.OutputWriter;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.connection.ZooKeeperNodeInitializer;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * The UrlTokenTool is a command-line tool for managing the URL tokens used in ETM.
 *
 * @author Ryan Holmes
 */
@Component
public class UrlTokenTool implements ZooKeeperConnectionListener {

    private static final Logger logger = Logger.getLogger(UrlTokenTool.class);

    private final ZooKeeperConnection connection;
    private final CommandLocator commandLocator;
    private final OutputWriter outputWriter;
    private final Integer lock = -1;

    private Command command;
    private String[] arguments;

    public static void main(String[] args) {
        // Create the Spring application context
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("etm-url-token-tool-context.xml");

        // Run the Apache agent
        UrlTokenTool tokenTool;
        tokenTool = (UrlTokenTool) ctx.getBean("urlTokenTool", UrlTokenTool.class);
        tokenTool.run(args);
    }

    @Autowired
    public UrlTokenTool(ZooKeeperConnection connection,
                        CommandLocator commandLocator,
                        OutputWriter outputWriter,
                        ControllerPaths controllerPaths) {
        this.connection = connection;
        this.commandLocator = commandLocator;
        this.outputWriter = outputWriter;
        connection.addListener(this);
        connection.addInitializer(new ZooKeeperNodeInitializer(controllerPaths.getUrlTokens()));
    }

    public void run(String[] args) {

        if (args.length < 1) {
            printUsage();
            return;
        }

        String commandName = args[0];
        Command cmd = commandLocator.get(commandName);
        if (cmd == null) {
            printUsage();
            return;
        }
        command = cmd;
        if (args.length > 1) {
            arguments = Arrays.copyOfRange(args, 1, args.length);
        } else {
            arguments = new String[]{};
        }

        connection.connect();

        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            logger.warn("Main thread interrupted, exiting", e);
        } finally {
            connection.close();
        }
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {
            processCommand();
        }
    }

    private void processCommand() {
        command.execute(arguments);

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void printUsage() {
        outputWriter.println("Usage: UrlTokenTool <command> [options]");
        outputWriter.printAvailableCommands();
    }
}
