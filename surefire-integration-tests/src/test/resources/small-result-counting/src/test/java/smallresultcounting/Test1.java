package smallresultcounting;

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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

public class Test1
{
    @Test
    public void testWithFailingAssumption1()
    {
        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption2()
    {
        try
        {
            Thread.sleep( 150 );
        }
        catch ( InterruptedException ignore )
        {
        }

        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption3()
    {
        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption4()
    {
        assumeThat( 2, is( 3 ) );
    }

    @Test
    public void testWithFailingAssumption5()
    {
        assumeThat( 2, is( 3 ) );
    }
}