package org.codehaus.surefire.report;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.surefire.util.StringUtils;


/**
 * XML format reporter.
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id: XMLReporter.java 61 2005-10-07 04:07:33Z jruiz $
 */
public class XMLReporter 
    extends AbstractReporter
{
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat( "##0.00" );

    private PrintWriter writer;
    
    private Xpp3Dom testSuite;
    
    private Xpp3Dom testCase;
    
    private long batteryStartTime;
    
    public void setTestCase( Xpp3Dom testCase )
    {
        this.testCase = testCase;
    }

    public Xpp3Dom getTestCase()
    {
        return testCase;
    }

    public void runStarting( int testCount )
    {

    }

    public void batteryStarting( ReportEntry report )
        throws Exception
    {   
        batteryStartTime = System.currentTimeMillis();
        
        File reportFile = new File( getReportsDirectory(),  "TEST-" + report.getName() +  ".xml" );

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();
        
        writer = new PrintWriter( new FileWriter( reportFile ) );
        
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        
        testSuite = new Xpp3Dom("testsuite");
         
        testSuite.setAttribute("name",  report.getName());
        
        showProperties();
    }

    public void batteryCompleted( ReportEntry report )
    {   
        testSuite.setAttribute("tests", String.valueOf(this.getNbTests()) );
        
        testSuite.setAttribute("errors", String.valueOf(this.getNbErrors()) );
        
        testSuite.setAttribute("failures", String.valueOf(this.getNbFailures()) );
        
        long runTime = System.currentTimeMillis() - this.batteryStartTime;
        
        testSuite.setAttribute("time", elapsedTimeAsString( runTime ));
        
        try
        {   
            Xpp3DomWriter.write( writer, testSuite );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public void testStarting( ReportEntry report )
    {
        super.testStarting(report);
        
        String reportName;
        
        if ( report.getName().indexOf( "(" ) > 0 )
        {
            reportName = report.getName().substring( 0, report.getName().indexOf( "(" ) );
        }
        else
        {
            reportName = report.getName();
        }
        
        testCase = createElement(testSuite, "testcase");
        
        testCase.setAttribute("name", reportName);
    }

    public void testSucceeded( ReportEntry report )
    {
        super.testSucceeded(report);
        
        long runTime = this.endTime - this.startTime;
        
        testCase.setAttribute("time", elapsedTimeAsString( runTime ));
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError(report, stdOut, stdErr);
        
        String stackTrace = getStackTrace(report);
        
        Xpp3Dom error = createElement (testCase, "error");
        
        Throwable t = report.getThrowable();

        if ( t != null )
        {

            String message = t.getMessage();

            if ( ( message != null ) && ( message.trim().length() > 0 ) )
            {

                message = StringUtils.replace( report.getThrowable().getMessage(), "<", "&lt;" );

                message = StringUtils.replace( message, ">", "&gt;" );

                message = StringUtils.replace( message, "\"", "&quot;" );

                error.setAttribute( "message", message );

                error.setAttribute( "type", stackTrace.substring( 0, stackTrace.indexOf( ":" ) ) );
            }
            
            else
            {
                error.setAttribute( "type", new StringTokenizer( stackTrace ).nextToken() );
            }

        }

        error.setValue( stackTrace );
        
        error.setValue(stackTrace);
        
        createElement(testCase, "system-out").setValue(stdOut);
        
        long runTime = endTime - startTime;
        
        testCase.setAttribute("time", elapsedTimeAsString( runTime ));
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed(report,stdOut,stdErr);
        
        String stackTrace = getStackTrace(report);
        
        Xpp3Dom failure = createElement (testCase, "failure");
        
        Throwable t = report.getThrowable();

        if ( t != null )
        {

            String message = t.getMessage();

            if ( ( message != null ) && ( message.trim().length() > 0 ) )
            {

                message = StringUtils.replace( report.getThrowable().getMessage(), "<", "&lt;" );

                message = StringUtils.replace( message, ">", "&gt;" );

                message = StringUtils.replace( message, "\"", "&quot;" );

                failure.setAttribute( "message", message );

                failure.setAttribute( "type", stackTrace.substring( 0, stackTrace.indexOf( ":" ) ) );
            }
            else
            {
                failure.setAttribute( "type", new StringTokenizer( stackTrace ).nextToken() );
            }
        }
               
        failure.setValue(getStackTrace(report));

        createElement(testCase, "system-out").setValue(stdOut);
        
        createElement(testCase, "system-err").setValue(stdErr);
        
        long runTime = endTime - startTime;
        
        testCase.setAttribute("time", elapsedTimeAsString( runTime ));
    }

    public void dispose()
    {
        errors = 0;
        
        failures = 0;
        
        completedCount = 0;       
    }
    
    private Xpp3Dom createElement( Xpp3Dom element, String name )
    {
        Xpp3Dom component = new Xpp3Dom( name );
        
        element.addChild( component );
        
        return component;
    }
    /**
     * Returns stacktrace as String.
     * @param report ReportEntry object. 
     * @return stacktrace as string. 
     */
    private String getStackTrace(ReportEntry report)
    {   
        StringWriter writer = new StringWriter();
        
        report.getThrowable().printStackTrace(new PrintWriter(writer));
      
        writer.flush();
        
        return writer.toString();
    }
    
    /**
     * Adds system properties to the XML report.
     *
     */
    private void showProperties()
    {
        Xpp3Dom properties = createElement(testSuite,"properties");
        
        Xpp3Dom property; 
        
        Properties systemProperties = System.getProperties();
                
        if ( systemProperties != null )
        {
            Enumeration propertyKeys = systemProperties.propertyNames();
            
            while ( propertyKeys.hasMoreElements() )
            {
                String key = (String) propertyKeys.nextElement();
                
                property = createElement(properties,"property");
                
                property.setAttribute("name", key);
        
                property.setAttribute("value", systemProperties.getProperty( key ));
        
            }
        }
    }
    
    protected String elapsedTimeAsString( long runTime )
    {
        return TIME_FORMAT.format( (double) runTime / 1000 );
    }
}
