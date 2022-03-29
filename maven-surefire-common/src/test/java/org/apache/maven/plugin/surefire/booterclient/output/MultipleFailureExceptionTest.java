package org.apache.maven.plugin.surefire.booterclient.output;

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

import java.io.IOException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code MultipleFailureException}.
 */
public class MultipleFailureExceptionTest
{
    @Test
    public void test()
    {
        MultipleFailureException e = new MultipleFailureException();
        NullPointerException suppressed1 = new NullPointerException( "field is null" );
        IOException suppressed2 = new IOException( "read error" );
        e.addException( suppressed1 );
        e.addException( suppressed2 );

        assertThat( e.getMessage() )
            .contains( "field is null" )
            .contains( "read error" );

        assertThat( e.getLocalizedMessage() )
            .contains( "field is null" )
            .contains( "read error" );

        assertThat( e.getSuppressed() )
            .hasSize( 2 );

        assertThat( e.getSuppressed() )
            .contains( suppressed1, suppressed2 );

        assertThat( e.hasNestedExceptions() )
            .isTrue();
    }
}
