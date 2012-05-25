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
package com.edmunds.etm.runtime.api;

import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * A {@code VersionNumber} specifies the version of an application.
 * <p/>
 * {@code VersionNumber} objects are compared according to their numerical components rather than lexicographically.
 * Maven-style qualifiers are also taken into account.
 * <p/>
 * This class is an adaptation of Maven's DefaultArtifactVersion and is subject to the Apache License, version 2.0.
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Ryan Holmes
 */
public class ApplicationVersion implements Comparable<ApplicationVersion>, Serializable {

    private Integer majorVersion;
    private Integer minorVersion;
    private Integer incrementalVersion;
    private Integer buildNumber;
    private String qualifier;
    private String unparsed;

    /**
     * Constructs a new {@code VersionNumber} with the specified version.
     *
     * @param version the version string
     */
    public ApplicationVersion(String version) {
        Validate.notEmpty(version);
        parseVersion(version);
    }

    public int getMajorVersion() {
        return majorVersion != null ? majorVersion : 0;
    }

    public int getMinorVersion() {
        return minorVersion != null ? minorVersion : 0;
    }

    public int getIncrementalVersion() {
        return incrementalVersion != null ? incrementalVersion : 0;
    }

    public int getBuildNumber() {
        return buildNumber != null ? buildNumber : 0;
    }

    public String getQualifier() {
        return qualifier;
    }

    public int compareTo(ApplicationVersion otherVersion) {

        int result = getMajorVersion() - otherVersion.getMajorVersion();
        if (result == 0) {
            result = getMinorVersion() - otherVersion.getMinorVersion();
        }
        if (result == 0) {
            result = getIncrementalVersion() - otherVersion.getIncrementalVersion();
        }
        if (result == 0) {
            if (qualifier != null) {
                String otherQualifier = otherVersion.getQualifier();

                if (otherQualifier != null) {
                    if ((qualifier.length() > otherQualifier.length())
                            && qualifier.startsWith(otherQualifier)) {
                        // here, the longer one that otherwise match is considered older
                        result = -1;
                    } else if ((qualifier.length() < otherQualifier.length())
                            && otherQualifier.startsWith(qualifier)) {
                        // here, the longer one that otherwise match is considered older
                        result = 1;
                    } else {
                        result = qualifier.compareTo(otherQualifier);
                    }
                } else {
                    // otherVersion has no qualifier but we do - that's newer
                    result = -1;
                }
            } else if (otherVersion.getQualifier() != null) {
                // otherVersion has a qualifier but we don't, we're newer
                result = 1;
            } else {
                result = getBuildNumber() - otherVersion.getBuildNumber();
            }
        }
        return result;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ApplicationVersion)) {
            return false;
        }

        ApplicationVersion that = (ApplicationVersion) other;

        return 0 == compareTo(that);
    }

    public int hashCode() {
        int result = 1229;

        result = 1223 * result + getMajorVersion();
        result = 1223 * result + getMinorVersion();
        result = 1223 * result + getIncrementalVersion();
        result = 1223 * result + getBuildNumber();

        if (null != getQualifier()) {
            result = 1223 * result + getQualifier().hashCode();
        }

        return result;
    }

    public String toString() {
        return unparsed;
    }

    private void parseVersion(String version) {
        unparsed = version;

        int index = version.indexOf("-");

        String part1;
        String part2 = null;

        if (index < 0) {
            part1 = version;
        } else {
            part1 = version.substring(0, index);
            part2 = version.substring(index + 1);
        }

        if (part2 != null) {
            parseBuildNumberOrQualifier(part2);
        }

        if ((part1.indexOf(".") < 0) && !part1.startsWith("0")) {
            try {
                majorVersion = Integer.valueOf(part1);
            } catch (NumberFormatException e) {
                // qualifier is the whole version, including "-"
                qualifier = version;
                buildNumber = null;
            }
        } else {
            boolean fallback = parseVersionNumbers(part1);

            if (fallback) {
                // qualifier is the whole version, including "-"
                qualifier = version;
                majorVersion = null;
                minorVersion = null;
                incrementalVersion = null;
                buildNumber = null;
            }
        }
    }

    private void parseBuildNumberOrQualifier(String value) {
        try {
            if ((value.length() == 1) || !value.startsWith("0")) {
                buildNumber = Integer.valueOf(value);
            } else {
                qualifier = value;
            }
        } catch (NumberFormatException e) {
            qualifier = value;
        }
    }

    private boolean parseVersionNumbers(String value) {
        boolean fallback = false;

        StringTokenizer tok = new StringTokenizer(value, ".");
        try {
            majorVersion = getNextIntegerToken(tok);
            if (tok.hasMoreTokens()) {
                minorVersion = getNextIntegerToken(tok);
            }
            if (tok.hasMoreTokens()) {
                incrementalVersion = getNextIntegerToken(tok);
            }
            if (tok.hasMoreTokens()) {
                fallback = true;
            }

            // string tokenzier won't detect these and ignores them
            if (value.indexOf("..") >= 0 || value.startsWith(".") || value.endsWith(".")) {
                fallback = true;
            }
        } catch (NumberFormatException e) {
            fallback = true;
        }

        return fallback;
    }

    private static Integer getNextIntegerToken(StringTokenizer tok) {
        String s = tok.nextToken();
        if ((s.length() > 1) && s.startsWith("0")) {
            throw new NumberFormatException("Number part has a leading 0: '" + s + "'");
        }
        return Integer.valueOf(s);
    }
}
