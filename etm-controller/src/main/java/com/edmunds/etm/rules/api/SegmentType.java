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
 * Types of segments that form a url rule.
 */
public enum SegmentType {
    /**
     * The segment is empty, this only occurs at the end of the url (the index.html page).
     */
    EMPTY,

    /**
     * The segment consists of a single star.
     */
    STAR,

    /**
     * The segment consists of a double star.
     */
    DOUBLE_STAR,

    /**
     * The segment is complete (does not contain any wild cards or patterns).
     */
    COMPLETE,

    /**
     * The segment contains a * hence multiple matches are possible.
     */
    WILDCARD,

    /**
     * The segment is a special token such as: [make] [model], ...
     */
    TOKEN;
}
