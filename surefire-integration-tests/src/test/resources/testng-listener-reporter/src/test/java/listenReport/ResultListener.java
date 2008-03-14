package listenReport;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.internal.IResultListener;


public class ResultListener
    implements IResultListener
{

    public void onFinish( ITestContext context )
    {

    }

    public void onStart( ITestContext context )
    {
        FileHelper.writeFile( "resultlistener-output.txt", "This is a result listener" );
    }

    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {

    }

    public void onTestFailure( ITestResult result )
    {

    }

    public void onTestSkipped( ITestResult result )
    {


    }

    public void onTestStart( ITestResult result )
    {


    }

    public void onTestSuccess( ITestResult result )
    {


    }

    public void onConfigurationFailure( ITestResult itr )
    {


    }

    public void onConfigurationSkip( ITestResult itr )
    {


    }

    public void onConfigurationSuccess( ITestResult itr )
    {


    }

}
