package com.foo.impl;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BarTest
{
    private static final Logger LOG = LoggerFactory.getLogger( BarTest.class );

    @Test
    void shouldPrintModulePath()
    {
        Bar bar = new Bar();
        LOG.info( "======UNIT TEST=======" );
        LOG.info( "Lets see JDKModulePath: {}", System.getProperty( "jdk.module.path" ) );
        bar.doItNow( getClass() );
    }
}
