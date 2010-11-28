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

import org.apache.maven.surefire.providerapi.ProviderConfiguration;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestSuiteDefinition;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

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
{
    public static final String INCLUDES_PROPERTY_PREFIX = "includes";

    public static final String EXCLUDES_PROPERTY_PREFIX = "excludes";

    public static final String DIRSCANNER_PROPERTY_PREFIX = "dirscanner.";

    public static final String REPORT_PROPERTY_PREFIX = "report.";

    public static final String PARAMS_SUFIX = ".params";

    public static final String TYPES_SUFIX = ".types";


    public BooterConfiguration deserialize( InputStream inputStream )
        throws IOException
    {
        Properties properties = SystemPropertyManager.loadProperties( inputStream );
        DirectoryScannerParameters dirScannerParams;
        boolean enableAssertions = false;
        boolean childDelegation = true;
        boolean failIfNotests = false;  // todo; check this out.
        boolean useSystemClassLoader = false; // todo check default value
        boolean useManifestOnlyJar = false; // todo check default value

        SortedMap classPathUrls = new TreeMap();

        SortedMap surefireClassPathUrls = new TreeMap();
        SortedMap reportsMap = new TreeMap();

        boolean isTrimStackTrace = false;
        File reportsDirectory = null;

        String testNgVersion = null;
        String testNgClassifier = null;
        String testForFork = null;
        String requestedTest = null;
        File testSuiteDefinitionTestSourceDirectory = null;
        Object[] testSuiteXmlFiles = null;
        String providerConfiguration = null;
        SortedMap includes = new TreeMap();
        SortedMap excludes = new TreeMap();
        File testClassesDirectory = null;

        for ( Enumeration e = properties.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if ( name.startsWith( REPORT_PROPERTY_PREFIX ) && !isTypeHolderProperty( name ) )
            {
                String className = properties.getProperty( name );
                reportsMap.put( name, className );
            }
            else if ( name.startsWith( INCLUDES_PROPERTY_PREFIX ) && !isTypeHolderProperty( name ) )
            {
                String className = properties.getProperty( name );
                includes.put( name, className );
            }
            else if ( name.startsWith( EXCLUDES_PROPERTY_PREFIX ) && !isTypeHolderProperty( name ) )
            {
                String className = properties.getProperty( name );
                excludes.put( name, className );
            }
            else if ( name.startsWith( "classPathUrl." ) )
            {
                classPathUrls.put( Integer.valueOf( name.substring( name.indexOf( '.' ) + 1 ) ),
                                   properties.getProperty( name ) );
            }
            else if ( name.startsWith( "surefireClassPathUrl." ) )
            {
                surefireClassPathUrls.put( Integer.valueOf( name.substring( name.indexOf( '.' ) + 1 ) ),
                                           properties.getProperty( name ) );
            }
            else if ( "childDelegation".equals( name ) )
            {
                childDelegation = Boolean.valueOf( properties.getProperty( "childDelegation" ) ).booleanValue();
            }
            else if ( "enableAssertions".equals( name ) )
            {
                enableAssertions = Boolean.valueOf( properties.getProperty( "enableAssertions" ) ).booleanValue();
            }
            else if ( "useSystemClassLoader".equals( name ) )
            {
                useSystemClassLoader =
                    Boolean.valueOf( properties.getProperty( "useSystemClassLoader" ) ).booleanValue();
            }
            else if ( "useManifestOnlyJar".equals( name ) )
            {
                useManifestOnlyJar = Boolean.valueOf( properties.getProperty( "useManifestOnlyJar" ) ).booleanValue();
            }
            else if ( "failIfNoTests".equals( name ) )
            {
                failIfNotests = Boolean.valueOf( properties.getProperty( "failIfNoTests" ) ).booleanValue();
            }
            else if ( "isTrimStackTrace".equals( name ) )
            {
                failIfNotests = Boolean.valueOf( properties.getProperty( "isTrimStackTrace" ) ).booleanValue();
            }
            else if ( "reportsDirectory".equals( name ) )
            {
                reportsDirectory = new File( properties.getProperty( "reportsDirectory" ) );
            }
            else if ( "testNgVersion".equals( name ) )
            {
                testNgVersion = properties.getProperty( "testNgVersion" );
            }
            else if ( "testNgClassifier".equals( name ) )
            {
                testNgClassifier = properties.getProperty( "testNgClassifier" );
            }
            else if ( "testSuiteDefinitionTest".equals( name ) )
            {
                testForFork = properties.getProperty( "testSuiteDefinitionTest" );
            }
            else if ( "requestedTest".equals( name ) )
            {
                requestedTest = properties.getProperty( "requestedTest" );
            }
            else if ( "testSuiteDefinitionTestSourceDirectory".equals( name ) )
            {
                testSuiteDefinitionTestSourceDirectory =
                    (File) getParamValue( properties.getProperty( "testSuiteDefinitionTestSourceDirectory" ),
                                          File.class.getName() );
            }
            else if ( "testClassesDirectory".equals( name ) )
            {
                testClassesDirectory =
                    (File) getParamValue( properties.getProperty( "testClassesDirectory" ), File.class.getName() );
            }
            else if ( "testSuiteXmlFiles".equals( name ) )
            {
                testSuiteXmlFiles = constructParamObjects( properties.getProperty( "testSuiteXmlFiles" ), File.class );
            }
            else if ( "providerConfiguration".equals( name ) )
            {
                providerConfiguration = properties.getProperty( "providerConfiguration" );
            }
        }

        dirScannerParams = new DirectoryScannerParameters( testClassesDirectory, new ArrayList( includes.values() ),
                                                           new ArrayList( excludes.values() ),
                                                           Boolean.valueOf( failIfNotests ) );

        TestArtifactInfo testNg = new TestArtifactInfo( testNgVersion, testNgClassifier );
        TestSuiteDefinition testSuiteDefinition =
            new TestSuiteDefinition( testSuiteXmlFiles, testForFork, testSuiteDefinitionTestSourceDirectory,
                                     requestedTest );

        ClassLoaderConfiguration forkConfiguration =
            new ClassLoaderConfiguration( useSystemClassLoader, useManifestOnlyJar );

        ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( classPathUrls, surefireClassPathUrls, enableAssertions, childDelegation );

        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reportsDirectory, Boolean.valueOf( isTrimStackTrace ) );

        ProviderConfiguration providerConfigurationObj = new ProviderConfiguration( providerConfiguration );
        List reports = new ArrayList( reportsMap.values() );
        return new BooterConfiguration( forkConfiguration, classpathConfiguration, reports, dirScannerParams,
                                        failIfNotests, properties, reporterConfiguration, testNg, testSuiteDefinition,
                                        providerConfigurationObj );
    }

    private boolean isTypeHolderProperty( String name )
    {
        return name.endsWith( PARAMS_SUFIX ) || name.endsWith( TYPES_SUFIX );
    }

    void writePropertiesFile( File file, String name, Properties properties )
        throws IOException
    {
        FileOutputStream out = new FileOutputStream( file );

        try
        {
            properties.store( out, name );
        }
        finally
        {
            close( out );
        }
    }

    private static List processStringList( String stringList )
    {
        String sl = stringList;

        if ( sl.startsWith( "[" ) && sl.endsWith( "]" ) )
        {
            sl = sl.substring( 1, sl.length() - 1 );
        }

        List list = new ArrayList();

        String[] stringArray = StringUtils.split( sl, "," );

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

    /**
     * From IOUtils
     * Closes the output stream. The output stream can be null and any IOException's will be swallowed.
     *
     * @param outputStream The stream to close.
     */
    public static void close( OutputStream outputStream )
    {
        if ( outputStream == null )
        {
            return;
        }

        try
        {
            outputStream.close();
        }
        catch ( IOException ex )
        {
            // ignore
        }
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
