package testng.parallelOrdering;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Base
{
    private static final AtomicInteger resources = new AtomicInteger();

    // This simulates resource allocation
    @BeforeMethod
    public void setupAllocateResources()
    {
        int concurrentResources = resources.incrementAndGet();
        System.out.println("setupAllocateResources: " + concurrentResources);
        if (concurrentResources > 2) {
            throw new IllegalStateException("Tests execute in two threads, so there should be at most 2 resources allocated, got: " + concurrentResources);
        }
    }

    // This simulates freeing resources
    @AfterMethod(alwaysRun = true)
    public void tearDownReleaseResources()
    {
        System.out.println("tearDownReleaseResources: "  + resources.decrementAndGet());
    }

    @Test
    public void test1()
        throws Exception
    {
        sleepShortly("test1");
    }

    @Test
    public void test2()
        throws Exception
    {
        sleepShortly("test2");
    }

    @Test
    public void test3()
        throws Exception
    {
        sleepShortly("test3");
    }

    // Sleep random time to let tests interleave. Keep sleep short not to extend tests duration too much.
    private void sleepShortly(String method)
        throws InterruptedException
    {
        System.out.println("Sleep shortly:" + method + ", " + Thread.currentThread().getName() + " " + getClass().getSimpleName());
        Thread.sleep(ThreadLocalRandom.current().nextInt(3));
    }
}
