package org.apache.maven.surefire.its.fixture;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kristian Rosenvold
 */
public class TestSuiteXmlParserTest
{
    @Test
    public void testParser()
        throws IOException, SAXException, ParserConfigurationException
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser();

        Collection<ReportTestSuite> oldResult =
            parser.parse( "src/test/resources/fixture/testsuitexmlparser/TEST-org.apache.maven.surefire.test.FailingTest.xml" );

        assertNotNull( oldResult);

        assertEquals( 1, oldResult.size() );
        ReportTestSuite next = oldResult.iterator().next();
        assertEquals( 2, next.getNumberOfTests() );


    }
}
