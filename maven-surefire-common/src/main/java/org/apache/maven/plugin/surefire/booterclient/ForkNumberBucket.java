package org.apache.maven.plugin.surefire.booterclient;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bucket from which fork numbers can be drawn. Any drawn number needs to be returned to the bucket, in order to keep
 * the range of provided values delivered as small as possible.
 * 
 * @author Andreas Gudian
 */
public class ForkNumberBucket
{

    private static final ForkNumberBucket INSTANCE = new ForkNumberBucket();

    private Queue<Integer> qFree = new ConcurrentLinkedQueue<Integer>();

    private AtomicInteger highWaterMark = new AtomicInteger( 1 );

    /**
     * Non-public constructor
     */
    protected ForkNumberBucket()
    {
    }

    /**
     * @return a fork number that is not currently in use. The value must be returned to the bucket using
     *         {@link #returnNumber(int)}.
     */
    public static int drawNumber()
    {
        return getInstance()._drawNumber();
    }

    /**
     * @param number the number to return to the bucket so that it can be reused.
     */
    public static void returnNumber( int number )
    {
        getInstance()._returnNumber( number );
    }

    /**
     * @return a singleton instance
     */
    private static ForkNumberBucket getInstance()
    {
        return INSTANCE;
    }

    /**
     * @return a fork number that is not currently in use. The value must be returned to the bucket using
     *         {@link #returnNumber(int)}.
     */
    protected int _drawNumber()
    {
        Integer nextFree = qFree.poll();

        if ( null == nextFree )
        {
            return highWaterMark.getAndIncrement();
        }
        else
        {
            return nextFree.intValue();
        }
    }

    /**
     * @return the highest number that has been drawn
     */
    protected int getHighestDrawnNumber()
    {
        return highWaterMark.get() - 1;
    }

    /**
     * @param number the number to return to the bucket so that it can be reused.
     */
    protected void _returnNumber( int number )
    {
        qFree.add( Integer.valueOf( number ) );
    }
}
