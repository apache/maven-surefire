package jiras.surefire1095;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class Listener
    extends RunListener
{
    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
        String described = description.getDisplayName();
        System.out.println( "testRunStarted " +
                                ( described == null || described.equals( "null" )
                                    ? description.getChildren()
                                    : description ) );
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        System.out.println("testRunFinished " +
            (result != null || result.equals("null")
                ? result.wasSuccessful()
                : result));
    }
}
