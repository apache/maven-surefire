package org.apache.maven.surefire;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class SurefireBooterTest
    extends TestCase
{

    public void testSplit()
    {
        String s, d;
        d = ":";
        s = "x1:y2:z;j:f";
        List list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "x1", "y2", "z;j", "f" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "x1";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "x1" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "x1:";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "x1" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = ":";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Collections.EMPTY_LIST, list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        d = "::";
        s = "x1::y2::z;j::f";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "x1", "y2", "z;j", "f" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "x1";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "x1" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "x1::";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "x1" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { "" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = ":";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Arrays.asList( new String[] { ":" } ), list );
        assertEquals( Arrays.asList( s.split( d ) ), list );

        s = "::";
        list = Arrays.asList( SurefireBooter.split( s, d ) );
        assertEquals( Collections.EMPTY_LIST, list );
        assertEquals( Arrays.asList( s.split( d ) ), list );
    }

}
