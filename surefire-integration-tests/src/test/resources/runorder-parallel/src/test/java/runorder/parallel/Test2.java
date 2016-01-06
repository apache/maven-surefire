package runorder.parallel;

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

import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class Test2
{

    @Test
    public void testSleep1000()
    {
        System.out.println( "Test2.sleep1000 started @ " + System.currentTimeMillis() );
        Test1.sleep( 1000 );
    }

    @Test
    public void testSleep3000()
    {
        System.out.println( "Test2.sleep3000 started @ " + System.currentTimeMillis() );
        Test1.sleep( 3000 );
    }

    @Test
    public void testSleep5000()
    {
        System.out.println( "Test2.sleep5000 started @ " + System.currentTimeMillis() );
        Test1.sleep( 5000 );
    }
}