package surefireparallel;

import org.junit.Test;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class Waiting5Test
{
    @Test
    public void a()
        throws InterruptedException
    {
        Thread.sleep( 300L );
    }

    @Test
    public void b()
        throws InterruptedException
    {
        Thread.sleep( 300L );
    }

    @Test
    public void c()
        throws InterruptedException
    {
        Thread.sleep( 300L );
    }
}
