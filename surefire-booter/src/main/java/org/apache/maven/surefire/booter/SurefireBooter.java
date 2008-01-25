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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.booter.output.FileOutputConsumerProxy;
import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.booter.output.OutputConsumer;
import org.apache.maven.surefire.booter.output.StandardOutputConsumer;
import org.apache.maven.surefire.booter.output.SupressFooterOutputConsumerProxy;
import org.apache.maven.surefire.booter.output.SupressHeaderOutputConsumerProxy;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.UrlUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @version $Id$
 */
public class SurefireBooter
{
    private static final String TEST_SUITE_PROPERTY_PREFIX = "testSuite.";
    private static final String REPORT_PROPERTY_PREFIX = "report.";
    private static final String PARAMS_SUFIX = ".params";
    private static final String TYPES_SUFIX = ".types";

    private List reports = new ArrayList();

    private List classPathUrls = new ArrayList();

    private List surefireClassPathUrls = new ArrayList();

    private List surefireBootClassPathUrls = new ArrayList();

    private List testSuites = new ArrayList();
    
    private boolean failIfNoTests = false;
    
    private int forkedProcessTimeoutInSeconds = 0;

    private boolean redirectTestOutputToFile = false;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private ForkConfiguration forkConfiguration;

    public static final int TESTS_SUCCEEDED_EXIT_CODE = 0;

    public static final int TESTS_FAILED_EXIT_CODE = 255;
    
    public static final int NO_TESTS_EXIT_CODE = 254;

    private static Method assertionStatusMethod;

    /**
     * @deprecated because the IsolatedClassLoader is really isolated - no parent.
     */
    private boolean childDelegation = true;

    private File reportsDirectory;

    /**
     * This field is set to true if it's running from main. It's used to help decide what classloader to use.
     */
    private final boolean isForked;

    /**
     * Whether to enable assertions or not (can be affected by the fork arguments, and the ability to do so based on the
     * JVM).
     */
    private boolean enableAssertions;

    static
    {
        try
        {
            assertionStatusMethod =
                ClassLoader.class.getMethod( "setDefaultAssertionStatus", new Class[] { boolean.class } );
        }
        catch ( NoSuchMethodException e )
        {
            assertionStatusMethod = null;
        }
    }

    public SurefireBooter()
    {
        isForked = false;
    }

    private SurefireBooter( boolean isForked )
    {
        this.isForked = isForked;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void addReport( String report )
    {
        addReport( report, null );
    }

    public void addReport( String report, Object[] constructorParams )
    {
        reports.add( new Object[] { report, constructorParams } );
    }

    public void addTestSuite( String suiteClassName, Object[] constructorParams )
    {
        testSuites.add( new Object[] { suiteClassName, constructorParams } );
    }

    public void addClassPathUrl( String path )
    {
        if ( !classPathUrls.contains( path ) )
        {
            classPathUrls.add( path );
        }
    }

    public void addSurefireClassPathUrl( String path )
    {
        if ( !surefireClassPathUrls.contains( path ) )
        {
            surefireClassPathUrls.add( path );
        }
    }

    public void addSurefireBootClassPathUrl( String path )
    {
        if ( !surefireBootClassPathUrls.contains( path ) )
        {
            surefireBootClassPathUrls.add( path );
        }
    }

    /**
     * Setting this to true will cause a failure if there are no tests to run
     *
     * @param redirectTestOutputToFile
     */
    public void setFailIfNoTests( boolean failIfNoTests )
    {
        this.failIfNoTests = failIfNoTests;
    }
    
    /**
     * When forking, setting this to true will make the test output to be saved in a file instead of showing it on the
     * standard output
     *
     * @param redirectTestOutputToFile
     */
    public void setRedirectTestOutputToFile( boolean redirectTestOutputToFile )
    {
        this.redirectTestOutputToFile = redirectTestOutputToFile;
    }

    /**
     * Set the directory where reports will be saved
     *
     * @param reportsDirectory the directory
     */
    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    /**
     * Get the directory where reports will be saved
     */
    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public void setForkConfiguration( ForkConfiguration forkConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
    }
    
    public boolean isForking() {
        return forkConfiguration.isForking();
    }

    public int run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        int result;

        if ( ForkConfiguration.FORK_NEVER.equals( forkConfiguration.getForkMode() ) )
        {
            result = runSuitesInProcess();
        }
        else if ( ForkConfiguration.FORK_ONCE.equals( forkConfiguration.getForkMode() ) )
        {
            result = runSuitesForkOnce();
        }
        else if ( ForkConfiguration.FORK_ALWAYS.equals( forkConfiguration.getForkMode() ) )
        {
            result = runSuitesForkPerTestSet();
        }
        else
        {
            throw new SurefireExecutionException( "Unknown forkmode: " + forkConfiguration.getForkMode(), null );
        }
        return result;
    }

