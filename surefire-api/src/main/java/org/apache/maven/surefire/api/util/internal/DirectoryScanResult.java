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

/**
 * Scan for files in a directory at a given time and reports removed and added files
 * between captures.
 *
 * <p>Copied from maven-shared-utils, so that surefire-api, which ends up on the test class path, needs no
 * dependency for it.
 */
public class DirectoryScanResult {
    private final String[] filesAdded;

    private final String[] filesRemoved;

    /**
     * @param filesAdded added files
     * @param filesRemoved removed files
     */
    public DirectoryScanResult(String[] filesAdded, String[] filesRemoved) {
        this.filesAdded = filesAdded;
        this.filesRemoved = filesRemoved;
    }

    /**
     * @return all files which got detected as being added between 2 capture calls
     */
    public String[] getFilesAdded() {
        return filesAdded;
    }

    /**
     * @return all files which got detected as being removed between 2 capture calls
     */
    public String[] getFilesRemoved() {
        return filesRemoved;
    }
}
