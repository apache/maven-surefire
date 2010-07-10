package org.apache.maven.surefire.junitcore;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class ConcurrentPrintStream
    extends PrintStream
{
    private final boolean isStdout;

    private final LogicalStream defaultStream = new  LogicalStream();


    ConcurrentPrintStream( boolean stdout )
    {
        super( new ByteArrayOutputStream() );
        isStdout = stdout;
    }


    public void write( byte[] buf, int off, int len )
    {

        final TestMethod threadTestMethod = TestMethod.getThreadTestMethod();
        if ( threadTestMethod != null )
        {
            threadTestMethod.getLogicalStream().write( isStdout, buf, off, len );
        }
        else
        {
            ( (ByteArrayOutputStream) out ).write( buf, off, len );
        }
    }

    public void writeTo(PrintStream printStream)
        throws IOException
    {
         ( (ByteArrayOutputStream) out ).writeTo(  printStream );
    }

    public void close()
    {

    }

    public void flush()
    {
    }


}