    private int runSuitesInProcess( String testSet, Properties results )
        throws SurefireExecutionException
    {
        if ( testSuites.size() != 1 )
        {
            throw new IllegalArgumentException( "Cannot only specify testSet for single test suites" );
        }

        // TODO: replace with plexus

        // noinspection CatchGenericClass,OverlyBroadCatchBlock
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            ClassLoader testsClassLoader =
                useSystemClassLoader() ? ClassLoader.getSystemClassLoader() : createClassLoader( classPathUrls, null,
                                                                                                 childDelegation );

            // TODO: assertions = true shouldn't be required for this CL if we had proper separation (see TestNG)
            ClassLoader surefireClassLoader = createClassLoader( surefireClassPathUrls, testsClassLoader );

            Class surefireClass = surefireClassLoader.loadClass( Surefire.class.getName() );

            Object surefire = surefireClass.newInstance();

            Method run =
                surefireClass.getMethod( "run", new Class[] { List.class, Object[].class, String.class,
                    ClassLoader.class, ClassLoader.class, Properties.class, Boolean.class } );

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            Integer result =
                (Integer) run.invoke( surefire, new Object[] { reports, testSuites.get( 0 ), testSet,
                    surefireClassLoader, testsClassLoader, results, new Boolean( failIfNoTests ) } );

            return result.intValue();
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireExecutionException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( Exception e )
        {
            throw new SurefireExecutionException( "Unable to instantiate and execute Surefire", e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    private int runSuitesInProcess()
        throws SurefireExecutionException
    {
        // TODO: replace with plexus

        // noinspection CatchGenericClass,OverlyBroadCatchBlock
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            // The test classloader must be constructed first to avoid issues with commons-logging until we properly
            // separate the TestNG classloader
            ClassLoader testsClassLoader = useSystemClassLoader() ? getClass().getClassLoader() // ClassLoader.getSystemClassLoader()
                            : createClassLoader( classPathUrls, null, childDelegation );

            recordTestClassPath();
            
            ClassLoader surefireClassLoader = createClassLoader( surefireClassPathUrls, testsClassLoader );

            Class surefireClass = surefireClassLoader.loadClass( Surefire.class.getName() );

            Object surefire = surefireClass.newInstance();

            Method run =
                surefireClass.getMethod( "run", new Class[] { List.class, List.class, ClassLoader.class,
                    ClassLoader.class, Boolean.class } );

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            Integer result =
                (Integer) run.invoke( surefire, new Object[] { reports, testSuites, surefireClassLoader,
                    testsClassLoader, new Boolean( failIfNoTests ) } );

            return result.intValue();
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireExecutionException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( Exception e )
        {
            throw new SurefireExecutionException( "Unable to instantiate and execute Surefire", e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    
    
    private void recordTestClassPath()
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < classPathUrls.size(); i++) {
            sb.append( classPathUrls.get( i ) ).append( File.pathSeparatorChar );
        }
        System.setProperty( "surefire.test.class.path", sb.toString() );
    }
    
    private int runSuitesForkOnce()
        throws SurefireBooterForkException
    {
        return forkSuites( testSuites, true, true );
    }

    private int runSuitesForkPerTestSet()
        throws SurefireBooterForkException
    {
        ClassLoader testsClassLoader;
        ClassLoader surefireClassLoader;
        try
        {
            testsClassLoader = createClassLoader( classPathUrls, null, false );
            // TODO: assertions = true shouldn't be required if we had proper separation (see TestNG)
            surefireClassLoader = createClassLoader( surefireClassPathUrls, testsClassLoader, false );
        }
        catch ( MalformedURLException e )
        {
            throw new SurefireBooterForkException( "Unable to create classloader to find test suites", e );
        }

        int globalResult = 0;

        boolean showHeading = true;
        Properties properties = new Properties();
        for ( Iterator i = testSuites.iterator(); i.hasNext(); )
        {
            Object[] testSuite = (Object[]) i.next();

            Map testSets = getTestSets( testSuite, testsClassLoader, surefireClassLoader );

            for ( Iterator j = testSets.keySet().iterator(); j.hasNext(); )
            {
                String testSet = (String) j.next();
                boolean showFooter = !j.hasNext() && !i.hasNext();
                int result = forkSuite( testSuite, testSet, showHeading, showFooter, properties );
                if ( result > globalResult )
                {
                    globalResult = result;
                }
                showHeading = false;
            }
        }

        return globalResult;
    }

    private Map getTestSets( Object[] testSuite, ClassLoader testsClassLoader, ClassLoader surefireClassLoader )
        throws SurefireBooterForkException
    {
        String className = (String) testSuite[0];

        Object[] params = (Object[]) testSuite[1];

        Object suite;
        try
        {
            suite = Surefire.instantiateObject( className, params, surefireClassLoader );
        }
        catch ( TestSetFailedException e )
        {
            throw new SurefireBooterForkException( e.getMessage(), e.getCause() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SurefireBooterForkException( "Unable to find class for test suite '" + className + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new SurefireBooterForkException( "Unable to find appropriate constructor for test suite '" +
                className + "': " + e.getMessage(), e );
        }

        Map testSets;
        try
        {
            Method m = suite.getClass().getMethod( "locateTestSets", new Class[] { ClassLoader.class } );

            testSets = (Map) m.invoke( suite, new Object[] { testsClassLoader } );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireBooterForkException( "Error obtaining test sets", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new SurefireBooterForkException( "Error obtaining test sets", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireBooterForkException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        return testSets;
    }

    private int forkSuites( List testSuites, boolean showHeading, boolean showFooter )
        throws SurefireBooterForkException
    {
        Properties properties = new Properties();

        setForkProperties( testSuites, properties );

        return fork( properties, showHeading, showFooter );
    }

    private int forkSuite( Object[] testSuite, String testSet, boolean showHeading, boolean showFooter,
                               Properties properties )
        throws SurefireBooterForkException
    {
        setForkProperties( Collections.singletonList( testSuite ), properties );

        properties.setProperty( "testSet", testSet );

        return fork( properties, showHeading, showFooter );
    }

    private void setForkProperties( List testSuites, Properties properties )
    {
        addPropertiesForTypeHolder( reports, properties, REPORT_PROPERTY_PREFIX );
        addPropertiesForTypeHolder( testSuites, properties, TEST_SUITE_PROPERTY_PREFIX );

        for ( int i = 0; i < classPathUrls.size(); i++ )
        {
            String url = (String) classPathUrls.get( i );
            properties.setProperty( "classPathUrl." + i, url );
        }

        for ( int i = 0; i < surefireClassPathUrls.size(); i++ )
        {
            String url = (String) surefireClassPathUrls.get( i );
            properties.setProperty( "surefireClassPathUrl." + i, url );
        }

        properties.setProperty( "childDelegation", String.valueOf( childDelegation ) );
        properties.setProperty( "enableAssertions", String.valueOf( enableAssertions ) );
        properties.setProperty( "useSystemClassLoader", String.valueOf( useSystemClassLoader() ) );
        properties.setProperty( "failIfNoTests", String.valueOf( failIfNoTests ) );
    }

    private File writePropertiesFile( String name, Properties properties )
        throws IOException
    {
        File file = File.createTempFile( name, "tmp" );
        if ( !forkConfiguration.isDebug() )
        {
            file.deleteOnExit();
        }

        writePropertiesFile( file, name, properties );

        return file;
    }

    private void writePropertiesFile( File file, String name, Properties properties )
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
            String s = "[";
            File[] f = (File[]) param;
            for ( int i = 0; i < f.length; i++ )
            {
                s += f[i];
                if ( i > 0 )
                {
                    s += ",";
                }
            }
            return s + "]";
        } else if ( param instanceof Properties ) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try
            {
                ( (Properties) param ).store( baos, "" );
                return new String( baos.toByteArray(), "8859_1" );
            }
            catch ( Exception e )
            {
                throw new RuntimeException ( "bug in property conversion", e );
            }
        }
        else
        {
            return param.toString();
        }
    }

    private final boolean useSystemClassLoader()
    {
        return forkConfiguration.isUseSystemClassLoader() && ( isForked || forkConfiguration.isForking() );
    }

    private int fork( Properties properties, boolean showHeading, boolean showFooter )
        throws SurefireBooterForkException
    {
        File surefireProperties;
        File systemProperties = null;
        try
        {
            surefireProperties = writePropertiesFile( "surefire", properties );
            if ( forkConfiguration.getSystemProperties() != null )
            {
                systemProperties = writePropertiesFile( "surefire", forkConfiguration.getSystemProperties() );
            }
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating properties files for forking", e );
        }

        List bootClasspath = new ArrayList( surefireBootClassPathUrls.size() + classPathUrls.size() );

        bootClasspath.addAll( surefireBootClassPathUrls );

        if ( useSystemClassLoader() )
        {
            bootClasspath.addAll( classPathUrls );
        }

        Commandline cli = forkConfiguration.createCommandLine( bootClasspath, useSystemClassLoader() );

        cli.createArg().setFile( surefireProperties );

        if ( systemProperties != null )
        {
            cli.createArg().setFile( systemProperties );
        }

        
        ForkingStreamConsumer out = getForkingStreamConsumer( showHeading, showFooter, redirectTestOutputToFile );

        StreamConsumer err;
        
        if ( redirectTestOutputToFile )
        {
            err = out;
        }
        else
        { 
            err = getForkingStreamConsumer( showHeading, showFooter, redirectTestOutputToFile );
        }

        if ( forkConfiguration.isDebug() )
        {
            System.out.println( "Forking command line: " + cli );
        }

        int returnCode;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err, forkedProcessTimeoutInSeconds );
        }
        catch ( CommandLineException e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e );
        }

        if ( redirectTestOutputToFile )
        {
            // ensure the FileOutputConsumerProxy flushes/closes the output file
            try
            {
                out.getOutputConsumer().testSetCompleted();
            }
            catch ( Exception e )
            {
                // the FileOutputConsumerProxy might throw an IllegalStateException but that's not of interest now
            }
        }

        if ( surefireProperties != null && surefireProperties.exists() )
        {
            FileInputStream inStream = null;
            try
            {
                inStream = new FileInputStream( surefireProperties );

                properties.load( inStream );
            }
            catch ( FileNotFoundException e )
            {
                throw new SurefireBooterForkException( "Unable to reload properties file from forked process", e );
            }
            catch ( IOException e )
            {
                throw new SurefireBooterForkException( "Unable to reload properties file from forked process", e );
            }
            finally
            {
                IOUtil.close( inStream );
            }
        }

        return returnCode;
    }

    private ClassLoader createClassLoader( List classPathUrls, ClassLoader parent )
        throws MalformedURLException
    {
        return createClassLoader( classPathUrls, parent, false );
    }

    private ClassLoader createClassLoader( List classPathUrls, ClassLoader parent, boolean childDelegation )
        throws MalformedURLException
    {
        List urls = new ArrayList();

        for ( Iterator i = classPathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url != null )
            {
                File f = new File( url );
                urls.add( UrlUtils.getURL( f ) );
            }
        }

        IsolatedClassLoader classLoader = new IsolatedClassLoader( parent, childDelegation );
        if ( assertionStatusMethod != null )
        {
            try
            {
                Object[] args = new Object[] { enableAssertions ? Boolean.TRUE : Boolean.FALSE };
                if ( parent != null )
                {
                    assertionStatusMethod.invoke( parent, args );
                }
                assertionStatusMethod.invoke( classLoader, args );
            }
            catch ( IllegalAccessException e )
            {
                throw new NestedRuntimeException( "Unable to access the assertion enablement method", e );
            }
            catch ( InvocationTargetException e )
            {
                throw new NestedRuntimeException( "Unable to invoke the assertion enablement method", e );
            }
        }
        for ( Iterator iter = urls.iterator(); iter.hasNext(); )
        {
            URL url = (URL) iter.next();
            classLoader.addURL( url );
        }
        return classLoader;
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

    private static Properties loadProperties( File file )
        throws IOException
    {
        Properties p = new Properties();

        if ( file != null && file.exists() )
        {
            FileInputStream inStream = new FileInputStream( file );
            try
            {
                p.load( inStream );
            }
            finally
            {
                IOUtil.close( inStream );
            }
        }

        return p;
    }

    private static void setSystemProperties( File file )
        throws IOException
    {
        Properties p = loadProperties( file );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, p.getProperty( key ) );
        }
    }

    private static Object[] constructParamObjects( String paramProperty, String typeProperty )
    {
        Object[] paramObjects = null;
        if ( paramProperty != null )
        {
            // bit of a glitch that it need sto be done twice to do an odd number of vertical bars (eg |||, |||||).
            String[] params =
                StringUtils.split(
                                   StringUtils.replace( StringUtils.replace( paramProperty, "||", "| |" ), "||", "| |" ),
                                   "|" );
            String[] types =
                StringUtils.split(
                                   StringUtils.replace( StringUtils.replace( typeProperty, "||", "| |" ), "||", "| |" ),
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
                else if (types[i].equals(Properties.class.getName())) {
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

    /**
     * This method is invoked when Surefire is forked - this method parses and organizes the arguments passed to it and
     * then calls the Surefire class' run method. <p/> The system exit code will be 1 if an exception is thrown.
     *
     * @param args
     */
    public static void main( String[] args )
        throws Throwable
    {
        // noinspection CatchGenericClass,OverlyBroadCatchBlock
        try
        {
            if ( args.length > 1 )
            {
                setSystemProperties( new File( args[1] ) );
            }

            File surefirePropertiesFile = new File( args[0] );
            Properties p = loadProperties( surefirePropertiesFile );

            SortedMap classPathUrls = new TreeMap();

            SortedMap surefireClassPathUrls = new TreeMap();

            SurefireBooter surefireBooter = new SurefireBooter( true );

            ForkConfiguration forkConfiguration = new ForkConfiguration();
            forkConfiguration.setForkMode( "never" );
            surefireBooter.setForkConfiguration( forkConfiguration );

            for ( Enumeration e = p.propertyNames(); e.hasMoreElements(); )
            {
                String name = (String) e.nextElement();

                if ( name.startsWith( REPORT_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) && !name.endsWith( TYPES_SUFIX ) )
                {
                    String className = p.getProperty( name );

                    String params = p.getProperty( name + PARAMS_SUFIX );
                    String types = p.getProperty( name + TYPES_SUFIX );
                    surefireBooter.addReport( className, constructParamObjects( params, types ) );
                }
                else if ( name.startsWith( TEST_SUITE_PROPERTY_PREFIX ) && !name.endsWith( PARAMS_SUFIX ) && !name.endsWith( TYPES_SUFIX ) )
                {
                    String className = p.getProperty( name );

                    String params = p.getProperty( name + PARAMS_SUFIX );
                    String types = p.getProperty( name + TYPES_SUFIX );
                    surefireBooter.addTestSuite( className, constructParamObjects( params, types ) );
                }
                else if ( name.startsWith( "classPathUrl." ) )
                {
                    classPathUrls.put(Integer.valueOf(name.substring(name.indexOf('.') + 1)), p.getProperty( name ));
                }
                else if ( name.startsWith( "surefireClassPathUrl." ) )
                {
                    surefireClassPathUrls.put(Integer.valueOf(name.substring(name.indexOf('.') + 1)), p.getProperty( name ));
                }
                else if ( name.startsWith( "surefireBootClassPathUrl." ) )
                {
                    surefireBooter.addSurefireBootClassPathUrl( p.getProperty( name ) );
                }
                else if ( "childDelegation".equals( name ) )
                {
                    surefireBooter.childDelegation =
                        Boolean.valueOf( p.getProperty( "childDelegation" ) ).booleanValue();
                }
                else if ( "enableAssertions".equals( name ) )
                {
                    surefireBooter.enableAssertions =
                        Boolean.valueOf( p.getProperty( "enableAssertions" ) ).booleanValue();
                }
                else if ( "useSystemClassLoader".equals( name ) )
                {
                    surefireBooter.forkConfiguration.setUseSystemClassLoader( Boolean.valueOf(
                                                                                               p.getProperty( "useSystemClassLoader" ) ).booleanValue() );
                }
                else if ( "failIfNoTests".equals( name ) )
                {
                    surefireBooter.setFailIfNoTests( Boolean.valueOf( p.getProperty( "failIfNoTests" ) ).booleanValue() );
                }
            }

            for (Iterator cpi = classPathUrls.keySet().iterator(); cpi.hasNext();)
            {
                String url = (String) classPathUrls.get(cpi.next());
                surefireBooter.addClassPathUrl(url);
            }

            for (Iterator scpi = surefireClassPathUrls.keySet().iterator(); scpi.hasNext();)
            {
                String url = (String) surefireClassPathUrls.get(scpi.next());
                surefireBooter.addSurefireClassPathUrl(url);
            }

            String testSet = p.getProperty( "testSet" );
            int result;
            if ( testSet != null )
            {
                result = surefireBooter.runSuitesInProcess( testSet, p );
            }
            else
            {
                result = surefireBooter.runSuitesInProcess();
            }

            surefireBooter.writePropertiesFile( surefirePropertiesFile, "surefire", p );

            // noinspection CallToSystemExit
            System.exit( result );
        }
        catch ( Throwable t )
        {
            // Just throwing does getMessage() and a local trace - we want to call printStackTrace for a full trace
            // noinspection UseOfSystemOutOrSystemErr
            t.printStackTrace( System.err );
            // noinspection ProhibitedExceptionThrown,CallToSystemExit
            System.exit( 1 );
        }
    }

    public void setChildDelegation( boolean childDelegation )
    {
        this.childDelegation = childDelegation;
    }

    private ForkingStreamConsumer getForkingStreamConsumer( boolean showHeading, boolean showFooter,
                                                     boolean redirectTestOutputToFile )
    {
        OutputConsumer outputConsumer = new StandardOutputConsumer();

        if ( redirectTestOutputToFile )
        {
            outputConsumer = new FileOutputConsumerProxy( outputConsumer, getReportsDirectory() );
        }

        if ( !showHeading )
        {
            outputConsumer = new SupressHeaderOutputConsumerProxy( outputConsumer );
        }
        if ( !showFooter )
        {
            outputConsumer = new SupressFooterOutputConsumerProxy( outputConsumer );
        }

        return new ForkingStreamConsumer( outputConsumer );
    }

    public void setEnableAssertions( boolean enableAssertions )
    {
        this.enableAssertions = enableAssertions;
    }
    
    public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
    {
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
    }
}
