package runListener;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * {@link org.junit.runner.notification.RunListener} to generate an output file whose existence can be checked by surefire-integration.
 *
 * @author <a href="mailto:matthew.gilliard@gmail.com">Matthew Gilliard</a>
 */
public class EchoingRunListener
    extends RunListener
{

    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
        System.out.println("testRunStarted " + description);
    }

    @Override
    public void testRunFinished( Result result )
        throws Exception
    {
        System.out.println("testRunFinished " + result);
    }

    @Override
    public void testStarted( Description description )
        throws Exception
    {
        System.out.println("testStarted " + description);
    }

    @Override
    public void testFinished( Description description )
        throws Exception
    {
        System.out.println("testFinished " + description);
    }

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        System.out.println("testFailure " + failure);
    }

    @Override
    public void testIgnored( Description description )
        throws Exception
    {
        System.out.println("testIgnored " + description);
    }

    public void testAssumptionFailure( Failure failure )
    {
        System.out.println("testAssumptionFailure " + failure);
     }
}
