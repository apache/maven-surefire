package org.apache.maven.surefire.providerapi;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Simple test for {@link ProviderDetectorRequest}
 *
 * @author Slawomir Jaranowski
 */
public class ProviderDetectorRequestTest
{

    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void invalidAutoDetectMultipleFrameworks()
    {
        ProviderDetectorRequest request = new ProviderDetectorRequest();

        e.expect( IllegalArgumentException.class );
        request.setMultipleFrameworks( "invalidValue" );
    }

    @Test
    public void nullMultipleFrameworks()
    {
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setMultipleFrameworks( null );

        assertThat( request.isWarnOnMultipleFrameworks() ).isFalse();
        assertThat( request.isFailOnMultipleFrameworks() ).isFalse();
    }

    @Test
    public void defaultMultipleFrameworks()
    {
        ProviderDetectorRequest request = new ProviderDetectorRequest();

        assertThat( request.isWarnOnMultipleFrameworks() ).isFalse();
        assertThat( request.isFailOnMultipleFrameworks() ).isFalse();
    }

    @Test
    public void warnOnMultipleFrameworks()
    {
        ProviderDetectorRequest request = new ProviderDetectorRequest();

        request.setMultipleFrameworks( "wArn" );

        assertThat( request.isWarnOnMultipleFrameworks() ).isTrue();
        assertThat( request.isFailOnMultipleFrameworks() ).isFalse();
    }

    @Test
    public void failOnMultipleFrameworks()
    {
        ProviderDetectorRequest request = new ProviderDetectorRequest();

        request.setMultipleFrameworks( "Fail" );

        assertThat( request.isWarnOnMultipleFrameworks() ).isFalse();
        assertThat( request.isFailOnMultipleFrameworks() ).isTrue();
    }
}
