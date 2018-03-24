package com.appnexus.viewability.core.surefireJunitTests;

import org.junit.Assert;
import org.junit.Test;

public class BTest
        extends BaseTest
{
    public BTest( String param )
    {
        super( param );
    }

    @Test
    public void methodB1() throws InterruptedException
    {
        sleep( 10 );
        if ( Boolean.getBoolean( "canFail" ) )
        {
            Assert.fail( "Failing test: BTest.methodB1[" + param + "]" );
        }
    }

    @Test
    public void methodB2() throws InterruptedException
    {
        sleep( 10 );
        if ( Boolean.getBoolean( "canFail" ) )
        {
            Assert.fail( "Failing test: BTest.methodB2[" + param + "]" );
        }
    }
}
