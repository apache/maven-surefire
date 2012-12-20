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
import java.util.Collections;
import java.util.List;

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


    public SmartStackTraceParser( Class testClass, Throwable throwable )
    {
        this( testClass.getName(), throwable );
    }

    public SmartStackTraceParser( String testClassName, Throwable throwable )
    {
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
            return Class.forName( name );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( e );
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
        StringBuilder result = new StringBuilder();
        List<StackTraceElement> stackTraceElements = focusOnClass( stackTrace, testClass );
        Collections.reverse( stackTraceElements );
        StackTraceElement stackTraceElement;
        for ( int i = 0; i < stackTraceElements.size(); i++ )
        {
            stackTraceElement = stackTraceElements.get( i );
            if ( i == 0 )
            {
                result.append( simpleName );
                result.append( "#" );
            }
            if ( !stackTraceElement.getClassName().equals( testClassName ) )
            {
                if ( i > 0 )
                {
                    result.append( "<" );
                }
                result.append( getSimpleName( stackTraceElement.getClassName() ) ); // Add the name of the superclas
                result.append( "#" );
            }
            result.append( stackTraceElement.getMethodName() ).append( "(" ).append(
                stackTraceElement.getLineNumber() ).append( ")" );
            result.append( "." );
        }

        result.deleteCharAt( result.length() - 1 );

        if ( throwable.getTarget() instanceof AssertionError )
        {
            result.append( " " );
            result.append( throwable.getMessage() );
        }
        else
        {
            result.append( rootIsInclass() ? " " : " >> " );
            result.append( throwable.getTarget().getClass().getSimpleName() );
            result.append( getTruncatedMessage( 77 - result.length() ) );
        }
        return result.toString();
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
        return stackTrace[0].getClassName().equals( testClassName );
    }

    static List<StackTraceElement> focusOnClass( StackTraceElement[] stackTrace, Class clazz )
    {
        List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        for ( StackTraceElement element : stackTrace )
        {
            if ( isInSupers( clazz, element.getClassName() ) )
            {
                result.add( element );
            }
        }
        return result;
    }


    private static boolean isInSupers( Class testClass, String lookFor )
    {
        while ( !testClass.getName().equals( lookFor ) && testClass.getSuperclass() != null )
        {
            testClass = testClass.getSuperclass();
        }
        return testClass.getName().equals( lookFor );
    }

    private static Throwable findInnermost( Throwable t )
    {
        Throwable real = t;
        while ( real.getCause() != null )
        {
            real = real.getCause();
        }
        return real;
    }

    public static String innerMostWithFocusOnClass( Throwable t, String className )
    {
        List<StackTraceElement> stackTraceElements = focusInsideClass( findInnermost( t ).getStackTrace(), className );
        return toString( t, stackTraceElements );
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

    public static String toString( Throwable t, List<StackTraceElement> elements )
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


