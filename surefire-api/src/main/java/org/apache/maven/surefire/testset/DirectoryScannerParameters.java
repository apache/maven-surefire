package org.apache.maven.surefire.testset;
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
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Kristian Rosenvold
 */
public class DirectoryScannerParameters
{
    final File testClassesDirectory;

    final List includes;

    final List excludes;

    private final Boolean failIfNoTests;


    public DirectoryScannerParameters( File testClassesDirectory, List includes, List excludes, Boolean failIfNoTests )
    {
        this.testClassesDirectory = testClassesDirectory;
        this.includes = includes;
        this.excludes = excludes;
        this.failIfNoTests = failIfNoTests;
    }

    public File getTestClassesDirectory()
    {
        return testClassesDirectory;
    }

    public List getIncludes()
    {
        return includes;
    }

    public List getExcludes()
    {
        return excludes;
    }

    public Boolean isFailIfNoTests()
    {
        return failIfNoTests;
    }
}
