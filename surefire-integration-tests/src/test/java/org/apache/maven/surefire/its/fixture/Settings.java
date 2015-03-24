package org.apache.maven.surefire.its.fixture;

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

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public enum Settings
{
    JUNIT4_TEST( TestFramework.JUNIT4, Configuration.TEST ),
    JUNIT47_TEST( TestFramework.JUNIT47, Configuration.TEST ),
    JUNIT4_INCLUDES( TestFramework.JUNIT4, Configuration.INCLUDES ),
    JUNIT47_INCLUDES( TestFramework.JUNIT47, Configuration.INCLUDES ),
    JUNIT4_INCLUDES_EXCLUDES( TestFramework.JUNIT4, Configuration.INCLUDES_EXCLUDES ),
    JUNIT47_INCLUDES_EXCLUDES( TestFramework.JUNIT47, Configuration.INCLUDES_EXCLUDES ),
    JUNIT4_INCLUDES_FILE( TestFramework.JUNIT4, Configuration.INCLUDES_FILE ),
    JUNIT47_INCLUDES_FILE( TestFramework.JUNIT47, Configuration.INCLUDES_FILE ),
    JUNIT4_INCLUDES_EXCLUDES_FILE( TestFramework.JUNIT4, Configuration.INCLUDES_EXCLUDES_FILE ),
    JUNIT47_INCLUDES_EXCLUDES_FILE( TestFramework.JUNIT47, Configuration.INCLUDES_EXCLUDES_FILE ),
    TestNG_TEST( TestFramework.TestNG, Configuration.TEST ),
    TestNG_INCLUDES( TestFramework.TestNG, Configuration.INCLUDES ),
    TestNG_INCLUDES_EXCLUDES( TestFramework.TestNG, Configuration.INCLUDES_EXCLUDES ),
    TestNG_INCLUDES_FILE( TestFramework.TestNG, Configuration.INCLUDES_FILE ),
    TestNG_INCLUDES_EXCLUDES_FILE( TestFramework.TestNG, Configuration.INCLUDES_EXCLUDES_FILE );

    private final TestFramework framework;
    private final Configuration configuration;

    Settings( TestFramework framework, Configuration configuration )
    {
        this.framework = framework;
        this.configuration = configuration;
    }

    public String path()
    {
        return name().replace( '_', '-' ).toLowerCase();
    }

    public String profile()
    {
        return path();
    }

    public TestFramework getFramework()
    {
        return framework;
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }
}
