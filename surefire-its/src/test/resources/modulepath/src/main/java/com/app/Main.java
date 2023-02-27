package com.app;

import org.joda.time.DateTime;

public class Main
{
    public static void main( String... args )
    {
        System.out.println( "module path => " + System.getProperty( "jdk.module.path" ) );
        System.out.println( " class path => " + System.getProperty( "java.class.path" ) );

        DateTime dt = new DateTime();
        System.out.println( dt );
    }
}
