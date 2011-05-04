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

import org.apache.maven.surefire.util.NestedRuntimeException;

import java.io.IOException;
import java.io.OutputStream;

public class StreamUtils
{
    public static void toHex( OutputStream target, Integer i )
    {
        if ( i != null )
        {
            toHex( target, i.intValue() );
        }
    }

    /**
     * Convert the integer to an unsigned number.
     *
     * @param target The stream that will receive the encoded value
     * @param i      the value
     */
    public static void toHex( OutputStream target, int i )
    {
        byte[] buf = new byte[32];
        int charPos = 32;
        int radix = 1 << 4;
        int mask = radix - 1;
        do
        {
            buf[--charPos] = (byte) digits[i & mask];
            i >>>= 4;
        }
        while ( i != 0 );

        try
        {
            target.write( buf, charPos, ( 32 - charPos ) );
        } catch (IOException e)
        {
            throw new NestedRuntimeException(e);
        }
    }

    private final static char[] digits =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };


}
