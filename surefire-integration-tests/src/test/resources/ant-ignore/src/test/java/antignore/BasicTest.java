package antignore;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class BasicTest
{

    @Test
    @Ignore
    public void testIgnorable()
    {
        Assert.fail( "you should have ignored me!" );
    }

    @Test
    public void testSomethingElse()
    {

    }

}
