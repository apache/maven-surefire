package com.appnexus.viewability.core.surefireJunitTests;

import org.junit.Assert;
import org.junit.Test;

public class CTest
        extends BaseTest
{
    public CTest( String param )
    {
        super( param );
    }

    @Test
    public void methodC1() throws InterruptedException
    {
        sleep( 1 );
        if ( Boolean.getBoolean( "canFail" ) )
        {
            Assert.fail( "Failing test: CTest.methodC1[" + param + "]" );
        }
    }

    @Test
    public void methodC2() throws InterruptedException
    {
        sleep( 1 );
        if ( Boolean.getBoolean( "canFail" ) )
        {
            Assert.fail( "Failing test: CTest.methodC2[" + param + "]" );
        }
    }
}
