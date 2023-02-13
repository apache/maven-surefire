package pkg;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests grouping
 */
public class ATest
{
    @Test(groups = {"group"})
    public void group()
    {
    }

    @Test(groups = {"agroup"})
    public void agroup()
    {
        Assert.fail("Group should not be run");
    }
}
