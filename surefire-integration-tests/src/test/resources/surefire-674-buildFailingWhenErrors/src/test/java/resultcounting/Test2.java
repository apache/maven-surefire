package resultcounting;

import org.junit.Test;

/**
 * A test that causes an error
 * 
 * @author Kristian Rosenvold
 */
public class Test2
{
    @Test
    public void testWithException1()
    {
        throw new RuntimeException( "We expect this" );
    }
}