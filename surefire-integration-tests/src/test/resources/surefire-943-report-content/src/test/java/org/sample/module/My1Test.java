package org.sample.module;

import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

public class My1Test
{
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

    @Test
    @Ignore( "Ignore-Message" )
    public void alwaysIgnored()
    {

    }
}
