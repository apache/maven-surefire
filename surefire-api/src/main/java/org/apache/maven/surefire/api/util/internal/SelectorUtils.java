/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.api.util.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * <p>This is a utility class used by selectors and DirectoryScanner. The
 * functionality more properly belongs just to selectors, but unfortunately
 * DirectoryScanner exposed these as protected methods. Thus we have to
 * support any subclasses of DirectoryScanner that may access these methods.
 * </p>
 * <p>This is a Singleton.</p>
 *
 * @author Arnout J. Kuiper
 *         <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * <p>Copied from maven-shared-utils, so that surefire-api, which ends up on the test class path, needs no
 * dependency for it.
 */
public final class SelectorUtils {

    /**
     * Pattern handler prefix.
     */
    private static final String PATTERN_HANDLER_PREFIX = "[";

    /**
     * Pattern handler suffix.
     */
    public static final String PATTERN_HANDLER_SUFFIX = "]";

    /**
     * Regex start pattern.
     */
    public static final String REGEX_HANDLER_PREFIX = "%regex" + PATTERN_HANDLER_PREFIX;

    /**
     * ANT pattern prefix.
     */
    public static final String ANT_HANDLER_PREFIX = "%ant" + PATTERN_HANDLER_PREFIX;

    /**
     * Private Constructor.
     */
    private SelectorUtils() {}

    /**
     * <p>Tests whether or not a given path matches the start of a given
     * pattern up to the first "**".</p>
     * <p>
     * This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.</p>
     *
     * @param pattern the pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     the path to match, as a String. Must not be
     *                <code>null</code>.
     * @return whether or not a given path matches the start of a given
     *         pattern up to the first "**"
     */
    public static boolean matchPatternStart(String pattern, String str) {
        return matchPatternStart(pattern, str, true);
    }

    /**
     * <p>Tests whether or not a given path matches the start of a given
     * pattern up to the first "**".</p>
     * <p>This is not a general purpose test and should only be used if you
     * can live with false positives. For example, <code>pattern=**\a</code>
     * and <code>str=b</code> will yield <code>true</code>.</p>
     *
     * @param pattern         the pattern to match against. Must not be
     *                        <code>null</code>.
     * @param str             the path to match, as a String. Must not be
     *                        <code>null</code>.
     * @param isCaseSensitive whether or not matching should be performed
     *                        case sensitively
     * @return whether or not a given path matches the start of a given
     *         pattern up to the first "**"
     */
    public static boolean matchPatternStart(String pattern, String str, boolean isCaseSensitive) {
        if (isRegexPrefixedPattern(pattern)) {
            // FIXME: ICK! But we can't do partial matches for regex, so we have to reserve judgement until we have
            // a file to deal with, or we can definitely say this is an exclusion...
            return true;
        } else {
            if (isAntPrefixedPattern(pattern)) {
                pattern = pattern.substring(
                        ANT_HANDLER_PREFIX.length(), pattern.length() - PATTERN_HANDLER_SUFFIX.length());
            }

            String altPattern = pattern.replace('\\', '/');
            String altStr = str.replace('\\', '/');

            return matchAntPathPatternStart(altPattern, altStr, "/", isCaseSensitive);
        }
    }

