package org.apache.maven.plugin.surefire;
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


import java.lang.reflect.Field;
import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.toolchain.Toolchain;

import junit.framework.TestCase;

public class SurefirePluginTest
    extends TestCase
{

    public void testForkMode()
        throws NoSuchFieldException, IllegalAccessException
    {
        SurefirePlugin surefirePlugin = new SurefirePlugin();
        setFieldValue( surefirePlugin, "toolchain", new MyToolChain() );
        setFieldValue( surefirePlugin, "forkMode", "never" );
        assertEquals( ForkConfiguration.FORK_ONCE, surefirePlugin.getEffectiveForkMode() );
    }

    public void testForkCountComputation()
    {
        SurefirePlugin surefirePlugin = new SurefirePlugin();
        assertConversionFails( surefirePlugin, "nothing" );

        assertConversionFails( surefirePlugin, "5,0" );
        assertConversionFails( surefirePlugin, "5.0" );
        assertConversionFails( surefirePlugin, "5,0C" );
        assertConversionFails( surefirePlugin, "5.0CC" );
        
        assertForkCount( surefirePlugin, 5, "5" );
        
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        assertForkCount( surefirePlugin, 3*availableProcessors, "3C" );
        assertForkCount( surefirePlugin, (int) ( 2.5*availableProcessors ), "2.5C" );
        assertForkCount( surefirePlugin, availableProcessors, "1.0001 C" );
    }

    private void assertForkCount( SurefirePlugin surefirePlugin, int expected, String value )
    {
        assertEquals( expected, surefirePlugin.convertWithCoreCount( value ));
    }
    
    private void assertConversionFails( SurefirePlugin surefirePlugin, String value )
    {
        try {
            surefirePlugin.convertWithCoreCount( value );
        } catch (NumberFormatException nfe)
        {
            return;
        }
        fail( "Expected NumberFormatException when converting " + value );
    }

    private void setFieldValue( SurefirePlugin plugin, String fieldName, Object value )
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = findField( plugin.getClass(), fieldName );
        field.setAccessible( true );
        field.set( plugin, value );

    }

    private Field findField( Class clazz, String fieldName )
    {
        while ( clazz != null )
        {
            try
            {
                return clazz.getDeclaredField( fieldName );
            }
            catch ( NoSuchFieldException e )
            {
                clazz = clazz.getSuperclass();
            }
        }
        throw new IllegalArgumentException( "Field not found" );
    }

    private class MyToolChain
        implements Toolchain
    {
        public String getType()
        {
            return null;
        }

        public String findTool( String s )
        {
            return null;
        }
    }
}
