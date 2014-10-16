package environment;

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

import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;


public class BasicTest
{

    
    @Test
    public void testEnvVar()
    {
        Assert.assertThat( System.getenv( "PATH" ), notNullValue() );
        Assert.assertThat( System.getenv( "DUMMY_ENV_VAR" ), is( "foo" ) );
        Assert.assertThat( System.getenv( "EMPTY_VAR" ), is( "" ) );
        Assert.assertThat( System.getenv( "UNSET_VAR" ), is( "" ) );
        Assert.assertThat( System.getenv( "UNDEFINED_VAR" ), nullValue() );
    }


}
