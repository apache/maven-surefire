package listenReport;

import java.util.List;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;

public class Reporter
    implements IReporter
{

    public void generateReport( List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory )
    {
        FileHelper.writeFile( "reporter-output.txt", "This is a reporter" );
    }

}
