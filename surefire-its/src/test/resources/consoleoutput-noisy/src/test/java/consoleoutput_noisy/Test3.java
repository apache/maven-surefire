package consoleoutput_noisy;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;

/**
 *
 */
public class Test3
{
    @Test
    public void test() throws Exception
    {
        long t1 = System.currentTimeMillis();
        System.out.println( "t1 = " + t1 );
        for ( int i = 0; i < 320_000; i++ )
        {
            System.out.println( "01234567890123456789012345678901234567890123456789"
                + "01234567890123456789012345678901234567890123456789" );
        }
        long t2 = System.currentTimeMillis();
        System.out.println( "t2 = " + t2 );

        File target = new File( System.getProperty( "user.dir" ) );
        new File( target, ( t2 - t1 ) + "" )
            .createNewFile();
    }
}
