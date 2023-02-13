package org.sample.module;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.Ignore;

public class My2Test {
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
    @Ignore
    public void alwaysIgnored()
    {

    }
}
