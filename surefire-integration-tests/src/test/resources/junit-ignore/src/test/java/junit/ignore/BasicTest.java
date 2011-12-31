package junit.ignore;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore( "ignore this test" )
public class BasicTest
{

    @Ignore( "ignore this test" )
    @Test
    public void testIgnorable()
    {
        Assert.fail( "you should have ignored me!" );
    }

}
