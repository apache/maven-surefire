package consoleOutput;

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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

public class Test1
{
    @Test
    public void testSystemOut()
        throws IOException
    {
        PrintStream out = System.out;
        out.print( getS( "print" ));
        out.write( getS( "utf-8" ).getBytes( Charset.forName( "UTF-8" ) ) );
        out.write( getS( "8859-1" ).getBytes( Charset.forName( "ISO-8859-1" ) ) );
        out.write( getS( "utf-16" ).getBytes( Charset.forName( "UTF-16" ) ) );
    }

    private String getS( String s )
    {
        return " Hell\u00d8 " + s + "\n";
    }
}
