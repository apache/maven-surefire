package forktimeout;

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

public abstract class BaseForkTimeout
{
    protected void dumpStuff( String prefix )
    {
        reallySleep( 1350L );
        for ( int i = 0; i < 200; i++ )
        {
            System.out.println( prefix + " with lots of output " + i );
            System.err.println( prefix + "e with lots of output " + i );
        }
        System.out.println( prefix + "last line" );
        System.err.println( prefix + "e last line" );
    }

    private void reallySleep( long timeout )
    {
        long endAt = System.currentTimeMillis() + timeout;
        try
        {
            Thread.sleep( timeout );
            while ( System.currentTimeMillis() < endAt )
            {
                Thread.yield();
                Thread.sleep( 5 );
            }
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }
}
