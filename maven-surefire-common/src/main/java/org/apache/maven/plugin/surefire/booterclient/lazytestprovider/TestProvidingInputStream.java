package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class TestProvidingInputStream extends InputStream {
	private final Queue<String> testItemQueue;
	private byte[] currentBuffer;
	private int currentPos;
	private Semaphore semaphore = new Semaphore(0);
	private FlushReceiverProvider flushReceiverProvider;

	public TestProvidingInputStream(Queue<String> testItemQueue) {
		this.testItemQueue = testItemQueue;
	}

	public void setFlushReceiverProvider(FlushReceiverProvider flushReceiverProvider) {
		this.flushReceiverProvider = flushReceiverProvider;
	}

	@Override
	public synchronized int read() throws IOException {
		if (null == currentBuffer) {
			if (null != flushReceiverProvider && null != flushReceiverProvider.getFlushReceiver()) {
				flushReceiverProvider.getFlushReceiver().flush();
			}
			
			semaphore.acquireUninterruptibly();

			String currentElement = testItemQueue.poll();
			if (null != currentElement) {
				currentBuffer = currentElement.getBytes();
				currentPos = 0;
			} else {
				return -1;
			}
		}

		if (currentPos < currentBuffer.length) {
			return (currentBuffer[currentPos++] & 0xff);
		} else {
			currentBuffer = null;
			return ('\n' & 0xff);
		}
	}

	public void provideNewTest() {
		semaphore.release();
	}
}