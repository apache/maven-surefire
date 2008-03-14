package charsetProvider;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

public class MSUREFIRE77TestCase extends TestCase
{
	public void testThatICanUseCharsets() throws UnsupportedEncodingException
	{
		System.out.println( new String("foo".getBytes(), "GSM_0338"));
	}

        public static void main(String[] args) throws Exception { new MSUREFIRE77TestCase().testThatICanUseCharsets(); }
}
