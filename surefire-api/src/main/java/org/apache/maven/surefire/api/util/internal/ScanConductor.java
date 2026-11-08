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

/**
 * <p>Visitor pattern for the DirectoryScanner. A ScanConductor controls the scanning process.</p>
 * <p>Create an instance and pass it to
 * {@link org.apache.maven.surefire.api.util.internal.DirectoryScanner#setScanConductor(ScanConductor)}.
 * You will get notified about every visited directory and file. You can use the {@link ScanAction}
 * to control what should happen next.</p>
 * <p>A ScanConductor might also store own information but users must make sure that the state gets
 * cleaned between two scan() invocations.</p>
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * <p>Copied from maven-shared-utils, so that surefire-api, which ends up on the test class path, needs no
 * dependency for it.
 */
public interface ScanConductor {
    enum ScanAction {
        /**
         * Abort the whole scanning process. The current file will not
         * be added anymore.
         */
        ABORT,

        /**
         * Continue the scanning with the next item in the list.
         */
        CONTINUE,

        /**
         * This response is only valid for {@link ScanConductor#visitDirectory(String, java.io.File)}.
         * Do not recurse into the current directory. The current directory will not be added
         * and the processing will be continued with the next item in the list.
         */
        NO_RECURSE,

        /**
         * Abort processing the current directory.
         * The current file will not be added.
         * The processing will continue it's scan in the parent directory if any.
         */
        ABORT_DIRECTORY
    }

    /**
     * This method will get invoked for every detected directory.
     *
     * @param name      the directory name (contains parent folders up to the pwd)
     * @param directory the directory
     * @return the ScanAction to control how to proceed with the scanning
     */
    ScanAction visitDirectory(String name, File directory);

    /**
     * This method will get invoked for every detected file.
     *
     * @param name the file name (contains parent folders up to the pwd)
     * @param file the file
     * @return the ScanAction to control how to proceed with the scanning
     */
    ScanAction visitFile(String name, File file);
}
