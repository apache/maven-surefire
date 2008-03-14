package additionalClasspath;

import junit.framework.TestCase;

public class BasicTest
    extends TestCase
{

    public void testExtraResource() {
        assertNotNull(BasicTest.class.getResourceAsStream("/test.txt"));
    }

}
