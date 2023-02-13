package jiras.surefire1082;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith( Parameterized.class )
public class Jira1082Test
{
    private final int x;

    public Jira1082Test( int x )
    {
        this.x = x;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data()
    {
        return Arrays.asList( new Object[][]{ { 0 }, { 1 } } );
    }

    @Test
    public void a()
        throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep( 500 );
        System.out.println( getClass() + " a " + x + " " + Thread.currentThread().getName() );
    }

    @Test
    public void b()
        throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep( 500 );
        System.out.println( getClass() + " b " + x + " " + Thread.currentThread().getName() );
    }
}
