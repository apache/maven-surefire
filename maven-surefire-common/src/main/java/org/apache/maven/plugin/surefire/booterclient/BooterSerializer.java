package org.apache.maven.plugin.surefire.booterclient;

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

import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.codehaus.plexus.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
 * @author Brett Porter
 * @author Dan Fabulich
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class BooterSerializer
{
    private final ForkConfiguration forkConfiguration;

    private final PropertiesWrapper properties;

    public BooterSerializer( ForkConfiguration forkConfiguration, Properties properties )
    {
        this.forkConfiguration = forkConfiguration;
        this.properties = new PropertiesWrapper( properties );
    }


    public File serialize( ProviderConfiguration booterConfiguration, StartupConfiguration providerConfiguration,
                           Object testSet )
        throws IOException
    {
        List params = new ArrayList();
        params.add(
            new Object[]{ BooterConstants.DIRSCANNER_OPTIONS, booterConfiguration.getDirScannerParamsArray() } );
        addPropertiesForTypeHolder( params, properties.getProperties(), BooterConstants.DIRSCANNER_PROPERTY_PREFIX );

        providerConfiguration.getClasspathConfiguration().setForkProperties( properties.getProperties() );

        TestArtifactInfo testNg = booterConfiguration.getTestArtifact();
        if ( testNg != null )
        {
            properties.setProperty( BooterConstants.TESTARTIFACT_VERSION, testNg.getVersion() );
            properties.setProperty( BooterConstants.TESTARTIFACT_CLASSIFIER, testNg.getClassifier() );
        }

        properties.setProperty( BooterConstants.FORKTESTSET, getTypeEncoded( testSet ) );
        TestRequest testSuiteDefinition = booterConfiguration.getTestSuiteDefinition();
        if ( testSuiteDefinition != null )
        {
            properties.setProperty( BooterConstants.SOURCE_DIRECTORY, testSuiteDefinition.getTestSourceDirectory() );
            properties.addList( testSuiteDefinition.getSuiteXmlFiles(), BooterConstants.TEST_SUITE_XML_FILES );
            properties.setProperty( BooterConstants.REQUESTEDTEST, testSuiteDefinition.getRequestedTest() );
            properties.setProperty( BooterConstants.REQUESTEDTESTMETHOD, testSuiteDefinition.getRequestedTestMethod() );
        }

        DirectoryScannerParameters directoryScannerParameters = booterConfiguration.getDirScannerParams();
        if ( directoryScannerParameters != null )
        {
            properties.setProperty( BooterConstants.FAILIFNOTESTS,
                                    String.valueOf( directoryScannerParameters.isFailIfNoTests() ) );
            properties.addList( directoryScannerParameters.getIncludes(), BooterConstants.INCLUDES_PROPERTY_PREFIX );
            properties.addList( directoryScannerParameters.getExcludes(), BooterConstants.EXCLUDES_PROPERTY_PREFIX );
            properties.setProperty( BooterConstants.TEST_CLASSES_DIRECTORY,
                                    directoryScannerParameters.getTestClassesDirectory() );
            properties.setProperty( BooterConstants.RUN_ORDER, directoryScannerParameters.getRunOrder() );
        }

        ReporterConfiguration reporterConfiguration = booterConfiguration.getReporterConfiguration();
        addList( reporterConfiguration.getReports(), properties.getProperties(),
                 BooterConstants.REPORT_PROPERTY_PREFIX );

        Boolean rep = reporterConfiguration.isTrimStackTrace();
        properties.setProperty( BooterConstants.ISTRIMSTACKTRACE, rep );
        properties.setProperty( BooterConstants.REPORTSDIRECTORY, reporterConfiguration.getReportsDirectory() );
        properties.setProperty( BooterConstants.FORKTIMEOUT, reporterConfiguration.getForkTimeout() );
        ClassLoaderConfiguration classLoaderConfiguration = providerConfiguration.getClassLoaderConfiguration();
        properties.setProperty( BooterConstants.USESYSTEMCLASSLOADER,
                                String.valueOf( classLoaderConfiguration.isUseSystemClassLoader() ) );
        properties.setProperty( BooterConstants.USEMANIFESTONLYJAR,
                                String.valueOf( classLoaderConfiguration.isUseManifestOnlyJar() ) );
        properties.setProperty( BooterConstants.FAILIFNOTESTS,
                                String.valueOf( booterConfiguration.isFailIfNoTests() ) );
        properties.setProperty( BooterConstants.PROVIDER_CONFIGURATION, providerConfiguration.getProviderClassName() );

        return SystemPropertyManager.writePropertiesFile( properties.getProperties(),
                                                          forkConfiguration.getTempDirectory(), "surefire",
                                                          forkConfiguration.isDebug() );
    }


    private String getTypeEncoded( Object value )
    {
        if ( value == null )
        {
            return null;
        }
        String valueToUse;
        if ( value instanceof Class )
        {
            valueToUse = ( (Class) value ).getName();
        }
        else
        {
            valueToUse = value.toString();
        }
        return value.getClass().getName() + "|" + valueToUse;
    }

    private void addPropertiesForTypeHolder( List typeHolderList, Properties properties, String propertyPrefix )
    {
        for ( int i = 0; i < typeHolderList.size(); i++ )
        {
            Object[] report = (Object[]) typeHolderList.get( i );

            String className = (String) report[0];
            Object[] params = (Object[]) report[1];

            properties.setProperty( propertyPrefix + i, className );

            if ( params != null )
            {
                String paramProperty = getValues( params );
                String typeProperty = getTypes( params );
                properties.setProperty( propertyPrefix + i + BooterConstants.PARAMS_SUFIX, paramProperty );
                properties.setProperty( propertyPrefix + i + BooterConstants.TYPES_SUFIX, typeProperty );
            }
        }
    }

    private String getValues( Object[] params )
    {
        StringBuffer result = new StringBuffer();
        if ( params != null && params.length > 0 )
        {
            result.append( convert( params[0] ) );
            for ( int j = 1; j < params.length; j++ )
            {
                result.append( "|" );
                if ( params[j] != null )
                {
                    result.append( convert( params[j] ) );
                }
            }
        }
        return result.toString();
    }

    private String getTypes( Object[] params )
    {
        StringBuffer result = new StringBuffer();
        if ( params != null && params.length > 0 )
        {
            result.append( params[0].getClass().getName() );
            for ( int j = 1; j < params.length; j++ )
            {
                result.append( "|" );
                if ( params[j] != null )
                {
                    result.append( params[j].getClass().getName() );
                }
            }
        }
        return result.toString();
    }

    private static String convert( Object param )
    {
        if ( param instanceof File[] )
        {
            File[] files = (File[]) param;
            return "[" + StringUtils.join( files, "," ) + "]";
        }
        else if ( param instanceof Properties )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try
            {
                ( (Properties) param ).store( baos, "" );
                return new String( baos.toByteArray(), "8859_1" );
            }
            catch ( Exception e )
            {
                throw new NestedRuntimeException( "bug in property conversion", e );
            }
        }
        else
        {
            return param.toString();
        }
    }

    private void addList( List items, Properties properties, String propertyPrefix )
    {
        for ( int i = 0; i < items.size(); i++ )
        {
            Object item = items.get( i );
            if ( item == null )
            {
                throw new NullPointerException( propertyPrefix + i + " has null value" );
            }
            properties.setProperty( propertyPrefix + i, item.toString() );
        }
    }

}
