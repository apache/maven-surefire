package org.apache.maven.surefire.util.internal;

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

/**
 * @author Kristian Rosenvold
 */
public class StringUtilsTest
    extends TestCase
{

    public void testUnescape()
    {
        byte[] buffer = new byte[80];
        final int abc = StringUtils.unescapeJava( buffer, "ABC" );
        assertEquals( 3, abc );
    }

    public void testUnescapeWithEscape()
    {
        byte[] buffer = new byte[80];
        final int abc = StringUtils.unescapeJava( buffer, "AB\tC" );
        assertEquals( 4, abc );
    }

    public void testEscape()
    {
        ByteBuffer buffer = new ByteBuffer( 80 );
        StringUtils.escapeJavaStyleString( buffer, "AB\tC".getBytes(), 0, 4 );
        assertEquals( 5, buffer.getlength() );
        String temp = buffer.toString();
        assertEquals( "AB\\tC", temp );

    }

}
