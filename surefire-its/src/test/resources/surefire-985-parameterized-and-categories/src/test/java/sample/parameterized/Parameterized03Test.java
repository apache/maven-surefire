package sample.parameterized;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import sample.CategoryActivated;

@Category( CategoryActivated.class )
@RunWith( Parameterized.class )
public class Parameterized03Test
{
    static
    {
        System.out.println( "Initializing Parameterized03Test" );
    }

    @Parameters
    public static Collection<Integer[]> getParams()
    {
        return Arrays.asList( new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }, new Integer[] { 4 } );
    }

    public Parameterized03Test( Integer param )
    {
    }

    @Test
    public void testNothing()
    {
    }

    @Test
    public void testNothingEither()
    {
    }
}
