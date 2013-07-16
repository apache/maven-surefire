package junit.surefire697;

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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest
    extends TestCase
{

    public void testShort()
    {
        throw new RuntimeException( "A very short message" );
    }

    public void testShortMultiline()
    {
        throw new RuntimeException( "A very short multiline message\nHere is line 2" );
    }

    public void testLong()
    {
        throw new RuntimeException( "A very long single line message"
            + "012345678900123456789001234567890012345678900123456789001234567890"
            + "012345678900123456789001234567890012345678900123456789001234567890" );
    }

    public void testLongMultiLineNoCr()
    {
        throw new RuntimeException( "A very long multi line message"
            + "012345678900123456789001234567890012345678900123456789001234567890"
            + "012345678900123456789001234567890012345678900123456789001234567890\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ" );
    }

    public void testLongMultiLine()
    {
        throw new RuntimeException( "A very long single line message"
            + "012345678900123456789001234567890012345678900123456789001234567890"
            + "012345678900123456789001234567890012345678900123456789001234567890\n"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ\n" );
    }
}
