package testng.groups;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests grouping
 */
public class TestNGGroupTest
{
    private Object testObject;

    @BeforeClass( groups = "functional" )
    public void configureTest()
    {
        testObject = new Object();
    }

    @Test( groups = { "functional" } )
    public void isFunctional()
    {
        Assert.assertNotNull( testObject, "testObject is null" );
    }

    @Test( groups = { "functional", "notincluded" } )
    public void isFunctionalAndNotincluded()
    {
        Assert.assertNotNull( testObject, "testObject is null" );
    }

    @Test( groups = "notincluded" )
    public void isNotIncluded()
    {
        Assert.assertTrue( false );
    }

    @Test( groups = "abc-def" )
    public void isDashedGroup()
    {
    }

    @Test( groups = "foo.bar" )
    public void isFooBar()
    {
    }

    @Test( groups = "foo.zap" )
    public void isFooZap()
    {
    }

    @Test
    public void noGroup()
    {
    }
}
