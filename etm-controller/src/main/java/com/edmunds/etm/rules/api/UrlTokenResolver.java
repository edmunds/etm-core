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

/**
 * Resolves tokens to their equivalent regular expression. <p/>
 *
 * @author David Trott
 * @author Ryan Holmes
 * @author Julian Cardona
 */
public interface UrlTokenResolver {
    /**
     * Returns the regular expression corresponding to this token.
     *
     * @param token a delimited token string (e.g. [MYTOKEN]).
     * @return the regular expression that corresponds to this token.
     */
    String resolveToken(String token);

    /**
     * Indicates whether the specified token string is a URL token.
     *
     * @param token a delimited token string (e.g. [MYTOKEN])
     * @return true if the token is defined, false otherwise
     */
    boolean isTokenDefined(String token);
}
