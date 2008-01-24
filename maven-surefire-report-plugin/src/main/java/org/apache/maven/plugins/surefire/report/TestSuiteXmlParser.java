package org.apache.maven.plugins.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TestSuiteXmlParser
    extends DefaultHandler
{
    private ReportTestSuite suite;
    private NumberFormat numberFormat = NumberFormat.getInstance();

    /**
     * @noinspection StringBufferField
     */
    private StringBuffer currentElement;

    private ReportTestCase testCase;

    public ReportTestSuite parse( String xmlPath )
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser saxParser = factory.newSAXParser();

        saxParser.parse( new File( xmlPath ), this );
        
        return suite;
    }


    private int getAttributeAsInt( Attributes attributes, String name )
    {
        // may or may not exist
        String valueAsString = attributes.getValue( name );
        if ( valueAsString != null )
        {
            return Integer.parseInt( valueAsString );
        }
        return 0;
    }

    public void startElement( String uri, String localName, String qName, Attributes attributes )
        throws SAXException
    {
        try
        {
            if ( "testsuite".equals( qName ) )
            {
                suite = new ReportTestSuite();
                suite.setNumberOfErrors( getAttributeAsInt( attributes, "errors" ) );
                suite.setNumberOfFailures( getAttributeAsInt( attributes, "failures" ) );
                suite.setNumberOfSkipped( getAttributeAsInt( attributes, "skipped" ) );
                suite.setNumberOfTests( getAttributeAsInt( attributes, "tests" ) );

                Number time = numberFormat.parse( attributes.getValue( "time" ) );

                suite.setTimeElapsed( time.floatValue() );

                //check if group attribute is existing
                if ( attributes.getValue( "group" ) != null && !"".equals( attributes.getValue( "group" ) ) )
                {
                    String packageName = attributes.getValue( "group" );
                    suite.setPackageName( packageName );

                    String name = attributes.getValue( "name" );
                    suite.setName( name );

                    suite.setFullClassName( packageName + "." + name );
                }
                else
                {
                    String fullClassName = attributes.getValue( "name" );
                    suite.setFullClassName( fullClassName );

                    int lastDotPosition = fullClassName.lastIndexOf( "." );
                    
                    suite.setName( fullClassName.substring( lastDotPosition + 1, fullClassName.length() ) );
                    
                    if ( lastDotPosition < 0 )
                    {
                        /* no package name */
                        suite.setPackageName( "" );
                    }
                    else
                    {
                        suite.setPackageName( fullClassName.substring( 0, lastDotPosition ) );
                    }
                }

                suite.setTestCases( new ArrayList() );
            }
            else if ( "testcase".equals( qName ) )
            {
                currentElement = new StringBuffer();

                testCase = new ReportTestCase();

                testCase.setFullClassName( suite.getFullClassName() );

                testCase.setName( attributes.getValue( "name" ) );

                testCase.setClassName( suite.getName() );

                String timeAsString = attributes.getValue( "time" );

                Number time = new Integer( 0 );

                if ( timeAsString != null )
                {
                    time = numberFormat.parse( timeAsString );
                }

                testCase.setTime( time.floatValue() );

                testCase.setFullName( suite.getFullClassName() + "." + testCase.getName() );
            }
            else if ( "failure".equals( qName ) )
            {
                testCase.addFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
            }
            else if ( "error".equals( qName ) )
            {
                testCase.addFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
            }
            else if ( "skipped".equals( qName ) )
            {
                testCase.addFailure( "skipped", "skipped" ); // TODO extract real reasons
            }
        }
        catch ( ParseException e )
        {
            throw new SAXException( e.getMessage(), e );
        }
    }

    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( "testcase".equals( qName ) )
        {
            suite.getTestCases().add( testCase );
        }
        else if ( "failure".equals( qName ) )
        {
            Map failure = testCase.getFailure();

            failure.put( "detail", parseCause( currentElement.toString() ) );
        }
        else if ( "error".equals( qName ) )
        {
            Map error = testCase.getFailure();

            error.put( "detail", parseCause( currentElement.toString() ) );
        }
    }

    public void characters( char[] ch, int start, int length )
        throws SAXException
    {
        String s = new String( ch, start, length );

        if ( !"".equals( s.trim() ) )
        {
            currentElement.append( s );
        }
    }

    private List parseCause( String detail )
    {
        String fullName = testCase.getFullName();
        String name = fullName.substring( fullName.lastIndexOf( "." ) + 1 );
        return parseCause( detail, name );
    }

    private List parseCause( String detail, String compareTo )
    {
        StringTokenizer stringTokenizer = new StringTokenizer( detail, "\n" );
        List parsedDetail = new ArrayList( stringTokenizer.countTokens() );

        while ( stringTokenizer.hasMoreTokens() )
        {
            String lineString = stringTokenizer.nextToken().trim();
            parsedDetail.add( lineString );
            if ( lineString.indexOf( compareTo ) >= 0 )
            {
                break;
            }
        }

        return parsedDetail;
    }

}
