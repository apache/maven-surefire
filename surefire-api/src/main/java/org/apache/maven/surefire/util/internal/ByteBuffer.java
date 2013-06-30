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

/**
 * @author Kristian Rosenvold
 */
public class ByteBuffer
{
    private final byte[] data;

    private int position;

    public ByteBuffer( int length )
    {
        this.data = new byte[length];
    }

    public ByteBuffer( byte[] buf, int off, int len )
    {
        this.data = new byte[len];
        append( buf, off, len );
    }


    public void append( char chararcter )
    {
        data[position++] = (byte) chararcter;
    }

    public void append( byte chararcter )
    {
        data[position++] = chararcter;
    }

    private static final byte comma = (byte) ',';

    public void comma()
    {
        data[position++] = comma;
    }


    public void advance( int i )
    { // Oooh nice break of encapsulation
        position += i;
    }

    public void append( Integer integer )
    {
        toHex( integer );
    }

    /**
     * Convert the integer to an unsigned number.
     *
     * @param i the value
     */
    private void toHex( int i )
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

        append( buf, charPos, ( 32 - charPos ) );
    }

    private final static char[] digits =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };


    public byte[] getData()
    {
        return data;
    }

    public int getlength()
    {
        return position;
    }

    public String toString()
    {
        return new String( data, 0, position );
    }

    public static byte[] copy( byte[] src1, int off1, int len1 )
    {
        byte[] combined = new byte[len1];
        int pos = 0;
        for ( int i = off1; i < off1 + len1; i++ )
        {
            combined[pos++] = src1[i];
        }
        return combined;
    }

    void append( byte[] src1, int off1, int len1 )
    {
        for ( int i = off1; i < off1 + len1; i++ )
        {
            data[position++] = src1[i];
        }
    }

    public static byte[] join( byte[] src1, int off1, int len1, byte[] src2, int off2, int len2 )
    {
        byte[] combined = new byte[len1 + len2];
        int pos = 0;
        for ( int i = off1; i < off1 + len1; i++ )
        {
            combined[pos++] = src1[i];
        }
        for ( int i = off2; i < off2 + len2; i++ )
        {
            combined[pos++] = src2[i];
        }
        return combined;
    }

}
