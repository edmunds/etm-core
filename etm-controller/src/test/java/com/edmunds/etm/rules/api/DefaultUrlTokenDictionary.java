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

import com.edmunds.etm.common.api.FixedUrlToken;
import com.edmunds.etm.common.api.RegexUrlToken;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;

public class DefaultUrlTokenDictionary {

    public static UrlTokenDictionary newInstance() {

        UrlTokenDictionary dictionary = new UrlTokenDictionary();
        dictionary.add(new FixedUrlToken("make",
            "auburn",
            "duesenberg",
            "ford",
            "kissel",
            "locomobile",
            "packard",
            "studebaker",
            "stutz"));
        dictionary.add(new RegexUrlToken("model", "[^/]*"));
        dictionary.add(new RegexUrlToken("year", "(19|20)\\d{2}"));
        dictionary.add(new RegexUrlToken("state", "(ca|ny|other)"));
        dictionary.add(new RegexUrlToken("zipcode", "\\d{5}"));
        return dictionary;
    }


}
