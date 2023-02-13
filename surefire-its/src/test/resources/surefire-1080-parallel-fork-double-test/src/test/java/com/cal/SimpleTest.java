package com.cal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleTest
{

    /**
     * Make sure the universe hasn't broken.
     */
    @Test
    public void testAddition()
    {
        assertEquals( 2, 1 + 1 );
    }

    /**
     * Now try to break the universe :D
     */
    @Test(expected = ArithmeticException.class)
    public void testDivision()
    {
        int i = 1 / 0;
    }
}
