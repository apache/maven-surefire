package org.apache.maven.plugin.surefire.booterclient;

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

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ChecksumCalculator}.
 */
public class ChecksumCalculatorTest
{
    @Test
    public void testGetSha1()
    {
        final ChecksumCalculator calculator = new ChecksumCalculator();
        calculator.add( "foö 🔥 Россия 한국 中国" );
        assertEquals( "3421557EBE66A4741CA51C8D610AB1AB41D1693B", calculator.getSha1() );
    }
}
