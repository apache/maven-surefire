package antignore;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

public class BasicTest
{

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
