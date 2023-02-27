package jiras.surefire1098;

import org.junit.Test;

public final class BTest {

    @Test
    public void someMethod() throws InterruptedException {
        System.out.println(getClass() + " " + Thread.currentThread().getName());
        Locker.QUEUE.put( "B" );
    }

}
