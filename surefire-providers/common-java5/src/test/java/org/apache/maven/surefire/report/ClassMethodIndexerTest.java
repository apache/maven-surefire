package org.apache.maven.surefire.report;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class ClassMethodIndexerTest
    extends TestCase
{
    public void testNPE()
    {
        ClassMethodIndexer indexer = new ClassMethodIndexer();
        try
        {
            indexer.indexClass( null );
            fail( "NPE expected" );
        }
        catch ( NullPointerException e )
        {
            // expected
        }
    }

    public void testClass()
    {
        ClassMethodIndexer indexer = new ClassMethodIndexer();
        long index = indexer.indexClass( getClass().getName() );
        assertThat( index )
            .isEqualTo( 0x0000000100000000L );
    }

    public void testClassMethod()
    {
        ClassMethodIndexer indexer = new ClassMethodIndexer();
        long index = indexer.indexClassMethod( getClass().getName(), "methodName" );
        assertThat( index )
            .isEqualTo( 0x0000000100000001L );
    }

    public void testRun()
    {
        ClassMethodIndexer indexer = new ClassMethodIndexer();
        long index = indexer.indexClass( getClass().getName() );
        indexer.indexClass( "dummy" );
        assertThat( index )
            .isEqualTo( 0x0000000100000000L );
        index = indexer.indexClassMethod( getClass().getName(), "methodName" );
        assertThat( index )
            .isEqualTo( 0x0000000100000001L );

    }
}
