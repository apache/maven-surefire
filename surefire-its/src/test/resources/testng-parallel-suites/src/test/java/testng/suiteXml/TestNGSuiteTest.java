package testng.suiteXml;

import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class TestNGSuiteTest {
	private static final AtomicInteger COUNTER = new AtomicInteger();

	@Test
	public void shouldRunAndPrintItself()
		throws Exception
	{
		System.out.println( getClass().getSimpleName()
				+ "#shouldRunAndPrintItself() "
				+ COUNTER.incrementAndGet()
				+ "." );

		TimeUnit.SECONDS.sleep( 2 );
	}
}
