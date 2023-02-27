package com.foo.impl;

import com.foo.api.SomeInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Bar implements SomeInterface
{
    private static final Logger LOG = LoggerFactory.getLogger( Bar.class );

    private boolean weAreAmongModules;

    @Override
    public void doItNow( Class<?> observer )
    {
        ModuleLayer.boot().modules().forEach( m -> {
            if ( m == observer.getModule() || m == Bar.class.getModule() || m == Logger.class.getModule() )
            {
                weAreAmongModules = true;
            }
        } );
        LOG.info( "" );
        LOG.info( "Let's see if I or SLF4J are among boot layer modules: {}", weAreAmongModules );
        if ( !weAreAmongModules )
        {
            LOG.info( "Maybe we are in child layer? Or this is not module path?" );
        }
    }
}
