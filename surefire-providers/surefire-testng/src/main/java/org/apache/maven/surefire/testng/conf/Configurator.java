package org.apache.maven.surefire.testng.conf;

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

import java.util.Map;

import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

/**
 * Configurator for passing configuration properties to TestNG
 *
 */
public interface Configurator
{
    void configure( TestNG testng, Map<String, String> options )
        throws TestSetFailedException;

    void configure ( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException;
}
