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

import com.edmunds.etm.common.util.RegexUtil;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;

import static com.edmunds.etm.rules.api.RuleComparison.DISTINCT;
import static com.edmunds.etm.rules.api.RuleComparison.HIGH_PRIORITY;
import static com.edmunds.etm.rules.api.RuleComparison.IDENTICAL;
import static com.edmunds.etm.rules.api.RuleComparison.LOW_PRIORITY;
import static com.edmunds.etm.rules.api.RuleComparison.OVERLAP;
import static com.edmunds.etm.rules.api.SegmentType.COMPLETE;
import static com.edmunds.etm.rules.api.SegmentType.DOUBLE_STAR;
import static com.edmunds.etm.rules.api.SegmentType.EMPTY;
import static com.edmunds.etm.rules.api.SegmentType.STAR;
import static com.edmunds.etm.rules.api.SegmentType.TOKEN;
import static com.edmunds.etm.rules.api.SegmentType.WILDCARD;
import static com.edmunds.etm.rules.util.Constants.ANY_SYMBOLS_EXCEPT_SLASH_REGEXP;
import static com.edmunds.etm.rules.util.Constants.ANY_SYMBOLS_REGEXP;
import static com.edmunds.etm.rules.util.Constants.ASTERISK;

/**
 * Represents a segment of a url rule.
 * <p/>
 * Each segment is separated by a / character hence the segments will not contain this character.
 *
 * @author David Trott
 */
public class UrlRuleSegment {

    /**
     * The resolver for tokens into their defining regular expressions.
     */
    private UrlTokenResolver tokenResolver;

    private final String segment;
    private final boolean lastSegment;
    private final SegmentType segmentType;
    private final String prefix;
    private final String postfix;

    /**
     * Constructs a new UrlRule segment.
     *
     * @param segment the body text of the segment.
     * @throws IllegalArgumentException if the segment is invalid (cannot be handled by ETM).
     */
    public UrlRuleSegment(UrlTokenResolver tokenResolver, String segment) throws IllegalArgumentException {
        this(tokenResolver, segment, false);
    }

    /**
     * Constructs a new UrlRule segment.
     *
     * @param segment     the body text of the segment.
     * @param lastSegment is this the last segment (additional validations can be applied).
     * @throws IllegalArgumentException if the segment is invalid (cannot be handled by ETM).
     */
    public UrlRuleSegment(UrlTokenResolver tokenResolver, String segment, boolean lastSegment) throws
            IllegalArgumentException {
        Validate.notNull(tokenResolver, "tokenResolver is null");

        this.tokenResolver = tokenResolver;
        // Special case for last segment.
        if (segment == null) {
            segment = "**";
        }

        this.segment = segment;
        this.lastSegment = lastSegment;

        String regex = this.tokenResolver.resolveToken(segment);
        if (regex != null) {
            this.segmentType = TOKEN;
            this.prefix = null;
            this.postfix = null;
        } else if ("".equals(segment)) {
            this.segmentType = EMPTY;
            this.prefix = "";
            this.postfix = "";
        } else if ("*".equals(segment)) {
            this.segmentType = STAR;
            this.prefix = "";
            this.postfix = "";
        } else if ("**".equals(segment)) {
            this.segmentType = DOUBLE_STAR;
            this.prefix = "";
            this.postfix = "";
        } else {
            final int starIndex = segment.indexOf('*');
            validateFormat(segment, starIndex);

            if (starIndex == -1) {
                this.segmentType = COMPLETE;
                this.prefix = segment;
                this.postfix = segment;
            } else {
                this.segmentType = WILDCARD;
                this.prefix = segment.substring(0, starIndex);
                this.postfix = segment.substring(starIndex + 1);
            }
        }
    }

    public String getSegment() {
        return segment;
    }

    public SegmentType getSegmentType() {
        return segmentType;
    }

    /**
     * Compares this UrlRuleSegment with the other segment.
     * <p/>
     * A return value of HIGH_PRIORITY means this segment has higher priority that the other segment. A return value of
     * OVERLAP means the priorities cannot be determined.
     *
     * @param other the other segment to compare to.
     * @return the result of the comparison.
     */
    public RuleComparison compareTo(UrlRuleSegment other) {
        if (segment.equals(other.segment)) {
            return IDENTICAL;
        }

        // This logic is necessary to handle the combo of STAR - DOUBLE_STAR
        if (other.segmentType == DOUBLE_STAR) {
            return HIGH_PRIORITY;
        } else if (segmentType == STAR || segmentType == DOUBLE_STAR) {
            return LOW_PRIORITY;
        } else if (other.segmentType == STAR) {
            return HIGH_PRIORITY;
        } else if (segmentType == EMPTY || other.segmentType == EMPTY) {
            return DISTINCT;
        }

        // We will only have COMPLETE or PATTERN now.
        if (segmentType == COMPLETE && other.segmentType == COMPLETE) {
            return DISTINCT;
        }

        if (segmentType == TOKEN || other.segmentType == TOKEN) {
            return compareToken(other);
        }

        return compareWildcards(other);
    }

