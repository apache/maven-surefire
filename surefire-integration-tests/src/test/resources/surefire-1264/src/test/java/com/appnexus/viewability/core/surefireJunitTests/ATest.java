package com.appnexus.viewability.core.surefireJunitTests;

import org.junit.Assert;
import org.junit.Test;

public class ATest
        extends BaseTest
{
    public ATest( String param )
    {
        super( param );
    }

    @Test
    public void methodA1() throws InterruptedException
    {
        sleep( 10 );
        if ( Boolean.getBoolean( "canFail" ) )
        {
            Assert.fail( "Failing test: ATest.methodA1[" + param + "]" );
        }
    }

    @Test
    public void methodA2() throws InterruptedException
    {
        sleep( 10 );
        if ( Boolean.getBoolean( "canFail" ) )
        {
            Assert.fail( "Failing test: ATest.methodA2[" + param + "]" );
        }
    }
}
