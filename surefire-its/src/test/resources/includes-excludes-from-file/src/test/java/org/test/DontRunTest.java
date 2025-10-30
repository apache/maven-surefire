package org.test;

import junit.framework.TestCase;
import org.junit.Test;

public class DontRunTest extends TestCase
{
    @Test
    public void testRun()
    {
        assertEquals(true, false);
    }
}
