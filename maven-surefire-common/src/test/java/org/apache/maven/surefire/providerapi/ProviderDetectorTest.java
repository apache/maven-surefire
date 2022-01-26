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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.surefire.api.provider.SurefireProvider;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * Unit test for ProviderDetector.
 *
 * @author Slawomir Jaranowski
 */
@RunWith( MockitoJUnitRunner.class )
public class ProviderDetectorTest
{

    @Mock
    private ServiceLoader serviceLoader;

    @Mock
    private Logger logger;

    @InjectMocks
    private ProviderDetector providerDetector;

    @Test
    public void defaultWhenAllKnownProviderNotApplicable() throws Exception
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );
        ProviderInfo defaultProvider = mock( ProviderInfo.class );

        // no manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( Collections.<String>emptySet() );

        // all well known providers are not applicable
        when( providerInfo1.isApplicable() ).thenReturn( false );
        when( providerInfo2.isApplicable() ).thenReturn( false );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setDefaultProvider( defaultProvider );
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then
        assertThat( providerInfoList )
            .containsExactly( defaultProvider );

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        verify( providerInfo1 ).isApplicable();
        verify( providerInfo2 ).isApplicable();

        verifyNoMoreInteractions( logger, serviceLoader, providerInfo1, providerInfo2 );
    }

    @Test
    public void onlyOneWhenAllKnownProviderIsApplicable() throws Exception
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );

        // no manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( Collections.<String>emptySet() );

        // first well known providers are applicable
        when( providerInfo1.isApplicable() ).thenReturn( true );
        // second not
        when( providerInfo2.isApplicable() ).thenReturn( false );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then - only first is returned
        assertThat( providerInfoList ).containsExactly( providerInfo1 );

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        // all providers are checked
        verify( providerInfo1 ).isApplicable();
        verify( providerInfo2 ).isApplicable();
        verify( providerInfo1 ).getProviderName();

        verify( logger ).info( anyString() );

        verifyNoMoreInteractions( logger, serviceLoader, providerInfo1, providerInfo2 );
    }

    @Test
    public void manyAllKnownProviderIsAllowedWithWarn() throws Exception
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );

        // no manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( Collections.<String>emptySet() );

        when( providerInfo1.isApplicable() ).thenReturn( true );
        when( providerInfo1.getProviderName() ).thenReturn( "someProvider1" );

        when( providerInfo2.isApplicable() ).thenReturn( true );
        when( providerInfo2.getProviderName() ).thenReturn( "someProvider2" );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        request.setMultipleFrameworks( "warn" );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then - only first is returned
        assertThat( providerInfoList ).containsExactly( providerInfo1 );

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        // all providers are checked
        verify( providerInfo1 ).isApplicable();
        verify( providerInfo1, times( 3 ) ).getProviderName();
        verify( providerInfo2 ).isApplicable();
        verify( providerInfo2, times( 2 ) ).getProviderName();

        // warn for user is generated
        verify( logger, times( 6 ) ).warn( anyString() );
        verify( logger ).info( anyString() );

        verifyNoMoreInteractions( logger, serviceLoader, providerInfo1, providerInfo2 );
    }

    @Test
    public void manyAllKnownProviderIsNotAllowed() throws Exception
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );

        // no manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( Collections.<String>emptySet() );

        when( providerInfo1.isApplicable() ).thenReturn( true );
        when( providerInfo1.getProviderName() ).thenReturn( "someProvider1" );

        when( providerInfo2.isApplicable() ).thenReturn( true );
        when( providerInfo2.getProviderName() ).thenReturn( "someProvider2" );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        request.setMultipleFrameworks( "fail" );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then - list of providers will be empty
        assertThat( providerInfoList ).isEmpty();

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        // all providers are checked
        verify( providerInfo1 ).isApplicable();
        verify( providerInfo1, times( 2 ) ).getProviderName();
        verify( providerInfo2 ).isApplicable();
        verify( providerInfo2, times( 2 ) ).getProviderName();

        // error for user is generated
        verify( logger, times( 6 ) ).error( anyString() );

        verifyNoMoreInteractions( logger, serviceLoader, providerInfo1, providerInfo2 );
    }

    @Test
    public void specialCaseForJunit4AndJunit47() throws IOException
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );

        // no manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( Collections.<String>emptySet() );

        when( providerInfo1.isApplicable() ).thenReturn( true );
        when( providerInfo1.getProviderName() ).thenReturn( "org.apache.maven.surefire.junitcore.JUnitCoreProvider" );

        when( providerInfo2.isApplicable() ).thenReturn( true );
        when( providerInfo2.getProviderName() ).thenReturn( "org.apache.maven.surefire.junit4.JUnit4Provider" );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        request.setMultipleFrameworks( "fail" );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then - list of providers will be empty
        assertThat( providerInfoList ).containsExactly( providerInfo1 );

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        // all providers are checked
        verify( providerInfo1 ).isApplicable();
        verify( providerInfo1, times( 2 ) ).getProviderName();

        verify( providerInfo2 ).isApplicable();
        verify( providerInfo2 ).getProviderName();

        verify( logger ).info( anyString() );

        verifyNoMoreInteractions( logger, serviceLoader, providerInfo1, providerInfo2 );
    }

    @Test
    public void allManuallyConfiguredProviderAreReturned() throws IOException
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );

        // manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( new HashSet<>( asList( "provider1", "provider2" ) ) );

        when( providerInfo1.getProviderName() ).thenReturn( "provider1" );
        when( providerInfo2.getProviderName() ).thenReturn( "provider2" );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then - all providers on list
        assertThat( providerInfoList ).containsExactly( providerInfo1, providerInfo2 );

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        verify( providerInfo1, times( 3 ) ).getProviderName();
        verify( providerInfo2, times( 2 ) ).getProviderName();

        // don't check - if it should be used  ???
        verify( providerInfo1, never() ).isApplicable();
        verify( providerInfo1, never() ).isApplicable();

        verify( logger, times( 2 ) ).info( anyString() );

        verifyNoMoreInteractions( logger, serviceLoader, providerInfo1, providerInfo2 );
    }

    @Test
    public void dynamicallyProviderAreReturned() throws IOException
    {
        // given
        ProviderInfo providerInfo1 = mock( ProviderInfo.class );
        ProviderInfo providerInfo2 = mock( ProviderInfo.class );
        ProviderInfo dynProviderInfo = mock( ProviderInfo.class );

        ConfigurableProviderInfo dynamicProvider = mock( ConfigurableProviderInfo.class );

        // manually configured providers
        when( serviceLoader.lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) ) )
            .thenReturn( new HashSet<>( asList( "unKnown", "provider2" ) ) );

        when( dynamicProvider.instantiate( anyString() ) ).thenReturn( dynProviderInfo );

        when( providerInfo1.getProviderName() ).thenReturn( "provider1" );
        when( providerInfo2.getProviderName() ).thenReturn( "provider2" );

        // when
        ProviderDetectorRequest request = new ProviderDetectorRequest();
        request.setDynamicProvider( dynamicProvider );
        request.setWellKnownProviders( providerInfo1, providerInfo2 );
        List<ProviderInfo> providerInfoList = providerDetector.resolve( request );

        // then - all providers on list
        assertThat( providerInfoList ).containsExactly( dynProviderInfo, providerInfo2 );

        verify( serviceLoader ).lookup( eq( SurefireProvider.class ), any( ClassLoader.class ) );

        verify( dynamicProvider ).instantiate( "unKnown" );

        verify( providerInfo1, times( 2 ) ).getProviderName();
        verify( providerInfo2, times( 3 ) ).getProviderName();

        // don't check - if it should be used  ???
        verify( providerInfo1, never() ).isApplicable();
        verify( dynamicProvider, never() ).isApplicable();

        verify( logger, times( 2 ) ).info( anyString() );

        verifyNoMoreInteractions( logger, serviceLoader, dynamicProvider, providerInfo1, providerInfo2 );
    }

}
