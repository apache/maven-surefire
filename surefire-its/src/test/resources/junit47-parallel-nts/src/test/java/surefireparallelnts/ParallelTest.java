package surefireparallelnts;

import org.junit.Test;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
public class ParallelTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        String name = Thread.currentThread().getName();
        System.out.println( "maven-surefire-plugin@NotThreadSafe".equals( name ) ? "wrong-thread" : "expected-thread" );
    }
}
