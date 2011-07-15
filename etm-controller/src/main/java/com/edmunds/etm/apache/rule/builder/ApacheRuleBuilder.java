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
package com.edmunds.etm.apache.rule.builder;

import com.edmunds.etm.common.util.RegexUtil;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.edmunds.etm.rules.util.Constants.*;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Rule builder for Apache web servers. <p/> <p/> Copyright (C) 2010 Edmunds.com <p/> <p/> Date: Mar 18, 2010
 *
 * @author Aliaksandr Savin
 * @author Ryan Holmes
 */
@Component
public class ApacheRuleBuilder {

    /**
     * Regex OR.
     */
    static final String OR = "|";
    /**
     * Left bracket.
     */
    static final String LEFT_BRACKET = "(";
    /**
     * Right bracket.
     */
    static final String RIGHT_BRACKET = ")";

    static final String URL_PATTERN = "^" + SLASH + "{0}$";

    /**
     * URL token value resolver.
     */
    private UrlTokenResolver urlTokenResolver;

    /**
     * Transformation map.
     */
    private final Map<String, String> transformationMap = new HashMap<String, String>();

    public ApacheRuleBuilder() {

        // initialize symbol transformation map
        transformationMap.put(DOUBLE_ASTERISK, ANY_SYMBOLS_REGEXP);
        transformationMap.put(ASTERISK, ANY_SYMBOLS_EXCEPT_SLASH_REGEXP);
    }

    @Autowired
    public void setUrlTokenResolver(UrlTokenResolver urlTokenResolver) {
        this.urlTokenResolver = urlTokenResolver;
    }

    public String build(String rule) {
        if(rule == null || rule.isEmpty()) {
            return rule;
        }
        String replacedString = replaceSpecialSymbols(rule);
        return buildRuleString(replacedString);
    }

    /**
     * Adds to regexp start, end of string and slash.
     *
     * @param replacedString string with replaced symbols.
     * @return rule in string in mod_rewrite format.
     */
    private String buildRuleString(String replacedString) {
        return MessageFormat.format(URL_PATTERN, replacedString);
    }

    /**
     * Replaces wildcards with regexps.
     *
     * @param rule source rule with wildcards.
     * @return regular expression.
     */
    private String replaceSpecialSymbols(String rule) {
        String[] tokens = removeEmptyTokens(rule.split(SLASH));
        List<String> changedTokens = new LinkedList<String>();
        for(String token : tokens) {
            if(transformationMap.containsKey(token)) {
                token = transformationMap.get(token);
            } else if(urlTokenResolver.isTokenDefined(token)) {
                token = urlTokenResolver.resolveToken(token);
            } else {
                token = RegexUtil.escapeRegex(token);
                token = token.replace(ASTERISK, transformationMap.get(ASTERISK));
            }
            changedTokens.add(token);
        }
        if(rule.endsWith(SLASH)) {
            changedTokens.add(EMPTY);
        }
        return StringUtils.join(changedTokens, SLASH);
    }

    /**
     * Remove elements strings from array.
     *
     * @param strings array of strings.
     * @return array without empty elements.
     */
    private String[] removeEmptyTokens(String[] strings) {
        List<String> list = new ArrayList<String>(Arrays.asList(strings));
        list.removeAll(Arrays.asList(""));
        return list.toArray(new String[list.size()]);
    }
}
