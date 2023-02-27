package sample.parameterized;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import sample.CategoryNotSelected;

@RunWith( Parameterized.class )
@Category( CategoryNotSelected.class )
public class Parameterized01Test
{
    static
    {
        System.out.println( "Initializing Parameterized01Test" );
    }

    @Parameters
    public static Collection<Integer[]> getParams()
    {
        return Arrays.asList( new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }, new Integer[] { 4 } );
    }

    public Parameterized01Test( Integer param )
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
