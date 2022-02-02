package org.apache.maven.surefire.spi;

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

import org.apache.maven.surefire.providerapi.ServiceLoader;
import org.junit.Test;

import static java.lang.Thread.currentThread;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public class SPITest
{
    private final ServiceLoader spi = new ServiceLoader();
    private final ClassLoader ctx = currentThread().getContextClassLoader();

    @Test
    public void shouldNotLoadSpiDoesNotExist() throws IOException
    {
        assertThat( spi.lookup( NoServiceInterface.class, ctx ) )
            .isEmpty();

        assertThat( spi.load( NoServiceInterface.class, ctx ) )
            .isEmpty();
    }

    @Test
    public void shouldNotLoadEmptySpi() throws IOException
    {
        assertThat( spi.lookup( EmptyServiceInterface.class, ctx ) )
            .isEmpty();

        assertThat( spi.load( EmptyServiceInterface.class, ctx ) )
            .isEmpty();
    }

    @Test
    public void shouldLoad2SpiObjects() throws IOException
    {
        assertThat( spi.lookup( ExistingServiceInterface.class, ctx ) )
            .hasSize( 2 );

        assertThat( spi.lookup( ExistingServiceInterface.class, ctx ) )
            .containsOnly( SPImpl1.class.getName(), SPImpl2.class.getName() );


        assertThat( spi.load( ExistingServiceInterface.class, ctx ) )
            .hasSize( 2 );

        assertThat( spi.load( ExistingServiceInterface.class, ctx ) )
            .contains( new SPImpl1(), new SPImpl2() );
    }
}
