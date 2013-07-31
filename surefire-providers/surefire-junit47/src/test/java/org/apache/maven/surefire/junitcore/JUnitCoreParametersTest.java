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

import java.util.Properties;

import junit.framework.TestCase;

/*
 * @author Kristian Rosenvold, kristian.rosenvold@gmail com
 */
public class JUnitCoreParametersTest
    extends TestCase
{
    public void testIsParallelMethod()
        throws Exception
    {
        assertFalse( getTestSetClasses().isParallelMethod() );
        assertTrue( getTestSetMethods().isParallelMethod() );
        assertTrue( getTestSetBoth().isParallelMethod() );
    }

    public void testIsParallelClasses()
        throws Exception
    {
        assertTrue( getTestSetClasses().isParallelClasses() );
        assertFalse( getTestSetMethods().isParallelClasses() );
        assertTrue( getTestSetBoth().isParallelClasses() );
    }

    public void testIsParallelBoth()
        throws Exception
    {
        assertFalse( isParallelMethodsAndClasses( getTestSetClasses() ) );
        assertFalse( isParallelMethodsAndClasses( getTestSetMethods() ) );
        assertTrue( isParallelMethodsAndClasses( getTestSetBoth() ) );
    }

    public void testIsPerCoreThreadCount()
        throws Exception
    {
        assertFalse( getTestSetClasses().isPerCoreThreadCount() );
        assertFalse( getTestSetMethods().isPerCoreThreadCount() );
        assertTrue( getTestSetBoth().isPerCoreThreadCount() );
    }

    public void testGetThreadCount()
        throws Exception
    {
        assertFalse( getTestSetClasses().isPerCoreThreadCount() );
        assertFalse( getTestSetMethods().isPerCoreThreadCount() );
        assertTrue( getTestSetBoth().isPerCoreThreadCount() );
    }

    public void testIsUseUnlimitedThreads()
        throws Exception
    {
        assertFalse( getTestSetClasses().isUseUnlimitedThreads() );
        assertTrue( getTestSetMethods().isUseUnlimitedThreads() );
        assertFalse( getTestSetBoth().isUseUnlimitedThreads() );
    }

    public void testIsNoThreading()
        throws Exception
    {
        assertFalse( getTestSetClasses().isNoThreading() );
        assertFalse( getTestSetMethods().isNoThreading() );
        assertFalse( getTestSetBoth().isNoThreading() );
    }

    public void testIsAnyParallelitySelected()
        throws Exception
    {
        assertTrue( getTestSetClasses().isAnyParallelitySelected() );
        assertTrue( getTestSetMethods().isAnyParallelitySelected() );
        assertTrue( getTestSetBoth().isAnyParallelitySelected() );
    }


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
        return props;
    }

    public Properties getPropsetMethods()
    {
        Properties props = new Properties();
        props.setProperty( JUnitCoreParameters.PARALLEL_KEY, "methods" );
        props.setProperty( JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "false" );
        props.setProperty( JUnitCoreParameters.THREADCOUNT_KEY, "2" );
        props.setProperty( JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "true" );
        return props;
    }

    public Properties getPropsetBoth()
    {
        Properties props = new Properties();
        props.setProperty( JUnitCoreParameters.PARALLEL_KEY, "both" );
        props.setProperty( JUnitCoreParameters.PERCORETHREADCOUNT_KEY, "true" );
        props.setProperty( JUnitCoreParameters.THREADCOUNT_KEY, "7" );
        props.setProperty( JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY, "false" );
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

    private boolean isParallelMethodsAndClasses( JUnitCoreParameters jUnitCoreParameters )
    {
        return jUnitCoreParameters.isParallelMethod() && jUnitCoreParameters.isParallelClasses();
    }
}
