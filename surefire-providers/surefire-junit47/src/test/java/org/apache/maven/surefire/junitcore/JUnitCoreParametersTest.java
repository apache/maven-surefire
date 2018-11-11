package org.apache.maven.surefire.junitcore;

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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.is;

/*
 * @author Kristian Rosenvold, kristian.rosenvold@gmail com
 */
public class JUnitCoreParametersTest
{
    @Test
    public void defaultParameters()
    {
        assertFalse( newTestSetDefault().isParallelismSelected() );
        assertTrue( newTestSetDefault().isPerCoreThreadCount() );
        assertThat( newTestSetDefault().getThreadCount(), is( 0 ) );
        assertThat( newTestSetDefault().getThreadCountMethods(), is( 0 ) );
        assertThat( newTestSetDefault().getThreadCountClasses(), is( 0 ) );
        assertThat( newTestSetDefault().getThreadCountSuites(), is( 0 ) );
        assertFalse( newTestSetDefault().isUseUnlimitedThreads() );
        assertThat( newTestSetDefault().getParallelTestsTimeoutInSeconds(), is( 0d ) );
        assertThat( newTestSetDefault().getParallelTestsTimeoutForcedInSeconds(), is( 0d ) );
        assertTrue( newTestSetDefault().isParallelOptimization() );
    }

    @Test
    public void optimizationParameter()
    {
        assertFalse( newTestSetOptimization( false ).isParallelOptimization() );
    }

    @Test
    public void timeoutParameters()
    {
        JUnitCoreParameters parameters = newTestSetTimeouts( 5.5d, 11.1d );
        assertThat( parameters.getParallelTestsTimeoutInSeconds(), is( 5.5d ) );
        assertThat( parameters.getParallelTestsTimeoutForcedInSeconds(), is( 11.1d ) );
    }

    @Test
    public void isParallelMethod()
    {
        assertFalse( newTestSetClasses().isParallelMethods() );
        assertTrue( newTestSetMethods().isParallelMethods() );
        assertTrue( newTestSetBoth().isParallelMethods() );
    }

    @Test
    public void isParallelClasses()
    {
        assertTrue( newTestSetClasses().isParallelClasses() );
        assertFalse( newTestSetMethods().isParallelClasses() );
        assertTrue( newTestSetBoth().isParallelClasses() );
    }

    @Test
    public void isParallelBoth()
    {
        assertFalse( isParallelMethodsAndClasses( newTestSetClasses() ) );
        assertFalse( isParallelMethodsAndClasses( newTestSetMethods() ) );
        assertTrue( isParallelMethodsAndClasses( newTestSetBoth() ) );
    }

    @Test
    public void isPerCoreThreadCount()
    {
        assertFalse( newTestSetClasses().isPerCoreThreadCount() );
        assertFalse( newTestSetMethods().isPerCoreThreadCount() );
        assertTrue( newTestSetBoth().isPerCoreThreadCount() );
    }

    @Test
    public void getThreadCount()
    {
        assertFalse( newTestSetClasses().isPerCoreThreadCount() );
        assertFalse( newTestSetMethods().isPerCoreThreadCount() );
        assertTrue( newTestSetBoth().isPerCoreThreadCount() );
    }

    @Test
    public void isUseUnlimitedThreads()
    {
        assertFalse( newTestSetClasses().isUseUnlimitedThreads() );
        assertTrue( newTestSetMethods().isUseUnlimitedThreads() );
        assertFalse( newTestSetBoth().isUseUnlimitedThreads() );
    }

    @Test
    public void isNoThreading()
    {
        assertFalse( newTestSetClasses().isNoThreading() );
        assertFalse( newTestSetMethods().isNoThreading() );
        assertFalse( newTestSetBoth().isNoThreading() );
    }

    @Test
    public void isAnyParallelismSelected()
    {
        assertTrue( newTestSetClasses().isParallelismSelected() );
        assertTrue( newTestSetMethods().isParallelismSelected() );
        assertTrue( newTestSetBoth().isParallelismSelected() );
    }

    private Map<String, String> newDefaultProperties()
    {
        return new HashMap<>();
    }


    private Map<String, String> newPropertiesClasses()
    {
        Map<String, String> props = new HashMap<>();
        props.put(JUnitCoreParameters.PARALLEL_KEY, "classes");
        props.put(JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "false");
        props.put(JUnitCoreParameters.THREADCOUNT_KEY, "2");
        props.put(JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "false");
        return props;
    }

    private Map<String, String> newPropertiesMethods()
    {
        Map<String, String> props = new HashMap<>();
        props.put(JUnitCoreParameters.PARALLEL_KEY, "methods");
        props.put(JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "false");
        props.put(JUnitCoreParameters.THREADCOUNT_KEY, "2");
        props.put(JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "true");
        return props;
    }

    private Map<String, String> newPropertiesBoth()
    {
        Map<String, String> props = new HashMap<>();
        props.put(JUnitCoreParameters.PARALLEL_KEY, "both");
        props.put(JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "true");
        props.put(JUnitCoreParameters.THREADCOUNT_KEY, "7");
        props.put(JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "false");
        return props;
    }

    private Map<String, String> newPropertiesTimeouts( double timeout, double forcedTimeout )
    {
        Map<String, String> props = new HashMap<>();
        props.put(JUnitCoreParameters.PARALLEL_TIMEOUT_KEY, Double.toString(timeout));
        props.put(JUnitCoreParameters.PARALLEL_TIMEOUTFORCED_KEY, Double.toString(forcedTimeout));
        return props;
    }

    private Map<String, String> newPropertiesOptimization( boolean optimize )
    {
        Map<String, String> props = new HashMap<>();
        props.put( JUnitCoreParameters.PARALLEL_OPTIMIZE_KEY, Boolean.toString( optimize ) );
        return props;
    }

    private JUnitCoreParameters newTestSetDefault()
    {
        return new JUnitCoreParameters( newDefaultProperties() );
    }

    private JUnitCoreParameters newTestSetBoth()
    {
        return new JUnitCoreParameters( newPropertiesBoth() );
    }

    private JUnitCoreParameters newTestSetClasses()
    {
        return new JUnitCoreParameters( newPropertiesClasses() );
    }

    private JUnitCoreParameters newTestSetMethods()
    {
        return new JUnitCoreParameters( newPropertiesMethods() );
    }

    private JUnitCoreParameters newTestSetOptimization( boolean optimize )
    {
        return new JUnitCoreParameters( newPropertiesOptimization( optimize ) );
    }

    private JUnitCoreParameters newTestSetTimeouts( double timeout, double forcedTimeout )
    {
        return new JUnitCoreParameters( newPropertiesTimeouts( timeout, forcedTimeout ) );
    }

    private boolean isParallelMethodsAndClasses( JUnitCoreParameters jUnitCoreParameters )
    {
        return jUnitCoreParameters.isParallelMethods() && jUnitCoreParameters.isParallelClasses();
    }
}
