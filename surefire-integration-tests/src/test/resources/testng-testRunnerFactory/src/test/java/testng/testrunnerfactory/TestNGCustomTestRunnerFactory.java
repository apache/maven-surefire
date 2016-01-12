package testng.testrunnerfactory;

import org.testng.ISuite;
import org.testng.ITestRunnerFactory;
import org.testng.TestRunner;
import org.testng.xml.XmlTest;
//import org.testng.IInvokedMethodListener;

import java.util.List;

public class TestNGCustomTestRunnerFactory
    implements ITestRunnerFactory
{

    public TestRunner newTestRunner( ISuite suite, XmlTest test/*, List<IInvokedMethodListener> listeners*/ )
    {
        FileHelper.writeFile( "testrunnerfactory-output.txt",
                              "Instantiated Test Runner for suite:\n\t" + suite
                                  + "\nand test:\n\t" + test +"\n\n" );
        return new TestRunner( suite, test, test.skipFailedInvocationCounts()/*, listeners*/ );
    }
}
