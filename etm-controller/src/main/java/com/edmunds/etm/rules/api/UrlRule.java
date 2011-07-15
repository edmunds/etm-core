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

import com.edmunds.etm.management.api.MavenModule;
import com.google.common.collect.Lists;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.List;

import static com.edmunds.etm.rules.api.RuleComparison.DISTINCT;
import static com.edmunds.etm.rules.api.RuleComparison.IDENTICAL;
import static com.edmunds.etm.rules.api.RuleComparison.OVERLAP;
import static com.edmunds.etm.rules.api.SegmentType.DOUBLE_STAR;

/**
 * Etm configuration rule domain object.
 * <p/>
 * Copyright (C) 2010 Edmunds.com
 *
 * @author David Trott
 */
public class UrlRule {

    /**
     * The maven module of the vip that this rule corresponds to.
     */
    private final MavenModule mavenModule;

    /**
     * The address of the vip "1.2.3.4:7000".
     */
    private final String vipAddress;

    /**
     * Rule.
     */
    private final String rule;

    private final List<UrlRuleSegment> segments = Lists.newArrayList();

    /**
     * Calculate the hash code in advance.
     */
    private final int calculatedHashCode;

    /**
     * Constructor with rule parameter.
     *
     * @param tokenResolver the resolver of tokens into expressions.
     * @param mavenModule the mavenModule
     * @param vipAddress  the address of this vip.
     * @param rule        rule.
     */
    public UrlRule(UrlTokenResolver tokenResolver, MavenModule mavenModule, String vipAddress, String rule) {
        Validate.notNull(tokenResolver, "tokenResolver is null");
        Validate.notNull(mavenModule, "mavenModule is null");
        Validate.notNull(vipAddress, "vipAddress is null");
        Validate.notNull(rule, "rule is null");
        Validate.isTrue(rule.startsWith("/"), "rule must start with a /");

        this.mavenModule = mavenModule;
        this.vipAddress = vipAddress;
        this.rule = rule;
        this.calculatedHashCode = new HashCodeBuilder()
                .append(mavenModule)
                .append(rule)
                .toHashCode();

        final String[] split = rule.split("/", -1);
        final int lastIdx = split.length - 1;

        // Ignore the 'empty segment' before the first /
        for (int i = 1; i < split.length; i++) {
            // And apply special handling for the file name.
            segments.add(new UrlRuleSegment(tokenResolver, split[i], i == lastIdx));
        }
    }

    /**
     * Compares this rule to another rule to calculate the relative priority.
     *
     * @param other         the other rule to compare to.
     * @return the relative priority of the rules.
     */
    public RuleComparison compareTo(UrlRule other) {
        if (rule.equals(other.rule)) {
            return IDENTICAL;
        }

        if (isDistinct(other)) {
            return DISTINCT;
        }

        // Check the filename first
        final RuleComparison lastComparison = getLastSegment().compareTo(other.getLastSegment());
        if (lastComparison != IDENTICAL) {
            return lastComparison;
        }

        // Then check the path
        return comparePath(other);
    }

    private boolean isDistinct(UrlRule other) {
        final int minLength = Math.min(segments.size(), other.segments.size());

        for (int i = 0; i < minLength; i++) {
            final UrlRuleSegment segment = segments.get(i);
            final UrlRuleSegment otherSegment = other.segments.get(i);

            if (segment.compareTo(otherSegment) == DISTINCT) {
                return true;
            }

            // We only need to scan in reverse if we encounter a ** token.
            if (segment.getSegmentType() == DOUBLE_STAR || otherSegment.getSegmentType() == DOUBLE_STAR) {
                // There is nothing else we can conclude now so return the result of the reverse scan
                return isReverseDistinct(other);
            }
        }

        return segments.size() != other.segments.size();
    }

    private boolean isReverseDistinct(UrlRule other) {
        final int minLength = Math.min(segments.size(), other.segments.size());
        int index = segments.size();
        int otherIndex = other.segments.size();

        for (int i = 0; i < minLength; i++) {
            final UrlRuleSegment segment = segments.get(--index);
            final UrlRuleSegment otherSegment = other.segments.get(--otherIndex);

            if (segment.compareTo(otherSegment) == DISTINCT) {
                return true;
            }

            if (segment.getSegmentType() == DOUBLE_STAR || otherSegment.getSegmentType() == DOUBLE_STAR) {
                // We have scanned both ways and hit double stars so the string is not distinct.
                return false;
            }
        }

        // We have a weird special case (which is distinct):
        // This:    /a/**/c/d/a/b/
        // Other:   /a/b/
        return true;
    }

    private RuleComparison comparePath(UrlRule other) {
        final int minLength = Math.min(segments.size(), other.segments.size());
        for (int i = 0; i < minLength; i++) {
            final UrlRuleSegment segment = segments.get(i);
            final UrlRuleSegment otherSegment = other.segments.get(i);

            // Keep scanning as long as the strings are identical.
            final RuleComparison comparison = segment.compareTo(otherSegment);
            if (comparison != IDENTICAL) {
                return comparison;
            }

            if (segment.getSegmentType() == DOUBLE_STAR || otherSegment.getSegmentType() == DOUBLE_STAR) {
                // We have a really complex pattern so just call it an overlap and abort.
                return OVERLAP;
            }
        }

        // This line is unreachable, the only way to get here is two identical rules.
        // Which is a guarded condition at the top of the compareTo method.
        return OVERLAP;
    }

    /**
     * Returns the maven module.
     *
     * @return the maven module of the vip.
     */
    public MavenModule getMavenModule() {
        return mavenModule;
    }

    /**
     * Returns the vip address.
     *
     * @return the address of the vip servicing this rule.
     */
    public String getVipAddress() {
        return vipAddress;
    }

    /**
     * Returns rule.
     *
     * @return rule.
     */
    public String getRule() {
        return rule;
    }

    /**
     * Returns the rule decomposed into segments.
     *
     * @return the rule segments.
     */
    public List<UrlRuleSegment> getSegments() {
        return segments;
    }

    /**
     * Returns the last segment in the path.
     * <p/>
     * The shortest possible path is '/' so there is always at least one segment. In this case the segment would be
     * empty representing the empty file name after the /.
     *
     * @return the last segment.
     */
    public UrlRuleSegment getLastSegment() {
        return segments.get(segments.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UrlRule other = (UrlRule) o;

        return new EqualsBuilder()
                .append(mavenModule, other.mavenModule)
                .append(rule, other.rule)
                .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return calculatedHashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return rule;
    }
}
