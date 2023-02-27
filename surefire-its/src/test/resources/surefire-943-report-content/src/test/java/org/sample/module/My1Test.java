package org.sample.module;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.Ignore;

public class My1Test
{
    @Test
    public void fails()
        throws Exception
    {
        Thread.sleep( 1000 );
        fail( "Always fails" );
    }

    @Test
    public void alwaysSuccessful()
        throws Exception
    {
        Thread.sleep( 1000 );
    }

    @Test
    @Ignore( "Ignore-Message" )
    public void alwaysIgnored()
    {

    }
}
