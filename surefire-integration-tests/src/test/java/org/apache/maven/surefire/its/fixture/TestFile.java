package org.apache.maven.surefire.its.fixture;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static junit.framework.Assert.assertTrue;

/**
 * @author Kristian Rosenvold
 */
public class TestFile {

    private final File file;

    private final OutputValidator surefireVerifier;

    public TestFile( File file, OutputValidator surefireVerifier ) {
        this.file = file;
        this.surefireVerifier = surefireVerifier;
    }

    public OutputValidator assertFileExists()
    {
        assertTrue( "File doesn't exist: " + file.getAbsolutePath(), file.exists() );
        return surefireVerifier;
    }

    public void delete()
    {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public String getAbsolutePath()
    {
        return file.getAbsolutePath();
    }

    public boolean exists()
    {
        return file.exists();
    }

    public FileInputStream getFileInputStream()
        throws FileNotFoundException
    {
        return new FileInputStream( file );
    }
}
