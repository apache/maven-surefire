package jiras.surefire1098;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Tests are executet in one jvm in parallel,
 * so we can use simple synchronization.
 */
class Locker {
    static BlockingQueue<String> QUEUE = new ArrayBlockingQueue<>( 3 );
}
