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
public class ByteBufferTest
    extends TestCase
{
    public void testAppend()
        throws Exception
    {
        ByteBuffer byteBuffer = new ByteBuffer( 30 );
        byteBuffer.append( 'C' );
        byteBuffer.append( (byte) 'D' );
        assertEquals( "CD", byteBuffer.toString() );

    }


    public void testJoin()
    {
        byte[] b1 = "ABC".getBytes();
        byte[] b2 = "DE".getBytes();
        final byte[] join = ByteBuffer.join( b1, 0, 3, b2, 0, 2 );
        assertEquals( 5, join.length );
    }
}
