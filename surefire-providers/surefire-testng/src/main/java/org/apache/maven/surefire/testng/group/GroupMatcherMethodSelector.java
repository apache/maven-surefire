package org.apache.maven.surefire.testng.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.group.match.GroupMatcher;
import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;

public class GroupMatcherMethodSelector
    implements IMethodSelector
{

    private static final long serialVersionUID = 1L;

    private static GroupMatcher matcher;

    private Map answers = new HashMap();

    public boolean includeMethod( IMethodSelectorContext context, ITestNGMethod method, boolean isTestMethod )
    {
        Boolean result = (Boolean) answers.get( method );
        if ( result != null )
        {
            return result.booleanValue();
        }

        if ( matcher == null )
        {
            return true;
        }

        String[] groups = method.getGroups();
        result = Boolean.valueOf( matcher.enabled( groups ) );

        answers.put( method, result );

        return result.booleanValue();
    }

    public void setTestMethods( List testMethods )
    {
    }

    public static void setGroupMatcher( GroupMatcher matcher )
    {
        GroupMatcherMethodSelector.matcher = matcher;
    }

}
