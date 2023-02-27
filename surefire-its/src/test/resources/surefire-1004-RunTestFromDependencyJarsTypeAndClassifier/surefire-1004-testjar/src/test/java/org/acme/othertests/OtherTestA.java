package org.acme.othertests;

import static org.junit.Assert.*;
import org.junit.Test;

public class OtherTestA
{

    @Test
    public void shouldNotRun()
    {
        fail( "This test should not run" );
    }
}