    private RuleComparison compareWildcards(UrlRuleSegment other) {
        final RuleComparison prefixStatus = compareSubString(prefix, other.prefix, true);
        final RuleComparison postfixStatus = compareSubString(postfix, other.postfix, false);

        // Distinct status trumps all.
        if (postfixStatus == DISTINCT) {
            return DISTINCT;
        }

        // Return the prefix status unless it is identical in which case return postfix status
        return prefixStatus != IDENTICAL ? prefixStatus : postfixStatus;
    }

    private RuleComparison compareSubString(String local, String remote, boolean isPrefix) {
        // Assume local string is bigger.
        final boolean reverse = local.length() < remote.length();
        final String bigger;
        final String smaller;

        if (reverse) {
            bigger = remote;
            smaller = local;
        } else {
            bigger = local;
            smaller = remote;
        }

        if (bigger.equals(smaller)) {
            return IDENTICAL;
        }

        final boolean startsDistinct = isPrefix && !bigger.startsWith(smaller);
        final boolean endDistinct = (!isPrefix) && !bigger.endsWith(smaller);

        if (startsDistinct || endDistinct) {
            return DISTINCT;
        }

        return reverse ? LOW_PRIORITY : HIGH_PRIORITY;
    }

    private RuleComparison compareToken(UrlRuleSegment other) {
        // Do we have two patterns
        if (segmentType == TOKEN && other.segmentType == TOKEN) {
            return OVERLAP;
        }

        // Do we have one wildcard and one patterns
        if (segmentType == WILDCARD || other.segmentType == WILDCARD) {
            return OVERLAP;
        }

        final boolean reverse = other.segmentType == TOKEN;
        final UrlRuleSegment tokenSeg;
        final UrlRuleSegment nonToken;
        if (reverse) {
            tokenSeg = other;
            nonToken = this;
        } else {
            tokenSeg = this;
            nonToken = other;
        }

        // By now we must have a PATTERN and a COMPLETE.
        Validate.isTrue(nonToken.getSegmentType() == COMPLETE);

        final String regexp = tokenResolver.resolveToken(tokenSeg.getSegment());
        final String completeSegment = nonToken.getSegment();
        if (regexp != null) {
            if (!completeSegment.matches(regexp)) {
                return DISTINCT;
            }
        } else {
            // Should never happen.
            throw new IllegalStateException("Unmapped Token: " + tokenSeg.getSegment());
        }

        return reverse ? HIGH_PRIORITY : LOW_PRIORITY;
    }

    private void validateFormat(String fileName, int starIndex) {
        if (starIndex != -1 && fileName.indexOf('*', starIndex + 1) != -1) {
            throw new IllegalArgumentException("Two stars detected: " + fileName);
        }

        if (fileName.lastIndexOf('.') == -1 && lastSegment) {
            if (starIndex == -1) {
                String msg = String.format("Filename must contain either a dot or a star: %s", fileName);
                throw new IllegalArgumentException(msg);
            }

            if (starIndex != fileName.length() - 1) {
                String msg = String.format("File names that do not contain '.' must end with a star: %s", fileName);
                throw new IllegalArgumentException(msg);
            }
        }
    }

    @Override
    public String toString() {
        return "UrlRuleSegment{" + "segment='" + segment + "', segmentType=" + segmentType + '}';
    }

    public String toRegEx(UrlTokenResolver urlTokenResolver) {
        switch (segmentType) {
            case COMPLETE:
                return RegexUtil.escapeRegex(segment);
            case STAR:
                return ANY_SYMBOLS_EXCEPT_SLASH_REGEXP;
            case DOUBLE_STAR:
                return ANY_SYMBOLS_REGEXP;
            case TOKEN:
                return urlTokenResolver.resolveToken(segment);
            case WILDCARD:
                return RegexUtil.escapeRegex(segment).replace(ASTERISK, ANY_SYMBOLS_EXCEPT_SLASH_REGEXP);
            case EMPTY:
                return "";
            default:
                throw new IllegalArgumentException("Unknown segment type: " + segmentType);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final UrlRuleSegment other = (UrlRuleSegment) o;

        return new EqualsBuilder()
                .append(segment, other.segment)
                .append(lastSegment, other.lastSegment)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return segment.hashCode();
    }
}
