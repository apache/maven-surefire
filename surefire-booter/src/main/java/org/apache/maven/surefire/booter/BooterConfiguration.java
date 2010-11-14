package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.suite.SuiteDefinition;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Represents the surefire configuration that crosses booter forks into other vms and classloaders.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class BooterConfiguration
{
    /**
     * @noinspection UnusedDeclaration
     */
    public static final int TESTS_SUCCEEDED_EXIT_CODE = 0;

    public static final int TESTS_FAILED_EXIT_CODE = 255;

    public static final int NO_TESTS_EXIT_CODE = 254;

    private final ForkConfiguration forkConfiguration;

    private final ClasspathConfiguration classpathConfiguration;

    private final List reports = new ArrayList();

    private SuiteDefinition suiteDefinition;

    private boolean failIfNoTests = false;

    private boolean redirectTestOutputToFile = false;

    private Properties properties; // todo: Zap out of here !

    Object[] dirScannerParams;

    /**
     * This field is set to true if it's running from main. It's used to help decide what classloader to use.
     */
    private boolean forked;

    public BooterConfiguration( ForkConfiguration forkConfiguration, ClasspathConfiguration classpathConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
        this.classpathConfiguration = classpathConfiguration;
    }

    public BooterConfiguration( ForkConfiguration forkConfiguration, ClasspathConfiguration classpathConfiguration,
                                SuiteDefinition suiteDefinition, List reports, boolean forked,
                                Object[] dirScannerParams, boolean failIfNoTests, Properties properties )
    {
        this.forkConfiguration = forkConfiguration;
        this.classpathConfiguration = classpathConfiguration;
        this.suiteDefinition = suiteDefinition;
        this.reports.addAll( reports );
        this.forked = forked;
        this.dirScannerParams = dirScannerParams;
        this.failIfNoTests = failIfNoTests;
        this.properties = properties; // Hack hack. This must go
    }


    ClasspathConfiguration getClasspathConfiguration()
    {
        return classpathConfiguration;
    }

    public boolean useSystemClassLoader()
    {
        return forkConfiguration.isUseSystemClassLoader() && ( forked || forkConfiguration.isForking() );
    }


    public List getReports()
    {
        return reports;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public boolean isRedirectTestOutputToFile()
    {
        return redirectTestOutputToFile;
    }

    public List getTestSuites()
    {
        return suiteDefinition.asBooterFormat();
    }


    public Boolean isFailIfNoTests()
    {
        return ( failIfNoTests ) ? Boolean.TRUE : Boolean.FALSE;
    }

    public ForkConfiguration getForkConfiguration()
    {
        return forkConfiguration;
    }

    public void addReport( String report, Object[] constructorParams )
    {
        reports.add( new Object[]{ report, constructorParams } );
    }

    /**
     * Setting this to true will cause a failure if there are no tests to run
     *
     * @param failIfNoTests true if we should fail with no tests
     */
    public void setFailIfNoTests( boolean failIfNoTests )
    {
        this.failIfNoTests = failIfNoTests;
    }

    /**
     * When forking, setting this to true will make the test output to be saved in a file instead of showing it on the
     * standard output
     *
     * @param redirectTestOutputToFile to redirect test output to file
     */
    public void setRedirectTestOutputToFile( boolean redirectTestOutputToFile )
    {
        this.redirectTestOutputToFile = redirectTestOutputToFile;
    }

    public boolean isForking()
    {
        return forkConfiguration.isForking();
    }

    public void setDirectoryScannerOptions( File testClassesDirectory, List includes, List excludes )
    {
        dirScannerParams = new Object[]{ testClassesDirectory, includes, excludes };
    }

    public File getBaseDir()
    {
        return (File) getDirScannerParams()[0];
    }

    private Object[] getDirScannerParams()
    {
        if ( dirScannerParams == null )
        {
            throw new IllegalStateException( "Requesting paramater basedir which has not been set" );
        }
        return dirScannerParams;
    }

    public List getIncludes()
    {
        return (List) getDirScannerParams()[1];
    }

    public List getExcludes()
    {
        return (List) getDirScannerParams()[2];
    }

    public void setSuiteDefinition( SuiteDefinition suiteDefinition )
    {
        this.suiteDefinition = suiteDefinition;
    }
}
