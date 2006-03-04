package org.apache.maven.surefire.booter;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

/**
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @version $Id$
 */
public class SurefireBooter
{
    private List reports = new ArrayList();

    private List classPathUrls = new ArrayList();

    private List surefireClassPathUrls = new ArrayList();

    private List testSuites = new ArrayList();

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private ForkConfiguration forkConfiguration;

    private static final int TESTS_SUCCEEDED_EXIT_CODE = 0;

    private static final int TESTS_FAILED_EXIT_CODE = 255;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void addReport( String report )
    {
        addReport( report, null );
    }

    public void addReport( String report, Object[] constructorParams )
    {
        reports.add( new Object[]{report, constructorParams} );
    }

    public void addClassPathUrl( String path )
    {
        if ( !classPathUrls.contains( path ) )
        {
            classPathUrls.add( path );
        }
    }

    public void addTestSuite( String suiteClassName, Object[] constructorParams )
    {
        testSuites.add( new Object[]{suiteClassName, constructorParams} );
    }

    public void addSurefireClassPathUrl( String path )
    {
        if ( !surefireClassPathUrls.contains( path ) )
        {
            surefireClassPathUrls.add( path );
        }
    }

    public void setForkConfiguration( ForkConfiguration forkConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
    }

    public boolean run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        boolean result = false;

