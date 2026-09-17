package de.dagere.peass;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith( Parameterized.class )
public class ExampleTestJUnit4
{

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][] { { 0 }, { 1 } } );
    }

    int value;

    public ExampleTestJUnit4( int value )
    {
        this.value = value;
    }

    @Test
    public void test()
    {
        System.out.println( value );
    }

    @Test
    public void anotherTest()
    {
        System.out.println( "3 (and value ignored)" );
    }
}
