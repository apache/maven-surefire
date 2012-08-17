package smallresultcounting;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

public class Test1
{
    @Test
    public void testWithFailingAssumption1()
    {
        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption2()
    {
        try
        {
            Thread.sleep( 150 );
        }
        catch ( InterruptedException ignore )
        {
        }

        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption3()
    {
        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption4()
    {
        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption5()
    {
        assumeThat( 2, is( 3 ) );
    }
}