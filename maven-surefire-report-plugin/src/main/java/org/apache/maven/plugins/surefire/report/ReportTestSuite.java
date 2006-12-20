package org.apache.maven.plugins.surefire.report;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ReportTestSuite
    extends DefaultHandler
{
    private List testCases;

    private int numberOfErrors;

    private int numberOfFailures;
    
    private int numberOfSkipped;

    private int numberOfTests;

    private String name;

    private String fullClassName;

    private String packageName;

    private float timeElapsed;

    private NumberFormat numberFormat = NumberFormat.getInstance();

    /**
     * @noinspection StringBufferField
     */
    private StringBuffer currentElement;

    private ReportTestCase testCase;

    public void parse( String xmlPath )
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser saxParser = factory.newSAXParser();

        saxParser.parse( new File( xmlPath ), this );
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
                numberOfErrors = getAttributeAsInt( attributes, "errors" );
                numberOfFailures = getAttributeAsInt( attributes, "failures" );
                numberOfSkipped = getAttributeAsInt( attributes, "skipped" );
                numberOfTests = getAttributeAsInt( attributes, "tests" );

                Number time = numberFormat.parse( attributes.getValue( "time" ) );

                timeElapsed = time.floatValue();

                //check if group attribute is existing
                if ( attributes.getValue( "group" ) != null && !"".equals( attributes.getValue( "group" ) ) )
                {
                    packageName = attributes.getValue( "group" );

                    name = attributes.getValue( "name" );

                    fullClassName = packageName + "." + name;
                }
                else
                {
                    fullClassName = attributes.getValue( "name" );

                    name = fullClassName.substring( fullClassName.lastIndexOf( "." ) + 1, fullClassName.length() );

                    int lastDotPosition = fullClassName.lastIndexOf( "." );
                    if ( lastDotPosition < 0 )
                    {
                        /* no package name */
                        packageName = "";
                    }
                    else
                    {
                        packageName = fullClassName.substring( 0, lastDotPosition );
                    }
                }

                testCases = new ArrayList();
            }
            else if ( "testcase".equals( qName ) )
            {
                currentElement = new StringBuffer();

                testCase = new ReportTestCase();

                testCase.setFullClassName( fullClassName );

                testCase.setName( attributes.getValue( "name" ) );

                testCase.setClassName( name );

                String timeAsString = attributes.getValue( "time" );

                Number time = new Integer( 0 );

                if ( timeAsString != null )
                {
                    time = numberFormat.parse( timeAsString );
                }

                testCase.setTime( time.floatValue() );

                testCase.setFullName( packageName + "." + name + "." + testCase.getName() );
            }
            else if ( "failure".equals( qName ) )
            {
                testCase.addFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
            }
            else if ( "error".equals( qName ) )
            {
                testCase.addFailure( attributes.getValue( "message" ), attributes.getValue( "type" ) );
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
            testCases.add( testCase );
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

        if ( ! "".equals( s.trim() ) )
        {
            currentElement.append( s );
        }
    }

    public List getTestCases()
    {
        return this.testCases;
    }

    public int getNumberOfErrors()
    {
        return numberOfErrors;
    }

    public void setNumberOfErrors( int numberOfErrors )
    {
        this.numberOfErrors = numberOfErrors;
    }

    public int getNumberOfFailures()
    {
        return numberOfFailures;
    }

    public void setNumberOfFailures( int numberOfFailures )
    {
        this.numberOfFailures = numberOfFailures;
    }
    
    public int getNumberOfSkipped()
    {
        return numberOfSkipped;
    }
    
    public void setNumberOfSkipped( int numberOfSkipped )
    {
        this.numberOfSkipped = numberOfSkipped;
    }

    public int getNumberOfTests()
    {
        return numberOfTests;
    }

    public void setNumberOfTests( int numberOfTests )
    {
        this.numberOfTests = numberOfTests;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getFName()
    {
        return name;
    }

    public void setFName( String name )
    {
        this.name = name;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setPackageName( String packageName )
    {
        this.packageName = packageName;
    }

    public float getTimeElapsed()
    {
        return this.timeElapsed;
    }

    public void setTimeElapsed( float timeElapsed )
    {
        this.timeElapsed = timeElapsed;
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

    public void setTestCases( List testCases )
    {
        this.testCases = Collections.unmodifiableList( testCases );
    }
}
