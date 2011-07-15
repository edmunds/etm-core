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
package com.edmunds.etm.rules.impl;

import com.edmunds.etm.common.api.UrlToken;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

/**
 * A dictionary of {@link UrlToken} objects used to resolve (or "expand") token symbols in ETM URL rules. The tokens are
 * contained in a Map where the key is a token symbol (the token name surrounded by square brackets, [TOKEN_NAME]), and
 * the value is the token object.
 *
 * @author Julian Cardona
 * @author Ryan Holmes
 */
@Component
public class UrlTokenDictionary implements UrlTokenResolver {

    /**
     * The map that holds tokens and their symbols.
     */
    private Map<String, UrlToken> tokenDefinitions = new HashMap<String, UrlToken>();

    /**
     * Adds a UrlToken to the dictionary.
     *
     * @param token the UrlToken to add
     */
    public void add(UrlToken token) {
        Validate.notNull(token, "URL token is null");
        tokenDefinitions.put('[' + token.getName() + ']', token);
    }

    /**
     * Adds a collection of UrlTokens to the dictionary.
     *
     * @param tokens the tokens to add
     */
    public void addAll(Collection<UrlToken> tokens) {
        Validate.notNull(tokens, "URL token collection is null");
        for(UrlToken token : tokens) {
            add(token);
        }
    }

    /**
     * Returns a collection of all UrlToken objects from the dictionary.
     *
     * @return collection of UrlToken objects
     */
    public Collection<UrlToken> getAll() {
        return Collections.unmodifiableCollection(tokenDefinitions.values());
    }

    /**
     * Clears all tokens from the dictionary.
     */
    public void clear() {
        tokenDefinitions.clear();
    }

    /**
     * Return the regular expression to substitute for the given token symbol. Returns null if the token is not
     * defined.
     *
     * @param tokenSymbol the token symbol to resolve
     * @return the regular expression for the specified token symbol, or null if the token is not defined
     */
    public String resolveToken(String tokenSymbol) {
        UrlToken token = tokenDefinitions.get(tokenSymbol);
        if(token == null) {
            return null;
        }
        return token.toRegex();
    }

    /**
     * Indicates whether the specified token symbol is defined.
     *
     * @param tokenSymbol a token symbol (e.g. [MYTOKEN])
     * @return true if the token is defined, false otherwise
     */
    @Override
    public boolean isTokenDefined(String tokenSymbol) {
        return tokenDefinitions.containsKey(tokenSymbol);
    }
}
