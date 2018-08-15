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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.transform.stream.StreamSource;

import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.fest.assertions.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.ValidationProblem;
import org.xmlunit.validation.ValidationResult;
import org.xmlunit.validation.Validator;

public class SurefireSchemaValidationTest
{

    @Test
    public void validate_XMLs_against_schema()
        throws Exception
    {
        File basedir = getProjectBasedir();

        File xsd = getSchemaFile( basedir );
        Assert.assertTrue( "XSD schema validation not found", xsd.exists() );

        // looks for all xml surefire report in test resources
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( basedir );
        ds.setIncludes( "**/TEST-*.xml" );
        ds.scan();

        String[] xmlFiles = ds.getIncludedFiles();
        Assertions.assertThat( xmlFiles ).describedAs( "No XML surefire reports found to validate" ).isNotEmpty();

        Validator v = Validator.forLanguage( Languages.W3C_XML_SCHEMA_NS_URI );
        v.setSchemaSource( new StreamSource( xsd ) );

        for ( String xmlFile : xmlFiles )
        {
            ValidationResult vr = v.validateInstance( new StreamSource( new File( basedir, xmlFile ) ) );
            StringBuilder msg = new StringBuilder();
            if ( !vr.isValid() )
            {
                msg.append( xmlFile ).append( " has violations:" );
                for ( ValidationProblem problem : vr.getProblems() )
                {
                    msg.append( "\n" ) //
                       .append( " - " ).append( problem.getType() ) //
                       .append( " at row:" ).append( problem.getLine() ) //
                       .append( " col:" ).append( problem.getColumn() ) //
                       .append( ' ' ).append( problem.getMessage() );
                }
            }
            Assert.assertTrue( Utils.toSystemNewLine( msg.toString() ), vr.isValid() );
        }
    }

    private File getProjectBasedir()
        throws URISyntaxException
    {
        // get the root path of test-classes
        URL basedirURL = SurefireSchemaValidationTest.class.getClassLoader().getResource( "." );
        return new File( basedirURL.toURI() );
    }

    private File getSchemaFile( File basedir )
        throws IOException
    {
        // get the schema file placed in a different module
        Path xsd = Paths.get( basedir.getAbsolutePath(), "..", "..", "..", "maven-surefire-plugin", "src", "site",
                              "resources", "xsd", "surefire-test-report.xsd" );
        return xsd.toFile().getCanonicalFile();
    }
}
