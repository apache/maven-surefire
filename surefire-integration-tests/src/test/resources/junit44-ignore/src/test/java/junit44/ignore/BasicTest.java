package junit44.ignore;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;


public class BasicTest
{

    @Ignore("ignore this test")
    @Test
    public void testIgnorable()
    {
        Assert.fail("you should have ignored me!");
    }

}
