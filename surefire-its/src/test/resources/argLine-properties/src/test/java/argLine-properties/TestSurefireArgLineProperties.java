package argLine;

import org.junit.Assert;
import org.junit.Test;

public class TestSurefireArgLineProperties
{
    @Test
    public void testFromProp()
    {
        String fromProp = System.getProperty("p1");
        Assert.assertNotNull(fromProp, "incorrect arg line, no p1 present?");
        Assert.assertEquals("from-prop-value", fromProp);
    }

    @Test
    public void testOverrideProp()
    {
        String overrideProp = System.getProperty("p2");
        Assert.assertNotNull(overrideProp, "incorrect arg line, no p2 present?");
        Assert.assertEquals("override-prop-value", overrideProp);
    }

    @Test
    public void testUndefinedProp()
    {
        String undefinedProp = System.getProperty("p3");
        Assert.assertNotNull(undefinedProp, "incorrect arg line, no p3 present?");
        Assert.assertEquals("@{undefined.prop}", undefinedProp);
    }

    @Test
    public void testGeneratedProp()
    {
        String generatedProp = System.getProperty("p4");
        Assert.assertNotNull(generatedProp, "incorrect arg line, no p4 present?");
        Assert.assertEquals("generated-prop-value", generatedProp);
    }
}
