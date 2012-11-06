package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.Commandline;



public class ProcessAwareCommandline extends Commandline implements FlushReceiverProvider {
	private final class OutputStreamFlushReceiver implements FlushReceiver {
		private final OutputStream outputStream;

		private OutputStreamFlushReceiver(OutputStream outputStream) {
			this.outputStream = outputStream;
		}

		public void flush() throws IOException {
			outputStream.flush();
		}
	}

	private FlushReceiver flushReceiver;
	
	@Override
	public Process execute() throws CommandLineException {
		Process process = super.execute();

		if (process.getOutputStream() != null) {
			flushReceiver = new OutputStreamFlushReceiver(process.getOutputStream());
		}
		
		return process;
	}

	public FlushReceiver getFlushReceiver() {
		return flushReceiver;
	}

}