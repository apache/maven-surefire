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

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Represents the surefire configuration that crosses booter forks into other vms and classloaders.
 * <p/>
 * Also knows how to serialize and deserialize itself to disk, though that might some day be externalized.
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

    private static final String TEST_SUITE_PROPERTY_PREFIX = "testSuite.";

    private static final String REPORT_PROPERTY_PREFIX = "report.";

    private static final String PARAMS_SUFIX = ".params";

    private static final String TYPES_SUFIX = ".types";

    private final ForkConfiguration forkConfiguration;

    private final ClasspathConfiguration classpathConfiguration;

    private final List reports = new ArrayList();

    private final List testSuites = new ArrayList();

    private boolean failIfNoTests = false;

    private boolean redirectTestOutputToFile = false;

    private Properties properties;

    /**
     * This field is set to true if it's running from main. It's used to help decide what classloader to use.
     */
    private boolean forked;

    public BooterConfiguration( ForkConfiguration forkConfiguration, ClasspathConfiguration classpathConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
        this.classpathConfiguration = classpathConfiguration;
    }

    /*
    * Reads the config from the supplied stream. Closes the stream.
     */
    BooterConfiguration( InputStream config )
        throws IOException
    {
        this.forked = true;
        properties = loadProperties( config );
        boolean enableAssertions = false;
        boolean childDelegation = true;

        SortedMap classPathUrls = new TreeMap();

        SortedMap surefireClassPathUrls = new TreeMap();

        Collection booterClassPathUrl = new ArrayList();

        forkConfiguration = new ForkConfiguration();
        forkConfiguration.setForkMode( "never" );

        for ( Enumeration e = properties.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if ( name.startsWith( REPORT_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) &&
                !name.endsWith( TYPES_SUFIX ) )
            {
                String className = properties.getProperty( name );

                String params = properties.getProperty( name + PARAMS_SUFIX );
                String types = properties.getProperty( name + TYPES_SUFIX );
                addReport( className, constructParamObjects( params, types ) );
            }
            else if ( name.startsWith( TEST_SUITE_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) &&
                !name.endsWith( TYPES_SUFIX ) )
            {
                String className = properties.getProperty( name );

                String params = properties.getProperty( name + PARAMS_SUFIX );
                String types = properties.getProperty( name + TYPES_SUFIX );
                addTestSuite( className, constructParamObjects( params, types ) );
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
            else if ( name.startsWith( "surefireBootClassPathUrl." ) )
            {
                booterClassPathUrl.add( properties.getProperty( name ) );
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
                boolean value = Boolean.valueOf( properties.getProperty( "useSystemClassLoader" ) ).booleanValue();
                forkConfiguration.setUseSystemClassLoader( value );
            }
            else if ( "useManifestOnlyJar".equals( name ) )
            {
                boolean value = Boolean.valueOf( properties.getProperty( "useManifestOnlyJar" ) ).booleanValue();
                forkConfiguration.setUseManifestOnlyJar( value );
            }
            else if ( "failIfNoTests".equals( name ) )
            {
                boolean value = Boolean.valueOf( properties.getProperty( "failIfNoTests" ) ).booleanValue();
                setFailIfNoTests( value );
            }
        }

        classpathConfiguration =
            new ClasspathConfiguration( classPathUrls, surefireClassPathUrls, booterClassPathUrl, enableAssertions,
                                        childDelegation );
    }

    ClasspathConfiguration getClasspathConfiguration()
    {
        return classpathConfiguration;
    }

    void setForkProperties( List testSuites, Properties properties )
    {
        addPropertiesForTypeHolder( reports, properties, REPORT_PROPERTY_PREFIX );
        addPropertiesForTypeHolder( testSuites, properties, TEST_SUITE_PROPERTY_PREFIX );

        classpathConfiguration.setForkProperties( properties );

        properties.setProperty( "useSystemClassLoader", String.valueOf( useSystemClassLoader() ) );
        properties.setProperty( "useManifestOnlyJar",
                                String.valueOf( forkConfiguration.isManifestOnlyJarRequestedAndUsable() ) );
        properties.setProperty( "failIfNoTests", String.valueOf( failIfNoTests ) );
    }

    File writePropertiesFile( String name, Properties properties )
        throws IOException
    {
        File file = File.createTempFile( name, "tmp", forkConfiguration.getTempDirectory() );
        if ( !forkConfiguration.isDebug() )
        {
            file.deleteOnExit();
        }

        writePropertiesFile( file, name, properties );

        return file;
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
            IOUtil.close( out );
        }
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
                String paramProperty = convert( params[0] );
                String typeProperty = params[0].getClass().getName();
                for ( int j = 1; j < params.length; j++ )
                {
                    paramProperty += "|";
                    typeProperty += "|";
                    if ( params[j] != null )
                    {
                        paramProperty += convert( params[j] );
                        typeProperty += params[j].getClass().getName();
                    }
                }
                properties.setProperty( propertyPrefix + i + PARAMS_SUFIX, paramProperty );
                properties.setProperty( propertyPrefix + i + TYPES_SUFIX, typeProperty );
            }
        }
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
                throw new RuntimeException( "bug in property conversion", e );
            }
        }
        else
        {
            return param.toString();
        }
    }

    public boolean useSystemClassLoader()
    {
        return forkConfiguration.isUseSystemClassLoader() && ( forked || forkConfiguration.isForking() );
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

    /*
    Loads the properties, closes the stream
     */

    private static Properties loadProperties( InputStream inStream )
        throws IOException
    {
        Properties p = new Properties();

        try
        {
            p.load( inStream );
        }
        finally
        {
            IOUtil.close( inStream );
        }

        return p;
    }

    private static Object[] constructParamObjects( String paramProperty, String typeProperty )
    {
        Object[] paramObjects = null;
        if ( paramProperty != null )
        {
            // bit of a glitch that it need sto be done twice to do an odd number of vertical bars (eg |||, |||||).
            String[] params = StringUtils.split(
                StringUtils.replace( StringUtils.replace( paramProperty, "||", "| |" ), "||", "| |" ), "|" );
            String[] types =
                StringUtils.split( StringUtils.replace( StringUtils.replace( typeProperty, "||", "| |" ), "||", "| |" ),
                                   "|" );

            paramObjects = new Object[params.length];

            for ( int i = 0; i < types.length; i++ )
            {
                if ( types[i].trim().length() == 0 )
                {
                    params[i] = null;
                }
                else if ( types[i].equals( String.class.getName() ) )
                {
                    paramObjects[i] = params[i];
                }
                else if ( types[i].equals( File.class.getName() ) )
                {
                    paramObjects[i] = new File( params[i] );
                }
                else if ( types[i].equals( File[].class.getName() ) )
                {
                    List stringList = processStringList( params[i] );
                    File[] fileList = new File[stringList.size()];
                    for ( int j = 0; j < stringList.size(); j++ )
                    {
                        fileList[j] = new File( (String) stringList.get( j ) );
                    }
                    paramObjects[i] = fileList;
                }
                else if ( types[i].equals( ArrayList.class.getName() ) )
                {
                    paramObjects[i] = processStringList( params[i] );
                }
                else if ( types[i].equals( Boolean.class.getName() ) )
                {
                    paramObjects[i] = Boolean.valueOf( params[i] );
                }
                else if ( types[i].equals( Integer.class.getName() ) )
                {
                    paramObjects[i] = Integer.valueOf( params[i] );
                }
                else if ( types[i].equals( Properties.class.getName() ) )
                {
                    final Properties result = new Properties();
                    final String value = params[i];
                    try
                    {
                        ByteArrayInputStream bais = new ByteArrayInputStream( value.getBytes( "8859_1" ) );
                        result.load( bais );
                    }
                    catch ( Exception e )
                    {
                        throw new RuntimeException( "bug in property conversion", e );
                    }
                    paramObjects[i] = result;
                }
                else
                {
                    // TODO: could attempt to construct with a String constructor if needed
                    throw new IllegalArgumentException( "Unknown parameter type: " + types[i] );
                }
            }
        }
        return paramObjects;
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
        return testSuites;
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

    public void addTestSuite( String suiteClassName, Object[] constructorParams )
    {
        testSuites.add( new Object[]{ suiteClassName, constructorParams } );
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
}

