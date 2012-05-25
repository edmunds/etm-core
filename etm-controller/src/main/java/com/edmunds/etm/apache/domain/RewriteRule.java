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
package com.edmunds.etm.apache.domain;

import org.apache.commons.lang.StringUtils;

/**
 * The basic class for holding forward rules in application server specific format. The class assumes that the rule can
 * be represented in a form of at least of 3 pieces: pattern - what URLs should be handled by the rule, substitution -
 * how the matched URL will be changed and list of appserver sepcific options.
 * <p/>
 * The class is intended for subclassing and at least the output format of the rule should be declared in a subclass.
 * Subclasses may introduce new fields for some specific appserver needs.
 * <p/>
 * <p/> Copyright (C) 2010 Edmunds.com
 * <p/>
 * <p/> Date: Mar 19, 2010
 *
 * @author Aliaksandr Savin
 */
public abstract class RewriteRule {
    /**
     * Pattern.
     */
    private String pattern;

    /**
     * Replacement.
     */
    private String substitution;

    /**
     * Options.
     */
    private String[] options;

    /**
     * Constructor allows to create representation of rewrite rule.
     * <p/>
     * E.g. {@code ^/tmv/toyota/([^/]*)$ http://127.0.0.1:8080$0 [P]}
     * <p/>
     * where: <ul> <li>{@code ^/tmv/toyota/([^/]*)$} is a pattern <li> {@code http://127.0.0.1:8080$0} is a substitution
     * <li> {@code P} is an option </ul>
     *
     * @param pattern      pattern.
     * @param substitution substitution.
     * @param options      options.
     */
    public RewriteRule(String pattern, String substitution, String... options) {
        if (StringUtils.isEmpty(pattern)) {
            throw new IllegalArgumentException("The pattern can't be empty or null.");
        }

        if (StringUtils.isEmpty(substitution)) {
            throw new IllegalArgumentException("The Substitution can't be empty or null.");
        }

        this.pattern = pattern;
        this.substitution = substitution;
        this.options = options;
    }

    /**
     * Returns rewrite rule pattern.
     *
     * @return pattern.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns rewrite rule substitution.
     *
     * @return replacement.
     */
    public String getSubstitution() {
        return substitution;
    }

    /**
     * Returns options array.
     *
     * @return options.
     */
    public String[] getOptions() {
        return options;
    }

    /**
     * The method should be implemented in a subclass and represent th rule in application server specific format.
     *
     * @return string representation of  the rule in application server specific format.
     */
    public abstract String build();

    /**
     * Equals method. Compares this Object to another.
     *
     * @param o the Object to compare to
     * @return true if the Objects are considered equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RewriteRule that = (RewriteRule) o;

        if (pattern != null ? !pattern.equals(that.pattern) : that.pattern != null) {
            return false;
        }
        if (substitution != null ? !substitution.equals(that.substitution) : that.substitution != null) {
            return false;
        }

        return true;
    }

    /**
     * HashCode method. Generates a unique hashCode for this Object.
     *
     * @return a unique hashCode for this Object
     */
    @Override
    public int hashCode() {
        int result = pattern != null ? pattern.hashCode() : 0;
        result = 31 * result + (substitution != null ? substitution.hashCode() : 0);
        return result;
    }

    /**
     * Creates a String version of this Object.
     *
     * @return a String version of this Object.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RewriteRule");
        sb.append("{pattern='").append(pattern).append('\'');
        sb.append(", substitution='").append(substitution).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
