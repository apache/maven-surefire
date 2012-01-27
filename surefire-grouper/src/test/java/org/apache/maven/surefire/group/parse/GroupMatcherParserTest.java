package org.apache.maven.surefire.group.parse;

import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.apache.maven.surefire.group.match.OrGroupMatcher;
import org.apache.maven.surefire.group.match.SingleGroupMatcher;

import junit.framework.TestCase;

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
