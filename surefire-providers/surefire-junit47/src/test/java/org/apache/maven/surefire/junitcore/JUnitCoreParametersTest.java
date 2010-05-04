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
package org.apache.maven.surefire.junitcore;


import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/*
 * @author Kristian Rosenvold, kristian.rosenvold@gmail com
 */

public class JUnitCoreParametersTest
{
    @Test
    public void testIsParallelMethod()
        throws Exception
    {
        assertFalse( getTestSetClasses().isParallelMethod() );
        assertTrue( getTestSetMethods().isParallelMethod() );
        assertFalse( getTestSetBoth().isParallelMethod() );
    }

    @Test
    public void testIsParallelClasses()
        throws Exception
    {
        assertTrue( getTestSetClasses().isParallelClasses() );
        assertFalse( getTestSetMethods().isParallelClasses() );
        assertFalse( getTestSetBoth().isParallelClasses() );
    }

    @Test
    public void testIsParallelBoth()
        throws Exception
    {
        assertFalse( getTestSetClasses().isParallelBoth() );
        assertFalse( getTestSetMethods().isParallelBoth() );
        assertTrue( getTestSetBoth().isParallelBoth() );
    }

    @Test
    public void testIsPerCoreThreadCount()
        throws Exception
    {
        assertFalse( getTestSetClasses().isPerCoreThreadCount() );
        assertFalse( getTestSetMethods().isPerCoreThreadCount() );
        assertTrue( getTestSetBoth().isPerCoreThreadCount() );
    }

    @Test
    public void testGetThreadCount()
        throws Exception
    {
        assertFalse( getTestSetClasses().isPerCoreThreadCount() );
        assertFalse( getTestSetMethods().isPerCoreThreadCount() );
        assertTrue( getTestSetBoth().isPerCoreThreadCount() );
    }

    @Test
    public void testIsUseUnlimitedThreads()
        throws Exception
    {
        assertFalse( getTestSetClasses().isUseUnlimitedThreads() );
        assertTrue( getTestSetMethods().isUseUnlimitedThreads() );
        assertFalse( getTestSetBoth().isUseUnlimitedThreads() );
    }

    @Test
    public void testIsNoThreading()
        throws Exception
    {
        assertFalse( getTestSetClasses().isNoThreading() );
        assertFalse( getTestSetMethods().isNoThreading() );
        assertFalse( getTestSetBoth().isNoThreading() );
    }

    @Test
    public void testIsAnyParallelitySelected()
        throws Exception
    {
        assertTrue( getTestSetClasses().isAnyParallelitySelected() );
        assertTrue( getTestSetMethods().isAnyParallelitySelected() );
        assertTrue( getTestSetBoth().isAnyParallelitySelected() );
    }

    @Test
    public void testIsConfigurableParallelComputerPresent()
        throws Exception
    {
        assertFalse( getTestSetClasses().isConfigurableParallelComputerPresent() );
        assertFalse( getTestSetMethods().isConfigurableParallelComputerPresent() );
        assertTrue( getTestSetBoth().isConfigurableParallelComputerPresent() );
    }

    @Test
    public void testToString()
        throws Exception
    {
        assertNotNull( getTestSetBoth().toString() );
    }


    public Properties getPropsetClasses()
    {
        Properties props = new Properties();
        props.setProperty( JUnitCoreParameters.PARALLEL_KEY, "classes" );
        props.setProperty( JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "false" );
        props.setProperty( JUnitCoreParameters.THREADCOUNT_KEY, "2" );
        props.setProperty( JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "false" );
        props.setProperty( JUnitCoreParameters.CONFIGURABLEPARALLELCOMPUTERPRESENT_KEY, "false" );
        return props;
    }

    public Properties getPropsetMethods()
    {
        Properties props = new Properties();
        props.setProperty( JUnitCoreParameters.PARALLEL_KEY, "methods" );
        props.setProperty( JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "false" );
        props.setProperty( JUnitCoreParameters.THREADCOUNT_KEY, "2" );
        props.setProperty( JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "true" );
        props.setProperty( JUnitCoreParameters.CONFIGURABLEPARALLELCOMPUTERPRESENT_KEY, "false" );
        return props;
    }

    public Properties getPropsetBoth()
    {
        Properties props = new Properties();
        props.setProperty( JUnitCoreParameters.PARALLEL_KEY, "both" );
        props.setProperty( JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "true" );
        props.setProperty( JUnitCoreParameters.THREADCOUNT_KEY, "7" );
        props.setProperty( JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "false" );
        props.setProperty( JUnitCoreParameters.CONFIGURABLEPARALLELCOMPUTERPRESENT_KEY, "true" );
        return props;
    }

    private JUnitCoreParameters getTestSetBoth()
    {
        return new JUnitCoreParameters( getPropsetBoth() );
    }

    private JUnitCoreParameters getTestSetClasses()
    {
        return new JUnitCoreParameters( getPropsetClasses() );
    }

    private JUnitCoreParameters getTestSetMethods()
    {
        return new JUnitCoreParameters( getPropsetMethods() );
    }
}
