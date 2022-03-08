package org.apache.maven.surefire.its.jiras;

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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@RunWith( Parameterized.class )
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire946KillMainProcessInReusableForkIT
    extends SurefireJUnit4IntegrationTestCase
{
    // there are 10 test classes that each would wait 3.5 seconds.
    private static final int TEST_SLEEP_TIME = 3_500;

    private String classifierOfDummyDependency;

    @Parameter
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String shutdownMavenMethod;

    @Parameter( 1 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String shutdownSurefireMethod;

    @Parameters( name = "{0}-{1}" )
    public static Iterable<Object[]> data()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] { "halt", "exit" } );
        args.add( new Object[] { "halt", "kill" } );
        args.add( new Object[] { "exit", "exit" } );
        args.add( new Object[] { "exit", "kill" } );
        args.add( new Object[] { "interrupt", "exit" } );
        args.add( new Object[] { "interrupt", "kill" } );
        return args;
    }

    @BeforeClass
    public static void installSelfdestructPlugin()
    {
        unpack( Surefire946KillMainProcessInReusableForkIT.class, "surefire-946-self-destruct-plugin", "plugin" )
            .executeInstall();
    }

    @Before
    public void dummyDep()
    {
        classifierOfDummyDependency = shutdownMavenMethod + shutdownSurefireMethod;
        unpack( "surefire-946-dummy-dependency", classifierOfDummyDependency )
            .sysProp( "distinct.classifier", classifierOfDummyDependency )
            .executeInstall();
    }

    @Test( timeout = 60_000 )
    public void test() throws Exception
    {
        unpack( "surefire-946-killMainProcessInReusableFork",
                "-" + shutdownMavenMethod + "-" + shutdownSurefireMethod )
                .sysProp( "distinct.classifier", classifierOfDummyDependency )
                .sysProp( "surefire.shutdown", shutdownSurefireMethod )
                .sysProp( "selfdestruct.timeoutInMillis", "20000" )
                .sysProp( "selfdestruct.method", shutdownMavenMethod )
                .sysProp( "testSleepTime", String.valueOf( TEST_SLEEP_TIME ) )
                .addGoal( "org.apache.maven.plugins.surefire:maven-selfdestruct-plugin:selfdestruct" )
                .setForkJvm()
                .forkPerThread().threadCount( 1 ).reuseForks( true ).maven().withFailure().executeTest();


        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        File settings = new File( System.getProperty( "maven.settings.file" ) ).getCanonicalFile();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( settings );
        String localRepository = xpath.evaluate( "/settings/localRepository", doc );
        assertThat( localRepository )
                .isNotNull()
                .isNotEmpty();

        File dep = new File( localRepository,
                "org/apache/maven/plugins/surefire/surefire-946-dummy-dependency/0.1/"
                        + "surefire-946-dummy-dependency-0.1-" + classifierOfDummyDependency + ".jar" );

        assertThat( dep )
                .exists();

        boolean deleted;
        int iterations = 0;
        do
        {
            Thread.sleep( 1_000L );
            deleted = dep.delete();
        }
        while ( !deleted && ++iterations < 10 );
        assertThat( deleted )
                .isTrue();
    }
}
