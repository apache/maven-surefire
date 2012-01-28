package org.apache.maven.surefire.booter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:kristian.rosenvold@gmail.com">Kristian Rosenvold</a>
 */
public class ForkingRunListenerTest
    extends TestCase
{
    public void testInfo()
        throws Exception
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream target = new PrintStream( byteArrayOutputStream );
        ForkingRunListener forkingRunListener = new ForkingRunListener( target, 1, true );
        forkingRunListener.info( new String( new byte[]{ 65 } ) );
        forkingRunListener.info( new String( new byte[]{ } ) );

    }
}
