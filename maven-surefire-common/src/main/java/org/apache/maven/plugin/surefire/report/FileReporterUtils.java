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
package org.apache.maven.plugin.surefire.report;

import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;

/**
 * Utils class for file-based reporters.
 *
 * @author Andreas Gudian
 */
public final class FileReporterUtils {
    // well under the 255-byte limit most filesystems enforce per path component,
    // leaving room for encodings that use more than one byte per character and
    // for filesystems with a lower limit still (e.g. eCryptfs)
    private static final int MAX_FILE_NAME_LENGTH = 200;

    private FileReporterUtils() {
        throw new IllegalStateException("non instantiable constructor");
    }

    public static String stripIllegalFilenameChars(String original) {
        StringBuilder result = new StringBuilder(original);
        String illegalChars = getOSSpecificIllegalChars();
        for (int i = 0, len = result.length(); i < len; i++) {
            char charFromOriginal = result.charAt(i);
            boolean isIllegalChar = illegalChars.indexOf(charFromOriginal) != -1;
            if (isIllegalChar) {
                result.setCharAt(i, '_');
            }
        }
        return result.toString();
    }

    /**
     * Shortens a report file name that would otherwise exceed common filesystem limits
     * (e.g. a deeply nested, long test class name), keeping it unique by appending a hash
     * of the untruncated name.
     *
     * @param fileName the file name, including its extension
     * @return {@code fileName} unchanged if it already fits, otherwise a truncated name of
     *         the same extension
     */
    public static String truncateFileName(String fileName) {
        if (fileName.length() <= MAX_FILE_NAME_LENGTH) {
            return fileName;
        }
        int dot = fileName.lastIndexOf('.');
        String extension = dot == -1 ? "" : fileName.substring(dot);
        String hash = "_" + Integer.toHexString(fileName.hashCode());
        int keep = MAX_FILE_NAME_LENGTH - extension.length() - hash.length();
        String base = dot == -1 ? fileName : fileName.substring(0, dot);
        return base.substring(0, keep) + hash + extension;
    }

    private static String getOSSpecificIllegalChars() {
        // forbidden and quoted characters
        // https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
        // https://cygwin.com/cygwin-ug-net/using-specialnames.html
        // https://www.cyberciti.biz/faq/linuxunix-rules-for-naming-file-and-directory-names/
        return IS_OS_WINDOWS ? "[],\\/:*?\"<>|\0" : "()&\\/:*?\"<>|\0";
    }
}
