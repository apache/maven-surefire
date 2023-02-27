package jiras.surefire1098;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public final class DTest {

    @Test
    public void someMethod() throws InterruptedException {
        System.out.println(getClass() + " " + Thread.currentThread().getName());
        // wait from result from 3 tests
        // so test will be the longest and finish as last
        Locker.QUEUE.take();
        Locker.QUEUE.take();
        Locker.QUEUE.take();
        // and sleep a little
        TimeUnit.MILLISECONDS.sleep(100);
    }

}
