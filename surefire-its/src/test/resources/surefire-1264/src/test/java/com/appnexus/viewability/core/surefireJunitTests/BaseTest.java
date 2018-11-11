package com.appnexus.viewability.core.surefireJunitTests;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Collection;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;


import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith( Parameterized.class )
public abstract class BaseTest
{
    protected final String param;

    public BaseTest( String param )
    {
        this.param = param;
    }

    @Rule
    public TestName testName = new TestName();

    @Parameters( name = "{0}" )
    public static Collection< String > parameterList() throws Exception
    {
        Collection< String > c = new ConcurrentLinkedQueue<>();
        c.add( "p0" );
        c.add( "p1" );

        return c;
    }

    public void sleep( int time )
    {
        System.err.println( "Start: " + this.getClass().getSimpleName() + "." + testName.getMethodName() );
        try
        {
            Thread.sleep( time * 100 );
        }
        catch ( InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.err.println( "End:   " + this.getClass().getSimpleName() + "." + testName.getMethodName() );
    }
}
