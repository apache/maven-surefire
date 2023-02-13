package surefire.testcase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Surefire non-JunitParams test.
 */
public class NonJunitParamsTest
{

    @Test
    public void testSum()
    {
        assertThat( 1 + 2, equalTo( 3 ) );
    }
}
