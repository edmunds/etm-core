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

import com.edmunds.etm.common.api.UrlTokenType;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Command line option parsing utilities.
 *
 * @author Ryan Holmes
 */
@Component
public final class OptionUtils {

    private static final String TOKEN_TYPE_FIXED = "fixed";
    private static final String TOKEN_TYPE_REGEX = "regex";

    private OptionUtils() {
        // This class should not be instantiated.
    }

    public static String firstNonOptionArgument(OptionSet options) {
        List<String> arguments = options.nonOptionArguments();
        if(arguments.isEmpty()) {
            return null;
        }

        String tokenName = arguments.get(0);
        if(StringUtils.isBlank(tokenName)) {
            return null;
        }

        return tokenName;
    }

    public static UrlTokenType parseType(OptionSet options, OptionSpec<String> typeSpec, UrlTokenType defaultType) {

        UrlTokenType tokenType;
        // Token type
        String tokenTypeArg = options.valueOf(typeSpec);
        if(TOKEN_TYPE_FIXED.equals(tokenTypeArg)) {
            tokenType = UrlTokenType.FIXED;
        } else if(TOKEN_TYPE_REGEX.equals(tokenTypeArg)) {
            tokenType = UrlTokenType.REGEX;
        } else {
            tokenType = defaultType;
        }

        return tokenType;
    }

    @SuppressWarnings("unchecked")
    public static List<String> parseValues(OptionSet options, OptionSpec<File> fileSpec) throws IOException {

        List<String> values;
        File file = options.valueOf(fileSpec);
        if(file != null) {
            values = Lists.newArrayList(FileUtils.readLines(file));
        } else {
            values = Lists.newArrayList();
        }

        return values;
    }
}
