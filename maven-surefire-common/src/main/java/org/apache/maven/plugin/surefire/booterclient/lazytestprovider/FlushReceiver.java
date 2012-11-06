package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

import java.io.IOException;

public interface FlushReceiver {
	void flush() throws IOException;
}