    private static boolean matchAntPathPatternStart(
            String pattern, String str, String separator, boolean isCaseSensitive) {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if (separatorPatternStartSlashMismatch(pattern, str, separator)) {
            return false;
        }

        List<String> patDirs = tokenizePath(pattern, separator);
        List<String> strDirs = tokenizePath(str, separator);

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxStart);
            if ("**".equals(patDir)) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxStart), isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        return strIdxStart > strIdxEnd || patIdxStart <= patIdxEnd;
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern the pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     the path to match, as a String. Must not be
     *                <code>null</code>.
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise
     */
    public static boolean matchPath(String pattern, String str) {
        return matchPath(pattern, str, true);
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern         the pattern to match against. Must not be
     *                        <code>null</code>.
     * @param str             the path to match, as a String. Must not be
     *                        <code>null</code>.
     * @param isCaseSensitive whether or not matching should be performed
     *                        case sensitively
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise
     */
    public static boolean matchPath(String pattern, String str, boolean isCaseSensitive) {
        if (pattern.length() > (REGEX_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1)
                && pattern.startsWith(REGEX_HANDLER_PREFIX)
                && pattern.endsWith(PATTERN_HANDLER_SUFFIX)) {
            pattern = pattern.substring(
                    REGEX_HANDLER_PREFIX.length(), pattern.length() - PATTERN_HANDLER_SUFFIX.length());

            return str.matches(pattern);
        } else {
            if (pattern.length() > (ANT_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1)
                    && pattern.startsWith(ANT_HANDLER_PREFIX)
                    && pattern.endsWith(PATTERN_HANDLER_SUFFIX)) {
                pattern = pattern.substring(
                        ANT_HANDLER_PREFIX.length(), pattern.length() - PATTERN_HANDLER_SUFFIX.length());
            }

            return matchAntPathPattern(pattern, str, isCaseSensitive);
        }
    }

    private static boolean matchAntPathPattern(String pattern, String str, boolean isCaseSensitive) {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if (str.startsWith(File.separator) != pattern.startsWith(File.separator)) {
            return false;
        }

        List<String> patDirs = tokenizePath(pattern, File.separator);
        List<String> strDirs = tokenizePath(str, File.separator);

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxStart);
            if ("**".equals(patDir)) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxStart), isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!"**".equals(patDirs.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxEnd);
            if ("**".equals(patDir)) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxEnd), isCaseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!"**".equals(patDirs.get(i))) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if ("**".equals(patDirs.get(i))) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patDirs.get(patIdxStart + j + 1);
                    String subStr = strDirs.get(strIdxStart + i + j);
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!"**".equals(patDirs.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character.
     *
     * @param pattern the pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     the string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise
     */
    public static boolean match(String pattern, String str) {
        return match(pattern, str, true);
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character.
     *
     * @param pattern         the pattern to match against.
     *                        Must not be <code>null</code>.
     * @param str             the string which must be matched against the pattern.
     *                        Must not be <code>null</code>.
     * @param isCaseSensitive whether or not matching should be performed
     *                        case sensitively
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise
     */
    public static boolean match(String pattern, String str, boolean isCaseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (char aPatArr : patArr) {
            if (aPatArr == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?' && !equals(ch, strArr[i], isCaseSensitive)) {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        // CHECKSTYLE_OFF: InnerAssignment
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd)
        // CHECKSTYLE_ON: InnerAssignment
        {
            if (ch != '?' && !equals(ch, strArr[strIdxStart], isCaseSensitive)) {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        // CHECKSTYLE_OFF: InnerAssignment
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd)
        // CHECKSTYLE_ON: InnerAssignment
        {
            if (ch != '?' && !equals(ch, strArr[strIdxEnd], isCaseSensitive)) {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?' && !equals(ch, strArr[strIdxStart + i + j], isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether two characters are equal.
     */
    private static boolean equals(char c1, char c2, boolean isCaseSensitive) {
        if (c1 == c2) {
            return true;
        }
        if (!isCaseSensitive) {
            // NOTE: Try both upper case and lower case as done by String.equalsIgnoreCase()
            if (Character.toUpperCase(c1) == Character.toUpperCase(c2)
                    || Character.toLowerCase(c1) == Character.toLowerCase(c2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Breaks a path up into a List of path elements, tokenizing on
     * <code>File.separator</code>.
     *
     * @param path path to tokenize. Must not be <code>null</code>.
     * @param separator the separator to use
     * @return a List of path elements from the tokenized path
     */
    private static List<String> tokenizePath(String path, String separator) {
        List<String> ret = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            ret.add(st.nextToken());
        }
        return ret;
    }

    static boolean matchAntPathPatternStart(
            MatchPattern pattern, String str, String separator, boolean isCaseSensitive) {
        return !separatorPatternStartSlashMismatch(pattern, str, separator)
                && matchAntPathPatternStart(pattern.getTokenizedPathString(), str, separator, isCaseSensitive);
    }

    private static String[] tokenizePathToString(String path, String separator) {
        List<String> ret = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            ret.add(st.nextToken());
        }
        return ret.toArray(new String[ret.size()]);
    }

    private static boolean matchAntPathPatternStart(
            String[] patDirs, String str, String separator, boolean isCaseSensitive) {
        String[] strDirs = tokenizePathToString(str, separator);
        return matchAntPathPatternStart(patDirs, strDirs, isCaseSensitive);
    }

    private static boolean matchAntPathPatternStart(
            String[] patDirs, String[] tokenizedFileName, boolean isCaseSensitive) {

        int patIdxStart = 0;
        int patIdxEnd = patDirs.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = tokenizedFileName.length - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxStart];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, tokenizedFileName[strIdxStart], isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        return strIdxStart > strIdxEnd || patIdxStart <= patIdxEnd;
    }

    private static boolean separatorPatternStartSlashMismatch(MatchPattern matchPattern, String str, String separator) {
        return str.startsWith(separator) != matchPattern.startsWith(separator);
    }

    private static boolean separatorPatternStartSlashMismatch(String pattern, String str, String separator) {
        return str.startsWith(separator) != pattern.startsWith(separator);
    }

    static boolean matchAntPathPattern(String[] patDirs, String[] strDirs, boolean isCaseSensitive) {
        int patIdxStart = 0;
        int patIdxEnd = patDirs.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.length - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxStart];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, strDirs[strIdxStart], isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxEnd];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, strDirs[strIdxEnd], isCaseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patDirs[patIdxStart + j + 1];
                    String subStr = strDirs[strIdxStart + i + j];
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!patDirs[i].equals("**")) {
                return false;
            }
        }

        return true;
    }

    static boolean isRegexPrefixedPattern(String pattern) {
        return pattern.length() > (REGEX_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1)
                && pattern.startsWith(REGEX_HANDLER_PREFIX)
                && pattern.endsWith(PATTERN_HANDLER_SUFFIX);
    }

    static boolean isAntPrefixedPattern(String pattern) {
        return pattern.length() > (ANT_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1)
                && pattern.startsWith(ANT_HANDLER_PREFIX)
                && pattern.endsWith(PATTERN_HANDLER_SUFFIX);
    }

    static boolean matchAntPathPattern(
            MatchPattern matchPattern, String str, String separator, boolean isCaseSensitive) {
        if (separatorPatternStartSlashMismatch(matchPattern, str, separator)) {
            return false;
        }
        String[] patDirs = matchPattern.getTokenizedPathString();
        String[] strDirs = tokenizePathToString(str, separator);
        return matchAntPathPattern(patDirs, strDirs, isCaseSensitive);
    }
}
