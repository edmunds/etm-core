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

import com.edmunds.etm.tools.urltoken.command.Command;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryan Holmes
 */
@Component
public class CommandLocator {

    private Map<String, Command> commandsByName = Maps.newHashMap();

    @Autowired
    public void addAll(Command... commands) {
        for (Command cmd : commands) {
            add(cmd);
        }
    }

    public Set getAll() {
        return new HashSet<Command>(commandsByName.values());
    }

    public void add(Command command) {
        commandsByName.put(command.getName(), command);
    }

    public Command get(String name) {
        return commandsByName.get(name);
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<String>(commandsByName.keySet());
        Collections.sort(names);
        return names;
    }
}
