package pkg.junit4;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ParameterizedWithDisplayNameTest
{
    private static int count = 0;

    @Parameterized.Parameters(name = "value={0}")
    public static List<Integer> parameters() throws Exception
    {
        return Arrays.asList( 0, 1 );
    }

    @Parameterized.Parameter(0)
    public int expected;

    @Test
    public void notFlaky()
    {
        assertEquals( expected, 0 );
    }
}
