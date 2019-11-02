package org.apache.maven.surefire.group.parse;

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

import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.apache.maven.surefire.group.match.OrGroupMatcher;
import org.apache.maven.surefire.group.match.SingleGroupMatcher;

import junit.framework.TestCase;

/**
 *
 */
public class GroupMatcherParserTest
    extends TestCase
{

    public void testParseSingleClass()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser( GroupMatcherParser.class.getName() ).parse();
        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof SingleGroupMatcher );
        assertTrue( matcher.enabled( GroupMatcherParser.class ) );
    }

    public void testParseInvertedSingleClass()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser( "NOT " + GroupMatcherParser.class.getName() ).parse();
        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof InverseGroupMatcher );
        assertFalse( matcher.enabled( GroupMatcherParser.class ) );
    }

    public void testParseBareANDedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            GroupMatcherParser.class.getName() + " AND " + SingleGroupMatcher.class.getName() ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof AndGroupMatcher );
        assertFalse( matcher.enabled( GroupMatcherParser.class ) );
        assertTrue( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testParseBareORedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            GroupMatcherParser.class.getName() + " OR " + SingleGroupMatcher.class.getName() ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof OrGroupMatcher );
        assertTrue( matcher.enabled( GroupMatcherParser.class ) );
        assertTrue( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testBareCommaSeparatedORedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            GroupMatcherParser.class.getName() + ", " + SingleGroupMatcher.class.getName() ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof OrGroupMatcher );
        assertTrue( matcher.enabled( GroupMatcherParser.class ) );
        assertTrue( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testParseGroupedANDedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            "(" + GroupMatcherParser.class.getName() + " AND " + SingleGroupMatcher.class.getName() + ")" ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof AndGroupMatcher );
        assertFalse( matcher.enabled( GroupMatcherParser.class ) );
        assertTrue( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testParseGroupedORedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            "(" + GroupMatcherParser.class.getName() + " OR " + SingleGroupMatcher.class.getName() + ")" ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof OrGroupMatcher );
        assertTrue( matcher.enabled( GroupMatcherParser.class ) );
        assertTrue( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testParseInvertedGroupedANDedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            "NOT (" + GroupMatcherParser.class.getName() + " AND " + SingleGroupMatcher.class.getName() + ")" ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof InverseGroupMatcher );
        assertTrue( matcher.enabled( GroupMatcherParser.class ) );
        assertFalse( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testParseInvertedGroupedORedPair()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser(
            "NOT (" + GroupMatcherParser.class.getName() + " OR " + SingleGroupMatcher.class.getName() + ")" ).parse();

        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof InverseGroupMatcher );
        assertFalse( matcher.enabled( GroupMatcherParser.class ) );
        assertFalse( matcher.enabled( GroupMatcherParser.class, SingleGroupMatcher.class ) );
    }

    public void testSingleMatchWhenDotClassAppended()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser( SingleGroupMatcher.class.getName() + ".class" ).parse();
        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof SingleGroupMatcher );
        assertTrue( matcher.enabled( SingleGroupMatcher.class ) );
    }

    public void testSingleMatchWithOnlyClassSimpleName()
        throws ParseException
    {
        GroupMatcher matcher = new GroupMatcherParser( SingleGroupMatcher.class.getSimpleName() ).parse();
        assertTrue( "Wrong matcher type: " + matcher.getClass().getName(), matcher instanceof SingleGroupMatcher );
        assertTrue( matcher.enabled( SingleGroupMatcher.class ) );
    }

}
