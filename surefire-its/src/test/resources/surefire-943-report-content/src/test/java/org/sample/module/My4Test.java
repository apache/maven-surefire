package org.sample.module;

import org.junit.Test;
import org.junit.Ignore;

@Ignore( "Ignore-Message" )
public class My4Test
{

    @Test
    public void alsoIgnored()
    {

    }
    
    @Test
    @Ignore
    public void alwaysIgnored()
    {

    }
}
