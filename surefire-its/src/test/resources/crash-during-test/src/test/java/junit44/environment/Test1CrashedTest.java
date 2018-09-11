package junit44.environment;

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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;
import org.junit.Test;
import uk.me.mjt.CrashJvm;

public class Test1CrashedTest
{
    @Test
    public void testCrashJvm() throws Exception
    {
        MILLISECONDS.sleep( 1500L );

        assertTrue(CrashJvm.loadedOk());
        
        String crashType = System.getProperty("crashType");
        assertNotNull(crashType);
        if ( crashType.equals( "exit" ) )
        {
            CrashJvm.exit();
        }
        else if ( crashType.equals( "abort" ) )
        {
            CrashJvm.abort();
        }
        else if (crashType.equals( "segfault" ))
        {
            CrashJvm.segfault();
        }
        else
        {
            fail("Don't recognise crashType " + crashType);
        }
    }
}
