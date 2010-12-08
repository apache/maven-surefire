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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        DirectoryScannerParameters dirScannerParams;
        boolean failIfNotests = false;  // todo; check this out.

        SortedMap reportsMap = new TreeMap();

        boolean isTrimStackTrace = false;
        File reportsDirectory = null;

        String testNgVersion = null;
        String testNgClassifier = null;
        Object testForFork = null;
        String requestedTest = null;
        File sourceDirectory = null;
        Object[] testSuiteXmlFiles = null;
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
            else if ( FAILIFNOTESTS.equals( name ) )
            {
                failIfNotests = properties.getBooleanProperty( FAILIFNOTESTS );
            }
            else if ( ISTRIMSTACKTRACE.equals( name ) )
            {
                failIfNotests = properties.getBooleanProperty( ISTRIMSTACKTRACE );
            }
            else if ( REPORTSDIRECTORY.equals( name ) )
            {
                reportsDirectory = new File( properties.getProperty( REPORTSDIRECTORY ) );
            }
            else if ( TESTARTIFACT_VERSION.equals( name ) )
            {
                testNgVersion = properties.getProperty( TESTARTIFACT_VERSION );
            }
            else if ( TESTARTIFACT_CLASSIFIER.equals( name ) )
            {
                testNgClassifier = properties.getProperty( TESTARTIFACT_CLASSIFIER );
            }
            else if ( FORKTESTSET.equals( name ) )
            {
                testForFork = getTypeDecoded( properties.getProperty( FORKTESTSET ) );
            }
            else if ( REQUESTEDTEST.equals( name ) )
            {
                requestedTest = properties.getProperty( REQUESTEDTEST );
            }
            else if ( SOURCE_DIRECTORY.equals( name ) )
            {
                sourceDirectory =
                    (File) getParamValue( properties.getProperty( SOURCE_DIRECTORY ), File.class.getName() );
            }
            else if ( TEST_CLASSES_DIRECTORY.equals( name ) )
            {
                testClassesDirectory =
                    (File) getParamValue( properties.getProperty( TEST_CLASSES_DIRECTORY ), File.class.getName() );
            }
            else if ( TEST_SUITE_XML_FILES.equals( name ) )
            {
                testSuiteXmlFiles = constructParamObjects( properties.getProperty( TEST_SUITE_XML_FILES ), File.class );
            }
        }

        dirScannerParams = new DirectoryScannerParameters( testClassesDirectory, new ArrayList( includes.values() ),
                                                           new ArrayList( excludes.values() ),
                                                           valueOf( failIfNotests ) );

        TestArtifactInfo testNg = new TestArtifactInfo( testNgVersion, testNgClassifier );
        TestRequest testSuiteDefinition = new TestRequest( testSuiteXmlFiles, sourceDirectory, requestedTest );

        List reports = new ArrayList( reportsMap.values() );

        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reports, reportsDirectory, valueOf( isTrimStackTrace ) );

        return new ProviderConfiguration( dirScannerParams, failIfNotests, reporterConfiguration, testNg,
                                          testSuiteDefinition, properties.getProperties(), testForFork );
    }

    public StartupConfiguration getProviderConfiguration()
        throws IOException
    {
        boolean enableAssertions = false;
        boolean childDelegation = true;
        boolean useSystemClassLoader = false; // todo check default value
        boolean useManifestOnlyJar = false; // todo check default value

        SortedMap classPathUrls = new TreeMap();

        SortedMap surefireClassPathUrls = new TreeMap();

        String providerConfiguration = null;

        for ( Enumeration e = properties.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if ( name.startsWith( CLASSPATH_URL ) )
            {
                classPathUrls.put( Integer.valueOf( name.substring( name.indexOf( '.' ) + 1 ) ),
                                   properties.getProperty( name ) );
            }
            else if ( name.startsWith( SUREFIRE_CLASSPATHURL ) )
            {
                surefireClassPathUrls.put( Integer.valueOf( name.substring( name.indexOf( '.' ) + 1 ) ),
                                           properties.getProperty( name ) );
            }
            else if ( CHILD_DELEGATION.equals( name ) )
            {
                childDelegation = properties.getBooleanProperty( CHILD_DELEGATION );
            }
            else if ( ENABLE_ASSERTIONS.equals( name ) )
            {
                enableAssertions = properties.getBooleanProperty( ENABLE_ASSERTIONS );
            }
            else if ( USESYSTEMCLASSLOADER.equals( name ) )
            {
                useSystemClassLoader = properties.getBooleanProperty( USESYSTEMCLASSLOADER );
            }
            else if ( USEMANIFESTONLYJAR.equals( name ) )
            {
                useManifestOnlyJar = properties.getBooleanProperty( USEMANIFESTONLYJAR );
            }
            else if ( PROVIDER_CONFIGURATION.equals( name ) )
            {
                providerConfiguration = properties.getProperty( PROVIDER_CONFIGURATION );
            }
        }

        ClassLoaderConfiguration classLoaderConfiguration =
            new ClassLoaderConfiguration( useSystemClassLoader, useManifestOnlyJar );

        ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( classPathUrls, surefireClassPathUrls, enableAssertions, childDelegation );

        return StartupConfiguration.inForkedVm( providerConfiguration, classpathConfiguration,
                                                classLoaderConfiguration );
    }

    private Boolean valueOf( boolean aBoolean )
    {  // jdk1.3 compat
        return aBoolean ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean isTypeHolderProperty( String name )
    {
        return name.endsWith( PARAMS_SUFIX ) || name.endsWith( TYPES_SUFIX );
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

    private static Object getTypeDecoded( String typeEncoded )
    {
        int typeSep = typeEncoded.indexOf( "|" );
        String type = typeEncoded.substring( 0, typeSep );
        String value = typeEncoded.substring( typeSep + 1 );
        return getParamValue( value, type );
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
