package junit.surefire733;

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


import junit.framework.TestCase;

public class ATest
    extends TestCase
{
    public void testConsoleOut()
    {
        System.out.write( (int) 'a' );
        final byte[] bytes = "bc".getBytes();
        System.out.write( bytes, 0, bytes.length );
        System.out.write( '\n' );
        System.out.println( "ABC" );
        System.out.println( (String) null );

        final byte[] errbytes = "ef".getBytes();
        System.err.write( (int) 'z' );
        System.err.write( errbytes, 0, bytes.length );
        System.err.write( '\n' );

        System.err.println( "XYZ" );
        System.err.println( (String) null );

    }
}
