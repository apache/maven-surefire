package debug;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ParallelTest
{
    private static final AtomicInteger concurrency = new AtomicInteger();

    private static final AtomicInteger counter = new AtomicInteger();

    @DataProvider( parallel = true, name = "dataProvider" )
    public Iterator<Object[]> dataProvider()
    {
        List<Object[]> data = new ArrayList<Object[]>();
        for ( int i = 0; i < 5000; i++ )
        {
            data.add( new Object[]{ "ID_" + i } );
        }
        return data.iterator();
    }

    @Test( dataProvider = "dataProvider" )
    public void testParallelDataProvider( String iterId )
        throws Exception
    {
        int methodCount = counter.incrementAndGet();
        int currentlyParallelCalls = concurrency.incrementAndGet();
        if ( methodCount % 100 == 0 )
        {
            System.out.println( iterId + ": CONCURRENCY=" + currentlyParallelCalls + "." );
        }
        TimeUnit.MILLISECONDS.sleep( 20 );
        concurrency.decrementAndGet();
    }
}