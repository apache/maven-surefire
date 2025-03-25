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
package org.apache.maven.surefire.api.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for the surefire instance of temp file manager.
 *
 * @author Markus Spann
 */
public class SureFireFileManagerTest {

    @Test
    @Ignore("old not executing tests - to review")
    public void testCreateTempFile() throws IOException {

        File tempFile = SureFireFileManager.createTempFile("sfprefix", "sfsuffix");
        assertThat(tempFile).isWritable();
        assertThat(tempFile.getName()).startsWith("sfprefix").endsWith("sfsuffix");

        File tempDir = tempFile.getParentFile();
        assertThat(tempDir).isDirectory().isWritable();
        assertThat(tempDir.getName()).startsWith("surefire-").doesNotMatch("[^A-Za-z0-9\\\\-_]");

        boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        if (isPosix) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(tempDir.toPath());
            assertEquals("rwxrwxr-x", PosixFilePermissions.toString(permissions));
        }
    }
}
