package org.apache.maven.surefire.report;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.shared.utils.StringUtils;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
public class SmartStackTraceParser
{

    private final SafeThrowable throwable;

    private final StackTraceElement[] stackTrace;

    private final String simpleName;

    private String testClassName;

    private final Class testClass;

    private String testMethodName;

    public SmartStackTraceParser( Class testClass, Throwable throwable )
    {
        this( testClass.getName(), throwable, null );
    }

    public SmartStackTraceParser( String testClassName, Throwable throwable, String testMethodName )
    {
        this.testMethodName = testMethodName;
        this.testClassName = testClassName;
        this.testClass = getClass( testClassName );
        this.simpleName = this.testClassName.substring( this.testClassName.lastIndexOf( "." ) + 1 );
        this.throwable = new SafeThrowable( throwable );
        stackTrace = throwable.getStackTrace();
    }

    private static Class getClass( String name )
    {
        try
        {
            return Thread.currentThread().getContextClassLoader().loadClass( name );
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }

    private static String getSimpleName( String className )
    {
        int i = className.lastIndexOf( "." );
        return className.substring( i + 1 );
    }

    @SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
    public String getString()
    {
        if ( testClass == null )
        {
            return throwable.getLocalizedMessage();
        }

        StringBuilder result = new StringBuilder();
        List<StackTraceElement> stackTraceElements = focusOnClass( stackTrace, testClass );
        Collections.reverse( stackTraceElements );
        StackTraceElement stackTraceElement;
        if ( stackTraceElements.isEmpty() )
        {
            result.append( simpleName );
            if (StringUtils.isNotEmpty( testMethodName ))
            {
                result.append( "." ).append( testMethodName );
            }
        }
        else
        {
            for ( int i = 0; i < stackTraceElements.size(); i++ )
            {
                stackTraceElement = stackTraceElements.get( i );
                if ( i == 0 )
                {
                    result.append( simpleName );
                    if ( !stackTraceElement.getClassName().equals( testClassName ) )
                    {
                        result.append( ">" );
                    }
                    else
                    {
                        result.append( "." );
                    }

                }
                if ( !stackTraceElement.getClassName().equals( testClassName ) )
                {
                    result.append( getSimpleName( stackTraceElement.getClassName() ) ); // Add the name of the superclas
                    result.append( "." );
                }
                result.append( stackTraceElement.getMethodName() ).append( ":" ).append(
                    stackTraceElement.getLineNumber() );
                result.append( "->" );
            }

            if ( result.length() >= 2 )
            {
                result.deleteCharAt( result.length() - 1 );
                result.deleteCharAt( result.length() - 1 );
            }
        }

        Throwable target = throwable.getTarget();
        if ( target instanceof AssertionError )
        {
            result.append( " " );
            result.append( throwable.getMessage() );
        }
        else if ( "junit.framework.AssertionFailedError".equals( target.getClass().getName() )
            || "junit.framework.ComparisonFailure".equals( target.getClass().getName() ) )
        {
            result.append( " " );
            result.append( throwable.getMessage() );
        }
        else
        {
            result.append( rootIsInclass() ? " " : " » " );
            result.append( getMinimalThrowableMiniMessage( target ) );
            result.append( getTruncatedMessage( 77 - result.length() ) );
        }
        return result.toString();
    }

    private String getMinimalThrowableMiniMessage( Throwable throwable )
    {
        String name = throwable.getClass().getSimpleName();
        if ( name.endsWith( "Exception" ) )
        {
            return StringUtils.chompLast( name, "Exception" );
        }
        if ( name.endsWith( "Error" ) )
        {
            return StringUtils.chompLast( name, "Error" );
        }
        return name;
    }

    private String getTruncatedMessage( int i )
    {
        if ( i < 0 )
        {
            return "";
        }
        String msg = throwable.getMessage();
        if ( msg == null )
        {
            return "";
        }
        String substring = msg.substring( 0, Math.min( i, msg.length() ) );
        if ( i < msg.length() )
        {
            return " " + substring + "...";
        }
        else
        {
            return " " + substring;
        }
    }

    private boolean rootIsInclass()
    {
        return stackTrace.length > 0 && stackTrace[0].getClassName().equals( testClassName );
    }

    static List<StackTraceElement> focusOnClass( StackTraceElement[] stackTrace, Class clazz )
    {
        List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        for ( StackTraceElement element : stackTrace )
        {
            if ( element != null && isInSupers( clazz, element.getClassName() ) )
            {
                result.add( element );
            }
        }
        return result;
    }

    private static boolean isInSupers( Class testClass, String lookFor )
    {
        if ( lookFor.startsWith( "junit.framework." ) )
        {
            return false;
        }
        while ( !testClass.getName().equals( lookFor ) && testClass.getSuperclass() != null )
        {
            testClass = testClass.getSuperclass();
        }
        return testClass.getName().equals( lookFor );
    }

    static Throwable findInnermostWithClass( Throwable t, String className )
    {
        Throwable match = t;
        do
        {
            if ( containsClassName( t.getStackTrace(), className ) )
            {
                match = t;
            }

            t = t.getCause();

        }
        while ( t != null );
        return match;
    }

    public static String innerMostWithFocusOnClass( Throwable t, String className )
    {
        Throwable innermost = findInnermostWithClass( t, className );
        List<StackTraceElement> stackTraceElements = focusInsideClass( innermost.getStackTrace(), className );
        String s = causeToString( innermost.getCause() );
        return toString( t, stackTraceElements ) + s;
    }

    static List<StackTraceElement> focusInsideClass( StackTraceElement[] stackTrace, String className )
    {
        List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        boolean found = false;
        for ( StackTraceElement element : stackTrace )
        {
            if ( !found )
            {
                result.add( element );
            }

            if ( className.equals( element.getClassName() ) )
            {
                if ( found )
                {
                    result.add( element );
                }
                found = true;
            }
            else
            {
                if ( found )
                {
                    break;
                }
            }
        }
        return result;
    }

    static boolean containsClassName( StackTraceElement[] stackTrace, String className )
    {
        for ( StackTraceElement element : stackTrace )
        {
            if ( className.equals( element.getClassName() ) )
            {
                return true;
            }
        }
        return false;
    }

    public static String causeToString( Throwable cause )
    {
        StringBuilder resp = new StringBuilder();
        while ( cause != null )
        {
            resp.append( "Caused by: " );
            resp.append( toString( cause, Arrays.asList( cause.getStackTrace() ) ) );
            cause = cause.getCause();
        }
        return resp.toString();
    }

    public static String toString( Throwable t, Iterable<StackTraceElement> elements )
    {
        StringBuilder result = new StringBuilder();
        result.append( t.getClass().getName() );
        result.append( ": " );
        result.append( t.getMessage() );
        result.append( "\n" );

        for ( StackTraceElement element : elements )
        {
            result.append( "\tat " ).append( element.toString() );
            result.append( "\n" );
        }
        return result.toString();
    }
}
