package junit.ignore;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class NormalClassWithThreeIgnoredMethodsAnd1AssumptionFailureTest
{

    @Ignore
    @Test
    public void testWithIgnore1()
    {
    }

    @Ignore("Ignorance is bliss2")
    @Test
    public void testWithIgnore2()
    {
    }

    @Ignore("Ignorance \"is\' <>bliss2")
    @Test
    public void testWithQuotesInIgnore()
    {
    }

    @Test
    public void testWithAssumptionFailure()
    {
        Assume.assumeNotNull( new Object[]{ null} );
    }

    @Test
    public void testAllGood()
    {
        System.out.println("testAllGood");
    }

}

