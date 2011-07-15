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
package com.edmunds.etm.rules.api;

import java.util.Collection;

/**
 * Interface provides method for generation of web server specific rewrite configuration.
 * <p/>
 * <p/> Copyright (C) 2010 Edmunds.com
 * <p/>
 * <p/> Date: Mar 19, 2010
 *
 * @author Aliaksandr Savin
 */
public interface WebServerConfigurationBuilder {

    /**
     * Builds web server configuration according to the specified set of rules.
     *
     * @param rules set of rules.
     */
    void build(Collection<UrlRule> rules);
}