        if ( ForkConfiguration.FORK_NONE.equals( forkConfiguration.getForkMode() ) )
        {
            result = runSuitesInProcess( false );
        }
        else if ( ForkConfiguration.FORK_ONCE.equals( forkConfiguration.getForkMode() ) )
        {
            result = runSuitesForkOnce();
        }
        else if ( ForkConfiguration.FORK_PERTEST.equals( forkConfiguration.getForkMode() ) )
        {
            result = runSuitesForkPerTestSet();
        }
        return result;
    }

    private boolean runSuitesInProcess( String testSet, boolean childDelegation )
        throws SurefireExecutionException
    {
        if ( testSuites.size() != 1 )
        {
            throw new IllegalArgumentException( "Cannot only specify testSet for single test suites" );
        }

        // TODO: replace with plexus

        //noinspection CatchGenericClass,OverlyBroadCatchBlock
        try
        {
            // TODO: assertions = true shouldn't be required if we had proper separation (see TestNG)
            ClassLoader surefireClassLoader =
                createClassLoader( surefireClassPathUrls, getClass().getClassLoader(), true );

            ClassLoader testsClassLoader = createClassLoader( classPathUrls, childDelegation, true );

            Class surefireClass = surefireClassLoader.loadClass( Surefire.class.getName() );

            Object surefire = surefireClass.newInstance();

            Method run = surefireClass.getMethod( "run", new Class[]{List.class, Object[].class, String.class,
                ClassLoader.class, ClassLoader.class} );

            ClassLoader oldContextClassLoader = Thread.currentThread() .getContextClassLoader();

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            Boolean result = (Boolean) run.invoke( surefire, new Object[]{reports, testSuites.get( 0 ), testSet,
                surefireClassLoader, testsClassLoader} );

            Thread.currentThread().setContextClassLoader( oldContextClassLoader );

            return result.booleanValue();
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireExecutionException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( Exception e )
        {
            throw new SurefireExecutionException( "Unable to instantiate and execute Surefire", e );
        }
    }

    private boolean runSuitesInProcess( boolean childDelegation )
        throws SurefireExecutionException
    {
        // TODO: replace with plexus

        //noinspection CatchGenericClass,OverlyBroadCatchBlock
        try
        {
            ClassLoader surefireClassLoader =
                createClassLoader( surefireClassPathUrls, getClass().getClassLoader(), true );

            ClassLoader testsClassLoader = createClassLoader( classPathUrls, childDelegation, true );

            Class surefireClass = surefireClassLoader.loadClass( Surefire.class.getName() );

            Object surefire = surefireClass.newInstance();

            Method run = surefireClass.getMethod( "run", new Class[]{List.class, List.class, ClassLoader.class,
                ClassLoader.class} );

            ClassLoader oldContextClassLoader = Thread.currentThread() .getContextClassLoader();

            Thread.currentThread().setContextClassLoader( testsClassLoader );

            Boolean result = (Boolean) run.invoke( surefire, new Object[]{reports, testSuites, surefireClassLoader,
                testsClassLoader} );

            Thread.currentThread().setContextClassLoader( oldContextClassLoader );

            return result.booleanValue();
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireExecutionException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( Exception e )
        {
            throw new SurefireExecutionException( "Unable to instantiate and execute Surefire", e );
        }
    }

    private boolean runSuitesForkOnce()
        throws SurefireBooterForkException
    {
        return forkSuites( testSuites, true );
    }

    private boolean runSuitesForkPerTestSet()
        throws SurefireBooterForkException
    {
        ClassLoader testsClassLoader;
        ClassLoader surefireClassLoader;
        try
        {
            testsClassLoader = createClassLoader( classPathUrls, false, true );
            // TODO: assertions = true shouldn't be required if we had proper separation (see TestNG)
            surefireClassLoader = createClassLoader( surefireClassPathUrls, false, true );
        }
        catch ( MalformedURLException e )
        {
            throw new SurefireBooterForkException( "Unable to create classloader to find test suites", e );
        }

        boolean failed = false;

        boolean showHeading = true;
        for ( Iterator i = testSuites.iterator(); i.hasNext(); )
        {
            Object[] testSuite = (Object[]) i.next();

            Map testSets = getTestSets( testSuite, testsClassLoader, surefireClassLoader );

            for ( Iterator j = testSets.keySet().iterator(); j.hasNext(); )
            {
                String testSet = (String) j.next();
                boolean result = forkSuite( testSuite, testSet, showHeading );
                if ( !result )
                {
                    failed = true;
                }
                showHeading = false;
            }
        }

        return !failed;
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
            throw new SurefireBooterForkException(
                "Unable to find appropriate constructor for test suite '" + className + "': " + e.getMessage(), e );
        }

        Map testSets;
        try
        {
            Method m = suite.getClass().getMethod( "locateTestSets", new Class[]{ClassLoader.class} );

            testSets = (Map) m.invoke( suite, new Object[]{testsClassLoader} );
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

    private boolean forkSuites( List testSuites, boolean showHeading )
        throws SurefireBooterForkException
    {
        Properties properties = createForkProperties( testSuites );

        return fork( properties, showHeading );
    }

    private boolean forkSuite( Object[] testSuite, String testSet, boolean showHeading )
        throws SurefireBooterForkException
    {
        Properties properties = createForkProperties( Collections.singletonList( testSuite ) );

        properties.setProperty( "testSet", testSet );

        return fork( properties, showHeading );
    }

    private Properties createForkProperties( List testSuites )
    {
        Properties properties = new Properties();

        addPropertiesForTypeHolder( reports, properties, "report." );
        addPropertiesForTypeHolder( testSuites, properties, "testSuite." );

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

        properties.setProperty( "childDelegation", String.valueOf( forkConfiguration.isChildDelegation() ) );
        return properties;
    }

    private File writePropertiesFile( String name, Properties properties )
        throws IOException
    {
        File file = File.createTempFile( name, "tmp" );
        file.deleteOnExit();

        FileOutputStream out = new FileOutputStream( file );

        try
        {
            properties.store( out, name );
        }
        finally
        {
            IOUtil.close( out );
        }

        return file;
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
                String paramProperty = params[0].toString();
                String typeProperty = params[0].getClass().getName();
                for ( int j = 1; j < params.length; j++ )
                {
                    paramProperty += "|";
                    typeProperty += "|";
                    if ( params[j] != null )
                    {
                        paramProperty += params[j].toString();
                        typeProperty += params[j].getClass().getName();
                    }
                }
                properties.setProperty( propertyPrefix + i + ".params", paramProperty );
                properties.setProperty( propertyPrefix + i + ".types", typeProperty );
            }
        }
    }

    private boolean fork( Properties properties, boolean showHeading )
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

        Commandline cli = forkConfiguration.createCommandLine( surefireClassPathUrls );

        cli.createArgument().setFile( surefireProperties );

        if ( systemProperties != null )
        {
            cli.createArgument().setFile( systemProperties );
        }

        Writer consoleWriter = new OutputStreamWriter( System.out );

        StreamConsumer out = new ForkingWriterStreamConsumer( consoleWriter, showHeading );

        StreamConsumer err = new ForkingWriterStreamConsumer( consoleWriter, showHeading );

        if ( forkConfiguration.isDebug() )
        {
            System.out.println( "Forking command line: " + cli );
        }

        int returnCode;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );
        }
        catch ( CommandLineException e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e );
        }

        return returnCode == 0;
    }

    private static ClassLoader createClassLoader( List classPathUrls, ClassLoader parent, boolean assertionsEnabled )
        throws MalformedURLException
    {
        return createClassLoader( classPathUrls, parent, false, assertionsEnabled );
    }

    private static ClassLoader createClassLoader( List classPathUrls, boolean childDelegation,
                                                  boolean assertionsEnabled )
        throws MalformedURLException
    {
        return createClassLoader( classPathUrls, ClassLoader.getSystemClassLoader(), childDelegation,
                                  assertionsEnabled );
    }

    private static ClassLoader createClassLoader( List classPathUrls, ClassLoader parent, boolean childDelegation,
                                                  boolean assertionsEnabled )
        throws MalformedURLException
    {
        List urls = new ArrayList();

        for ( Iterator i = classPathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url != null )
            {
                File f = new File( url );
                urls.add( f.toURL() );
            }
        }

        IsolatedClassLoader classLoader = new IsolatedClassLoader( parent, childDelegation );
        // TODO: for some reason, this doesn't work when forked. -ea is added to the command line as a workaround
        // in forkConfiguration
        classLoader.setDefaultAssertionStatus( assertionsEnabled );
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

        String[] stringArray = sl.split( "," );

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
            String[] params = paramProperty.split( "\\|" );
            String[] types = typeProperty.split( "\\|" );

            paramObjects = new Object[params.length];

            for ( int i = 0; i < types.length; i++ )
            {
                if ( types[i].length() == 0 )
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
     * This method is invoked when Surefire is forked - this method parses and
     * organizes the arguments passed to it and then calls the Surefire class'
     * run method.
     * <p/>
     * The system exit code will be 1 if an exception is thrown.
     *
     * @param args
     */
    public static void main( String[] args )
        throws SurefireExecutionException, IOException
    {
        if ( args.length > 1 )
        {
            setSystemProperties( new File( args[1] ) );
        }

        Properties p = loadProperties( new File( args[0] ) );

        SurefireBooter surefireBooter = new SurefireBooter();

        for ( Enumeration e = p.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if ( name.startsWith( "report." ) && !name.endsWith( ".params" ) && !name.endsWith( ".types" ) )
            {
                String className = p.getProperty( name );

                String params = p.getProperty( name + ".params" );
                String types = p.getProperty( name + ".types" );
                surefireBooter.addReport( className, constructParamObjects( params, types ) );
            }
            else if ( name.startsWith( "testSuite." ) && !name.endsWith( ".params" ) && !name.endsWith( ".types" ) )
            {
                String className = p.getProperty( name );

                String params = p.getProperty( name + ".params" );
                String types = p.getProperty( name + ".types" );
                surefireBooter.addTestSuite( className, constructParamObjects( params, types ) );
            }
            else if ( name.startsWith( "classPathUrl." ) )
            {
                surefireBooter.addClassPathUrl( p.getProperty( name ) );
            }
            else if ( name.startsWith( "surefireClassPathUrl." ) )
            {
                surefireBooter.addSurefireClassPathUrl( p.getProperty( name ) );
            }
        }

        boolean childDelegation = Boolean.valueOf( p.getProperty( "childDelegation" ) ).booleanValue();
        String testSet = p.getProperty( "testSet" );
        boolean result;
        if ( testSet != null )
        {
            result = surefireBooter.runSuitesInProcess( testSet, childDelegation );
        }
        else
        {
            result = surefireBooter.runSuitesInProcess( childDelegation );
        }

        //noinspection CallToSystemExit
        System.exit( result ? TESTS_SUCCEEDED_EXIT_CODE : TESTS_FAILED_EXIT_CODE );
    }
}

