package org.sample.module;

import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Before;
import org.junit.Test;

public class My2Test {
    @Test
    public void fails()
    {
        fail( "Always fails" );
    }

    @Test
    public void alwaysSuccessful()
    {

    }
}
