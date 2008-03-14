package listenReport;

import org.testng.ISuite;
import org.testng.ISuiteListener;

public class SuiteListener
    implements ISuiteListener
{

    public void onFinish( ISuite suite )
    {

    }

    public void onStart( ISuite suite )
    {
        FileHelper.writeFile( "suitelistener-output.txt", "This is a suite listener" );
    }

}
