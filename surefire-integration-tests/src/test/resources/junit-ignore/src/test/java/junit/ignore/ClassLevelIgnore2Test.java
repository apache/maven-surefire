package junit.ignore;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore( "ignore this test" )
public class ClassLevelIgnore2Test
{

    @Test
    public void testIgnorable()
    {
        Assert.fail( "you should have ignored me!" );
    }

}
