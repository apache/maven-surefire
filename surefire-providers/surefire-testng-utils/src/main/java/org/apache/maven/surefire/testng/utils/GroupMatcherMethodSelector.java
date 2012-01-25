package org.apache.maven.surefire.testng.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;

public class GroupMatcherMethodSelector
    implements IMethodSelector
{

    private static final long serialVersionUID = 1L;

    private static GroupMatcher matcher;

    private Map<ITestNGMethod, Boolean> answers = new HashMap<ITestNGMethod, Boolean>();

    public boolean includeMethod( IMethodSelectorContext context, ITestNGMethod method, boolean isTestMethod )
    {
        // System.out.println( "Checking: " + method + " vs. matcher: " + matcher );
        Boolean result = (Boolean) answers.get( method );
        if ( result != null )
        {
            // System.out.println( "Enabled? " + result );
            return result;
        }

        if ( matcher == null )
        {
            // System.out.println( "No matcher, enable by default" );
            return true;
        }

        String[] groups = method.getGroups();
        result = Boolean.valueOf( matcher.enabled( groups ) );

        answers.put( method, result );

        // System.out.println( "Enabled? " + result );
        return result;
    }

    public void setTestMethods( List<ITestNGMethod> testMethods )
    {
    }

    public static void setGroups( String groups, String excludedGroups )
    {
        // System.out.println( "Processing group includes: '" + groups + "'\nExcludes: '" + excludedGroups + "'" );

        try
        {
            AndGroupMatcher matcher = new AndGroupMatcher();
            GroupMatcher in = groups == null ? null : new GroupMatcherParser( groups ).parse();
            if ( in != null )
            {
                matcher.addMatcher( in );
            }

            GroupMatcher ex = excludedGroups == null ? null : new GroupMatcherParser( excludedGroups ).parse();
            if ( ex != null )
            {
                matcher.addMatcher( new InverseGroupMatcher( ex ) );
            }

            if ( in != null || ex != null )
            {
                // System.out.println( "Group matcher: " + matcher );
                GroupMatcherMethodSelector.matcher = matcher;
            }
        }
        catch ( ParseException e )
        {
            throw new IllegalArgumentException( "Cannot parse group includes/excludes expression(s):\nIncludes: "
                + groups + "\nExcludes: " + excludedGroups, e );
        }
    }

    public static void setGroupMatcher( GroupMatcher matcher )
    {
        GroupMatcherMethodSelector.matcher = matcher;
    }

}
