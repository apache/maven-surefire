package com.foo.impl;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BarIT
{
    private static final Logger LOG = LoggerFactory.getLogger( BarIT.class );

    @Test
    void shouldPrintModulePath()
    {
        Bar bar = new Bar();
        LOG.info( "======INTEGRATION TEST=======" );
        LOG.info( "Lets see JDKModulePath: {}", System.getProperty( "jdk.module.path" ) );
        bar.doItNow( getClass() );
    }
}
