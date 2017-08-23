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
import java.util.List;
import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.plugin.surefire.runorder.impl.RunOrderLoader;

import static org.apache.maven.plugin.surefire.runorder.impl.RunOrderLoader.defaultRunOrder;

/**
 * @author Kristian Rosenvold
 */
public class DirectoryScannerParameters
{
    private final File testClassesDirectory;

    @Deprecated
    private final List<String> includes;

    @Deprecated
    private final List<String> excludes;

    @Deprecated
    private final List<String> specificTests;

    private final boolean failIfNoTests;

    private final RunOrder[] runOrders;

    private DirectoryScannerParameters( File testClassesDirectory, List<String> includes, List<String> excludes,
                                        List<String> specificTests, boolean failIfNoTests, RunOrder[] runOrders )
    {
        this.testClassesDirectory = testClassesDirectory;
        this.includes = includes;
        this.excludes = excludes;
        this.specificTests = specificTests;
        this.failIfNoTests = failIfNoTests;
        this.runOrders = runOrders;
    }

    public DirectoryScannerParameters( File testClassesDirectory, @Deprecated List<String> includes,
                                       @Deprecated List<String> excludes, @Deprecated List<String> specificTests,
                                       boolean failIfNoTests, String runOrder )
    {
        this( testClassesDirectory, includes, excludes, specificTests, failIfNoTests,
              runOrder == null ? defaultRunOrder() : RunOrderLoader.runOrdersOf( runOrder ) );
    }

    @Deprecated
    public List<String> getSpecificTests()
    {
        return specificTests;
    }

    /**
     * Returns the directory of the compiled classes, normally ${project.build.testOutputDirectory}
     *
     * @return A directory that can be scanned for .class files
     */
    public File getTestClassesDirectory()
    {
        return testClassesDirectory;
    }

    /**
     * The includes pattern list, as specified on the plugin includes parameter.
     *
     * @return A list of patterns. May contain both source file designators and .class extensions.
     */
    @Deprecated
    public List<String> getIncludes()
    {
        return includes;
    }

    /**
     * The excludes pattern list, as specified on the plugin includes parameter.
     *
     * @return A list of patterns. May contain both source file designators and .class extensions.
     */
    @Deprecated
    public List<String> getExcludes()
    {
        return excludes;
    }

    /**
     * Indicates if lack of runable tests should fail the entire build
     *
     * @return true if no tests should fail the build
     */
    public boolean isFailIfNoTests()
    {
        return failIfNoTests;
    }

    /**
     * RunOrder Implementations, as specified on the plugin runOrder parameter.
     *
     * @return The RunOrders
     */
    public RunOrder[] getRunOrders()
    {
        return runOrders;
    }
}
