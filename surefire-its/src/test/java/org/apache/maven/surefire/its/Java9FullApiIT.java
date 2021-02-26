package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.AbstractJava9PlusIT;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 * Running Surefire on the top of JDK 9 and should be able to load
 * classes of multiple different Jigsaw modules without error.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class Java9FullApiIT
    extends AbstractJava9PlusIT
{
    @Test
    @SuppressWarnings( "checkstyle:methodname" )
    public void shouldLoadMultipleJavaModules_JavaHome() throws Exception
    {
        OutputValidator validator = assumeJava9()
                                            .setForkJvm()
                                            .debugLogging()
                                            .execute( "verify" )
                                            .verifyErrorFree( 1 );

        validator.verifyTextInLog( "loaded class java.sql.SQLException" )
                .verifyTextInLog( "loaded class javax.xml.ws.Holder" )
                .verifyTextInLog( "loaded class javax.xml.bind.JAXBException" )
                .verifyTextInLog( "loaded class javax.transaction.TransactionManager" )
                .verifyTextInLog( "loaded class javax.transaction.InvalidTransactionException" )
                .assertThatLogLine( is( "java.specification.version=" + (int) JAVA_VERSION ),
                                    greaterThanOrEqualTo( 1 ) );
    }

    @Test
    @SuppressWarnings( "checkstyle:methodname" )
    public void shouldLoadMultipleJavaModules_JvmParameter() throws Exception
    {
        OutputValidator validator = assumeJava9()
                                            .setForkJvm()
                                            .debugLogging()
                                            .execute( "verify" )
                                            .verifyErrorFree( 1 );

        validator.verifyTextInLog( "loaded class java.sql.SQLException" )
                .verifyTextInLog( "loaded class javax.xml.ws.Holder" )
                .verifyTextInLog( "loaded class javax.xml.bind.JAXBException" )
                .verifyTextInLog( "loaded class javax.transaction.TransactionManager" )
                .verifyTextInLog( "loaded class javax.transaction.InvalidTransactionException" )
                .assertThatLogLine( is( "java.specification.version=" + (int) JAVA_VERSION ),
                                    greaterThanOrEqualTo( 1 ) );
    }

    @Test
    @SuppressWarnings( "checkstyle:methodname" )
    public void shouldLoadMultipleJavaModules_ToolchainsXML() throws Exception
    {
        OutputValidator validator = assumeJava9()
                                            .setForkJvm()
                                            .activateProfile( "use-toolchains" )
                                            .addGoal( "--toolchains" )
                                            .addGoal( System.getProperty( "maven.toolchains.file" ) )
                                            .execute( "verify" )
                                            .verifyErrorFree( 1 );

        validator.verifyTextInLog( "loaded class java.sql.SQLException" )
                .verifyTextInLog( "loaded class javax.xml.ws.Holder" )
                .verifyTextInLog( "loaded class javax.xml.bind.JAXBException" )
                .verifyTextInLog( "loaded class javax.transaction.TransactionManager" )
                .verifyTextInLog( "loaded class javax.transaction.InvalidTransactionException" )
                .assertThatLogLine( is( "java.specification.version=" + (int) JAVA_VERSION ),
                                    greaterThanOrEqualTo( 1 ) );
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "java9-full-api";
    }
}
