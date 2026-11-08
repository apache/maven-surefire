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

/**
 * Scans a directory for files matching include patterns and not matching exclude patterns, in Ant syntax
 * ({@code *}, {@code ?}, {@code **}) or as {@code %regex[...]}, see {@link SelectorUtils#matchPath}. Matching is
 * case-sensitive and symbolic links are followed.
 * <p>Reduced from the maven-shared-utils scanner to what Surefire uses, so that surefire-api, which ends up on the
 * test class path, needs no dependency for it.
 */
public class DirectoryScanner {

    private File basedir;

    private String[] includes;

    private String[] excludes;

    private MatchPatterns includesPatterns;

    private MatchPatterns excludesPatterns;

    private List<String> filesIncluded;

    /**
     * @param basedir the directory to scan; {@link #scan()} fails if it does not exist or is not a directory
     */
    public void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    public File getBasedir() {
        return basedir;
    }

    /**
     * @param includes patterns relative to the base directory, with {@code /} or {@code \} as separator; a pattern
     *                 ending in a separator includes everything below. {@code null} includes everything.
     * @throws NullPointerException if an element is {@code null}
     */
    public void setIncludes(final String... includes) {
        this.includes = normalize(includes, "includes");
    }

    /**
     * @param excludes patterns relative to the base directory, like {@link #setIncludes}; {@code null} excludes nothing
     * @throws NullPointerException if an element is {@code null}
     */
    public void setExcludes(final String... excludes) {
        this.excludes = normalize(excludes, "excludes");
    }

    private static String[] normalize(final String[] patterns, final String listName) {
        if (patterns == null) {
            return null;
        }
        String[] result = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] == null) {
                throw new NullPointerException(
                        "If a non-null " + listName + " list is given, all elements must be" + " non-null");
            }
            String pattern = patterns[i].trim().replace('/', File.separatorChar).replace('\\', File.separatorChar);
            if (pattern.endsWith(File.separator)) {
                pattern += "**";
            }
            result[i] = pattern;
        }
        return result;
    }

    /**
     * Scans the base directory. Afterwards {@link #getIncludedFiles()} lists the files that match an include pattern
     * and no exclude pattern.
     *
     * @throws IllegalStateException if no base directory was set, or it does not exist or is not a directory
     */
    public void scan() throws IllegalStateException {
        if (basedir == null) {
            throw new IllegalStateException("No basedir set");
        }
        if (!basedir.exists()) {
            throw new IllegalStateException("basedir " + basedir + " does not exist");
        }
        if (!basedir.isDirectory()) {
            throw new IllegalStateException("basedir " + basedir + " is not a directory");
        }
        includesPatterns =
                MatchPatterns.from(includes == null || includes.length == 0 ? new String[] {"**"} : includes);
        excludesPatterns = MatchPatterns.from(excludes == null ? new String[0] : excludes);
        filesIncluded = new ArrayList<>();
        scandir(basedir, "");
    }

    private void scandir(final File dir, final String vpath) {
        String[] newfiles = dir.list();
        if (newfiles == null) {
            // the directory could not be read: nothing to scan there
            return;
        }
        for (final String newfile : newfiles) {
            final String name = vpath + newfile;
            final File file = new File(dir, newfile);
            if (file.isDirectory()) {
                if (isIncluded(name) && !isExcluded(name) || couldHoldIncluded(name)) {
                    scandir(file, name + File.separator);
                }
            } else if (file.isFile() && isIncluded(name) && !isExcluded(name)) {
                filesIncluded.add(name);
            }
        }
    }

    private boolean isIncluded(final String name) {
        return includesPatterns.matches(name, true);
    }

    private boolean couldHoldIncluded(final String name) {
        return includesPatterns.matchesPatternStart(name, true);
    }

    private boolean isExcluded(final String name) {
        return excludesPatterns.matches(name, true);
    }

    /**
     * @return the matched files, relative to the base directory with {@link File#separator}, in directory listing
     *         order; empty before {@link #scan()}
     */
    public String[] getIncludedFiles() {
        return filesIncluded == null ? new String[0] : filesIncluded.toArray(new String[0]);
    }
}
