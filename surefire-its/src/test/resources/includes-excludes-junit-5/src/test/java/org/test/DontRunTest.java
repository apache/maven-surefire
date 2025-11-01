package org.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class DontRunTest
{
    @Test
    public void testRun()
    {
        assertEquals(true, false);
    }
}
