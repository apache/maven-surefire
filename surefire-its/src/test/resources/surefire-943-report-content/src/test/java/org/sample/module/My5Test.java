package org.sample.module;

import org.junit.BeforeClass;
import org.junit.Test;

public class My5Test {

    @BeforeClass 
    public static void failsOnBeforeClass()
    {
        throw new RuntimeException("always fails before class");
    }
    
    @Test
    public void neverExecuted()
        throws Exception
    {
        
    }
}
