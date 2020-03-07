package org.apache.maven.surefire.test;

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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FailingTest
{
    @Rule
    public TestName name = new TestName();

    @Test
    public void defaultTestValueIs_Value()
    {
        assertThat( new App().getTest(), equalTo( "wrong" ) );
    }

    @Test
    public void setTestAndRetrieveValue()
    {
        final App app = new App();
        final String val = "foo";

        app.setTest( val );

        assertThat( app.getTest(), equalTo( "bar" ) );
    }

    @After
    public void writeFile()
        throws IOException
    {
        final File f = new File( "target/tests-run", getClass().getName() + ".txt" );
        f.getParentFile().mkdirs();

        try ( FileWriter w = new FileWriter( f, true ) )
        {
            w.write( name.getMethodName() );
        }
    }
}
