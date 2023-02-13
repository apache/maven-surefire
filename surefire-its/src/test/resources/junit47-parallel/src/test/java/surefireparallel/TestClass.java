package surefireparallel;

import org.junit.Test;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.16
 */
public class TestClass
{
    @Test
    public void a()
        throws InterruptedException
    {
        Thread.sleep( 5000L );
    }

    @Test
    public void b()
        throws InterruptedException
    {
        Thread.sleep( 5000L );
    }

    @Test
    public void c()
        throws InterruptedException
    {
        Thread.sleep( 5000L );
    }
}
