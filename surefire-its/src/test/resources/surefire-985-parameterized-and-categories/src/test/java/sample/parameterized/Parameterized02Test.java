package sample.parameterized;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import sample.CategoryActivated;

@RunWith( Parameterized.class )
public class Parameterized02Test
{
    static
    {
        System.out.println( "Initializing Parameterized02Test" );
    }

    @Parameters
    public static Collection<Integer[]> getParams()
    {
        return Arrays.asList( new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }, new Integer[] { 4 } );
    }

    public Parameterized02Test( Integer param )
    {
    }

    @Test
    @Category( CategoryActivated.class )
    public void testNothing()
    {
    }

    @Test
    public void testNothingEither()
    {
    }
}
