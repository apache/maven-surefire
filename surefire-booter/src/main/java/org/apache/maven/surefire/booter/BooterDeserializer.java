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

import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.internal.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Knows how to serialize and deserialize the booter configuration.
 * <p/>
 * The internal serialization format is through a properties file. The long-term goal of this
 * class is not to expose this implementation information to its clients. This still leaks somewhat,
 * and there are some cases where properties are being accessed as "Properties" instead of
 * more representative domain objects.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class BooterDeserializer
    implements BooterConstants
{


    final PropertiesWrapper properties;

    public BooterDeserializer( InputStream inputStream )
        throws IOException
    {
        properties = SystemPropertyManager.loadProperties( inputStream );
    }

    public ProviderConfiguration deserialize()
        throws IOException
    {

        final File reportsDirectory = new File( properties.getProperty( REPORTSDIRECTORY ) );
        final String testNgVersion = properties.getProperty( TESTARTIFACT_VERSION );
        final String testArtifactClassifier = properties.getProperty( TESTARTIFACT_CLASSIFIER );
        final Object testForFork = properties.getTypeDecoded( FORKTESTSET );
        final String requestedTest = properties.getProperty( REQUESTEDTEST );
        final File sourceDirectory =
            (File) getParamValue( properties.getProperty( SOURCE_DIRECTORY ), File.class.getName() );

        final List reports = properties.getStringList( REPORT_PROPERTY_PREFIX );
        final List excludesList = properties.getStringList( EXCLUDES_PROPERTY_PREFIX );
        final List includesList = properties.getStringList( INCLUDES_PROPERTY_PREFIX );

        final List testSuiteXmlFiles = properties.getStringList( TEST_SUITE_XML_FILES );
        final File testClassesDirectory = properties.getFileProperty( TEST_CLASSES_DIRECTORY );
        final String runOrder = properties.getProperty( RUN_ORDER );

        DirectoryScannerParameters dirScannerParams =
            new DirectoryScannerParameters( testClassesDirectory, includesList, excludesList,
                                            valueOf( properties.getBooleanProperty( FAILIFNOTESTS ) ) , runOrder);

        TestArtifactInfo testNg = new TestArtifactInfo( testNgVersion, testArtifactClassifier );
        TestRequest testSuiteDefinition = new TestRequest( testSuiteXmlFiles, sourceDirectory, requestedTest );

        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( reports, reportsDirectory, valueOf(
            properties.getBooleanProperty( ISTRIMSTACKTRACE ) ) );

        return new ProviderConfiguration( dirScannerParams, properties.getBooleanProperty( FAILIFNOTESTS ),
                                          reporterConfiguration, testNg, testSuiteDefinition,
                                          properties.getProperties(), testForFork );
    }

    public StartupConfiguration getProviderConfiguration()
        throws IOException
    {
        boolean enableAssertions = properties.getBooleanProperty( ENABLE_ASSERTIONS );
        boolean childDelegation = properties.getBooleanProperty( CHILD_DELEGATION );
        boolean useSystemClassLoader = properties.getBooleanProperty( USESYSTEMCLASSLOADER );
        boolean useManifestOnlyJar = properties.getBooleanProperty( USEMANIFESTONLYJAR );
        String providerConfiguration = properties.getProperty( PROVIDER_CONFIGURATION );


        final List classpath = properties.getStringList( CLASSPATH_URL );
        final List sureFireClasspath = properties.getStringList( SUREFIRE_CLASSPATHURL );

        ClassLoaderConfiguration classLoaderConfiguration =
            new ClassLoaderConfiguration( useSystemClassLoader, useManifestOnlyJar );

        ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( classpath, sureFireClasspath, enableAssertions, childDelegation );

        return StartupConfiguration.inForkedVm( providerConfiguration, classpathConfiguration,
                                                classLoaderConfiguration );
    }

    private Boolean valueOf( boolean aBoolean )
    {  // jdk1.3 compat
        return aBoolean ? Boolean.TRUE : Boolean.FALSE;
    }

    private static List processStringList( String stringList )
    {
        String sl = stringList;

        if ( sl.startsWith( "[" ) && sl.endsWith( "]" ) )
        {
            sl = sl.substring( 1, sl.length() - 1 );
        }

        List list = new ArrayList();

        String[] stringArray = StringUtils.split(sl, ",");

        for ( int i = 0; i < stringArray.length; i++ )
        {
            list.add( stringArray[i].trim() );
        }
        return list;
    }

    private static Object[] constructParamObjects( String paramProperty, Class typeProperty )
    {
        Object[] paramObjects = null;
        if ( paramProperty != null )
        {
            // bit of a glitch that it need sto be done twice to do an odd number of vertical bars (eg |||, |||||).
            String[] params = StringUtils.split(
                StringUtils.replace( StringUtils.replace( paramProperty, "||", "| |" ), "||", "| |" ), "|" );
            paramObjects = new Object[params.length];

            String typeName = typeProperty.getName();
            for ( int i = 0; i < params.length; i++ )
            {
                String param = params[i];
                paramObjects[i] = getParamValue( param, typeName );
            }
        }
        return paramObjects;
    }

    private static Object getParamValue( String param, String typeName )
    {
        if ( typeName.trim().length() == 0 )
        {
            return null;
        }
        else if ( typeName.equals( String.class.getName() ) )
        {
            return param;
        }
        else if ( typeName.equals( File.class.getName() ) )
        {
            return new File( param );
        }
        else if ( typeName.equals( File[].class.getName() ) )
        {
            List stringList = processStringList( param );
            File[] fileList = new File[stringList.size()];
            for ( int j = 0; j < stringList.size(); j++ )
            {
                fileList[j] = new File( (String) stringList.get( j ) );
            }
            return fileList;
        }
        else if ( typeName.equals( ArrayList.class.getName() ) )
        {
            return processStringList( param );
        }
        else if ( typeName.equals( Boolean.class.getName() ) )
        {
            return Boolean.valueOf( param );
        }
        else if ( typeName.equals( Integer.class.getName() ) )
        {
            return Integer.valueOf( param );
        }
        else if ( typeName.equals( Properties.class.getName() ) )
        {
            final Properties result = new Properties();
            try
            {
                ByteArrayInputStream bais = new ByteArrayInputStream( param.getBytes( "8859_1" ) );
                result.load( bais );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( "bug in property conversion", e );
            }
            return result;
        }
        else
        {
            // TODO: could attempt to construct with a String constructor if needed
            throw new IllegalArgumentException( "Unknown parameter type: " + typeName );
        }
    }
}
