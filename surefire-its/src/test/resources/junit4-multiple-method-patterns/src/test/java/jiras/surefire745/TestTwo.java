package jiras.surefire745;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TestTwo
{
    @Rule
    public final TestName testName = new TestName();

    @Test
    public void testSuccessOne()
    {
        System.out.println( getClass() + "#" + testName.getMethodName() );
    }

    @Test
    public void testSuccessTwo()
    {
        System.out.println( getClass() + "#" + testName.getMethodName() );
    }
}
