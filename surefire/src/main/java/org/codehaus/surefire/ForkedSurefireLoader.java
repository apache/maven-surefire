package org.codehaus.surefire;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is executed when SurefireBooter forks surefire JUnit processes
 *
 * @author <a href="mailto:andyglick@acm.org">Andy Glick</a>
 * @version $Id$
 */
public class ForkedSurefireLoader
{
    static int TESTS_SUCCEEDED = 0;
    static int TESTS_FAILED = 255;
    static int ILLEGAL_ARGUMENT_EXCEPTION = 100;
    static int OTHER_EXCEPTION = 200;

    private static Class thisClass = ForkedSurefireLoader.class;

    private static final Log log = LogFactory.getLog(ForkedSurefireLoader.class);

    /**
     * Default constructor
     */
    private ForkedSurefireLoader()
    {
        super();
    }

    /**
     * Constructs a Map from a set of strings of the form <key>=<value>
     *
     * @param args an array of strings composed of name/value pairs
     * @return Map keyed by the names with the respective values
     */
    private static Map getArgMap(String[] args)
    {
        Map argMap = new LinkedHashMap();

        for( int i = 0; i < args.length; i++ )
        {
            String[] mapArgs = args[i].split( "=" );

            argMap.put( mapArgs[0], mapArgs[1] );
        }

        return argMap;
    }

    /**
     * This method is invoked when Surefire is forked - this method parses and
     * organizes the arguments passed to it and then calls the Surefire class'
     * run method.
     *
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception
    {
        Map argMap = getArgMap(args);

        log.debug( "argmap = " + argMap );

        ClassLoader surefireClassLoader = thisClass.getClassLoader();

        String batteryExecutorName = ( String ) argMap
            .get( "batteryExecutorName" );

        Class batteryExecutorClass = surefireClassLoader
            .loadClass( batteryExecutorName );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        // Thread.currentThread().setContextClassLoader( surefireClassLoader );

        String reports = ( String ) argMap.get( "reportClassNames" );

        String[] reportClasses = reports.split( "," );

        List reportList = Arrays.asList( reportClasses );

        log.debug( "reportList is " + reportList );

        String batteryConfig = ( String ) argMap.get( "batteryConfig" );

        log.debug( "batteryConfig is " + batteryConfig );

        String[] batteryParts = batteryConfig.split( "\\|" );

        log.debug( "length of batteryParts is " + batteryParts.length );

        String batteryClassName = batteryParts[0];

        log.debug( "batteryClassName = " + batteryClassName );

        log.debug(
            "batteryParts.length - 1 is " + ( batteryParts.length - 1 ) );

        Object[] batteryParms = new Object[batteryParts.length - 1];

        batteryParms[0] = new File( batteryParts[1] );

        String stringList = batteryParts[2];

        log.debug("stringList is " + stringList);

        if( stringList.startsWith( "[" ) && stringList.endsWith( "]" ) )
        {
            stringList = stringList.substring( 1, stringList.length() - 1 );
        }

        ArrayList includesList = new ArrayList();

        String[] stringArray = stringList.split( "," );

        for( int i = 0; i < stringArray.length; i++ )
        {
            includesList.add( stringArray[i].trim() );
        }

        log.debug( "includesList is " + includesList );

        batteryParms[1] = includesList;

        stringList = batteryParts[3];

        log.debug("stringList is " + stringList);

        ArrayList excludesList = new ArrayList();

        if( stringList.startsWith( "[" ) && stringList.endsWith( "]" ) )
        {
            stringList = stringList.substring( 1, stringList.length() - 1 );
        }

        stringArray = stringList.split( "," );

        for( int i = 0; i < stringArray.length; i++ )
        {
            excludesList.add( stringArray[i].trim() );
        }

        log.debug( "excludesList is " + excludesList );

        batteryParms[2] = excludesList;

        List batteryHolders = new ArrayList();

        batteryHolders.add( new Object[]{batteryClassName,
            batteryParms} );

        String reportsDirectory = ( String ) argMap.get( "reportsDirectory" );


        log.debug(
            "batteryExecutorName is " + batteryExecutorClass.getName() );

        log.debug( "reports are " + reports );

        log.debug( "reportsDirectory is " + reportsDirectory );

        Class reporterClass = surefireClassLoader
            .loadClass( "org.codehaus.surefire.report.Reporter" );

        log.debug( "reporterClass' classloader is " + reporterClass
            .getClassLoader().getClass().getName() );

        Method run = batteryExecutorClass.getMethod( "run",
            new Class[]{List.class,
                List.class,
                String.class} );

        Object[] parms = new Object[]{reportList,
            batteryHolders,
            reportsDirectory};

        log.debug( "size of batteryHolders is " + batteryHolders.size() );

        int returnCode = TESTS_SUCCEEDED;

        try
        {
            boolean result  = ((Boolean)
                run.invoke( batteryExecutor, parms )).booleanValue();

            if( result )
            {
                returnCode = TESTS_SUCCEEDED;
            }
        }
        catch( IllegalArgumentException e )
        {
            if( log.isDebugEnabled() )
            {
                log.debug( "IllegalArgumentException thrown" );

                log.debug( "method name is " + run.getName() );

                log.debug( "batteryExecutor is of type " + batteryExecutor
                    .getClass().getName() );

                for( int i = 0; i < parms.length; i++ )
                {
                    log.debug(
                        "for parm[" + i + "] the class is " + parms[i].getClass()
                            .getName() );
                }
            }

            log.error( "IllegalArgumentException thrown", e);

            returnCode = ILLEGAL_ARGUMENT_EXCEPTION;
        }
        catch( Exception e)
        {
            log.error( "Exception thrown", e);

            returnCode = OTHER_EXCEPTION;
        }

        System.exit(returnCode);
    }
}
