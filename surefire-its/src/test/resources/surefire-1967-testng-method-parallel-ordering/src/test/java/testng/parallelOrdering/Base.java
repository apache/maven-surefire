package testng.parallelOrdering;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Base
{
    private static final AtomicInteger resources = new AtomicInteger();

    // This simulates resource allocation
    @BeforeClass
    public void setupAllocateResources()
    {
        int concurrentResources = resources.incrementAndGet();
        if (concurrentResources > 2) {
            throw new IllegalStateException("Tests execute in two threads, so there should be at most 2 resources allocated, got: " + concurrentResources);
        }
    }

    // This simulates freeing resources
    @AfterClass(alwaysRun = true)
    public void tearDownReleaseResources()
    {
        resources.decrementAndGet();
    }

    @Test
    public void test1()
            throws Exception
    {
        sleepShortly();
    }

    @Test
    public void test2()
            throws Exception
    {
        sleepShortly();
    }

    @Test
    public void test3()
            throws Exception
    {
        sleepShortly();
    }

    // Sleep random time to let tests interleave. Keep sleep short not to extend tests duration too much.
    private void sleepShortly()
            throws InterruptedException
    {
        Thread.sleep(ThreadLocalRandom.current().nextInt(3));
    }
}
