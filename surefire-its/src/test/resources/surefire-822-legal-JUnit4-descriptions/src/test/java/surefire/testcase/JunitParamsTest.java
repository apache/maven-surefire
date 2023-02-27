package surefire.testcase;
import java.util.Arrays;
import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Surefire JunitParams test.
 */
@RunWith( JUnitParamsRunner.class )
public class JunitParamsTest
{

    @Parameters( method = "parameters" )
    @Test
    public void testSum( int a, int b, int expected )
    {
        assertThat( a + b, equalTo( expected ) );
    }

    public Collection<Integer[]> parameters()
    {
        Integer[][] parameters = { { 1, 2, 3 }, { 2, 3, 5 }, { 3, 4, 7 }, };
        return Arrays.asList( parameters );
    }
}
