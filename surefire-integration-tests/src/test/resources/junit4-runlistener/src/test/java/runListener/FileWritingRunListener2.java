package runListener;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

/**
 * {@link RunListener} to generate an output file whose existence can be checked by surefire-integration.
 *
 * @author <a href="mailto:matthew.gilliard@gmail.com">Matthew Gilliard</a>
 */
public class FileWritingRunListener2
    extends RunListener
{

    @Override
    public void testStarted( Description description )
    {
        FileHelper.writeFile( "runlistener-output-2.txt", "This written by RunListener#testStarted()" );
    }

}
