package com.example.demo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

public class JUnitPlatformSampleTest
{

    @Test
    public void sampleTest()
    {
        Logger.getLogger( getClass().getName() ).info( "running test" );
        Assertions.assertTrue( true );
    }
}
