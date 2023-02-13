package surefireparallelnts;

import org.junit.Test;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
@net.jcip.annotations.NotThreadSafe
public class RunningInSequenceTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        System.out.println( "xxx-" + Thread.currentThread().getName() );
    }
}
