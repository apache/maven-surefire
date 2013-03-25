package org.sample.module;

import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Before;
import org.junit.Test;

public class My3Test {
    @Test
    public void fails()
        throws Exception
    {
        Thread.sleep( 100 );
        fail( "Always fails" );
    }

    @Test
    public void alwaysSuccessful()
        throws Exception
    {
        Thread.sleep( 100 );
    }
}
