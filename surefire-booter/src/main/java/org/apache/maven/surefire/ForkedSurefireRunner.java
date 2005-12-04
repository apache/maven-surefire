package org.apache.maven.surefire;

import java.io.File;
import java.lang.reflect.Method;
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
public class ForkedSurefireRunner
{
    public static String FORK_ONCE = "once";

    public static String FORK_PERTEST = "pertest";

    static int TESTS_SUCCEEDED = 0;

    static int TESTS_FAILED = 255;

    static int ILLEGAL_ARGUMENT_EXCEPTION = 100;

    static int OTHER_EXCEPTION = 200;

    private static Class thisClass = ForkedSurefireRunner.class;

    private ForkedSurefireRunner()
    {
        super();
    }

    /**
     * Constructs a Map from a set of strings of the form <key>=<value>
     *
     * @param args an array of strings composed of name/value pairs
     * @return Map keyed by the names with the respective values
     */
    private static Map getArgMap( String[] args )
    {
        Map argMap = new LinkedHashMap();

        for ( int i = 0; i < args.length; i++ )
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
    public static void main( String[] args )
        throws Exception
    {
        Map argMap = getArgMap( args );

        ClassLoader surefireClassLoader = thisClass.getClassLoader();

        String batteryExecutorName = (String) argMap.get( "batteryExecutorName" );

        Class batteryExecutorClass = surefireClassLoader.loadClass( batteryExecutorName );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        String reports = (String) argMap.get( "reportClassNames" );

        String[] reportClasses = reports.split( "," );

        List reportList = Arrays.asList( reportClasses );

        String batteryConfig = (String) argMap.get( "batteryConfig" );

        String[] batteryParts = batteryConfig.split( "\\|" );

        String batteryClassName = batteryParts[0];

        Object[] batteryParms;

        String forkMode = (String) argMap.get( "forkMode" );

        if ( forkMode.equals( FORK_ONCE ) )
        {
            batteryParms = new Object[batteryParts.length - 1];

            batteryParms[0] = new File( batteryParts[1] );

            String stringList = batteryParts[2];

            if ( stringList.startsWith( "[" ) && stringList.endsWith( "]" ) )
            {
                stringList = stringList.substring( 1, stringList.length() - 1 );
            }

            ArrayList includesList = new ArrayList();

            String[] stringArray = stringList.split( "," );

            for ( int i = 0; i < stringArray.length; i++ )
            {
                includesList.add( stringArray[i].trim() );
            }

            batteryParms[1] = includesList;

            stringList = batteryParts[3];

            ArrayList excludesList = new ArrayList();

            if ( stringList.startsWith( "[" ) && stringList.endsWith( "]" ) )
            {
                stringList = stringList.substring( 1, stringList.length() - 1 );
            }

            stringArray = stringList.split( "," );

            for ( int i = 0; i < stringArray.length; i++ )
            {
                excludesList.add( stringArray[i].trim() );
            }

            batteryParms[2] = excludesList;
        }
        else
        {
            batteryParms = new Object[1];

            batteryParms[0] = batteryParts[1];
        }

        List batteryHolders = new ArrayList();

        batteryHolders.add( new Object[]{batteryClassName, batteryParms} );

        String reportsDirectory = (String) argMap.get( "reportsDirectory" );

        Method run = batteryExecutorClass.getMethod( "run", new Class[]{List.class, List.class, String.class} );

        Object[] parms = new Object[]{reportList, batteryHolders, reportsDirectory};

        int returnCode = TESTS_FAILED;

        try
        {
            boolean result = ( (Boolean) run.invoke( batteryExecutor, parms ) ).booleanValue();

            if ( result )
            {
                returnCode = TESTS_SUCCEEDED;
            }

        }
        catch ( IllegalArgumentException e )
        {
            returnCode = ILLEGAL_ARGUMENT_EXCEPTION;
        }
        catch ( Exception e )
        {
            returnCode = OTHER_EXCEPTION;
        }

        System.exit( returnCode );
    }
}
