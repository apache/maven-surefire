/**
 * 
 */
package org.apache.maven.surefire.util;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.apache.maven.shared.utils.cli.StreamPumper;
import org.apache.maven.surefire.booter.ForkingRunListener;

/**
 * A variant of TestsToRun that is provided with test class names asynchronously
 * from an {@link InputStream} (e.g. {@code System.in}). The method
 * {@link #iterator()} returns an Iterator that blocks on calls to
 * {@link Iterator#hasNext()} until new classes are available, or no more
 * classes will be available.
 * <p/>
 * The methods {@link #getLocatedClasses()} and {@link #size()} will throw an
 * {@link UnsupportedOperationException}.
 * 
 * @author Andreas Gudian
 * 
 */
public class LazyTestsToRun extends TestsToRun {
	private List workQueue = new ArrayList();
	private StreamPumper streamPumper;
	private boolean started = false;
	private boolean streamClosed = false;
	private ClassLoader testClassLoader;
	private PrintStream originalOutStream;

	public LazyTestsToRun(InputStream testSource, ClassLoader testClassLoader, PrintStream originalOutStream) {
		super(Collections.emptyList());

		this.testClassLoader = testClassLoader;
		this.originalOutStream = originalOutStream;

		streamPumper = new DoneReportingStreamPumper(testSource, new StreamConsumer() {
			public void consumeLine(String line) {
				addWorkItem(line);
			}
		});
	}

	protected void addWorkItem(String className) {
		synchronized (workQueue) {
			workQueue.add(ReflectionUtils.loadClass(testClassLoader, className));
			workQueue.notifyAll();
		}
	}

	private final class DoneReportingStreamPumper extends StreamPumper {
		private DoneReportingStreamPumper(InputStream in, StreamConsumer consumer) {
			super(in, consumer);
		}

		public void setDone() {
			super.setDone();
			synchronized (workQueue) {
				streamClosed = true;
				workQueue.notifyAll();
			}
		}
	}

	protected void nextTestRequired() {
		StringBuffer sb = new StringBuffer().append((char) ForkingRunListener.BOOTERCODE_NEXT_TEST).append(
				",0,want more!\n");
		originalOutStream.print(sb.toString());
	}

	private class BlockingIterator implements Iterator {
		private static final long TIMEOUT = 30000L;
		private int lastPos = -1;

		public boolean hasNext() {
			int nextPos = lastPos + 1;
			synchronized (workQueue) {
				if (workQueue.size() > nextPos) {
					return true;
				} else {
					if (needsToWaitForInput(nextPos)) {
						// request new test
						nextTestRequired();

						long waitUntilTime = System.currentTimeMillis() + TIMEOUT;
						
						// wait for the queue to get filled, the stream to
						// get closed, to our timeout to run up
						while (needsToWaitForInput(nextPos) && !hasWaitedLongEnough(waitUntilTime)) {
							safeWait(workQueue);
						}
					}

					return (workQueue.size() > nextPos);
				}
			}
		}

		private boolean hasWaitedLongEnough(long waitUntilTime) {
			return System.currentTimeMillis() > waitUntilTime;
		}

		private boolean needsToWaitForInput(int nextPos) {
			return workQueue.size() == nextPos && !streamClosed;
		}

		private void safeWait(Object obj) {
			try {
				obj.wait(TIMEOUT);
			} catch (InterruptedException e) {
			}
		}

		public Object next() {
			synchronized (workQueue) {
				return workQueue.get(++lastPos);
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Iterator iterator() {
		startPumperIfNecessary();
		return new BlockingIterator();
	}

	protected void startPumperIfNecessary() {
		synchronized (streamPumper) {
			if (!started) {
				streamPumper.start();
				started = true;
			}
		}
	}

	/**
	 * Unsupported. Use {@link #iterator()} instead.
	 */
	public int size() {
		throw new UnsupportedOperationException("use method iterator()");
	}

	/**
	 * Unsupported. Use {@link #iterator()} instead.
	 */
	public Class[] getLocatedClasses() {
		throw new UnsupportedOperationException("use method iterator()");
	}

	public String toString() {
		startPumperIfNecessary();
		StringBuffer sb = new StringBuffer("LazyTestsToRun ");
		synchronized (workQueue) {
			sb.append("(more items expected: ").append(!streamClosed).append("): ");
			sb.append(workQueue);
		}

		return sb.toString();
	}

}
