import junit.framework.TestCase;

public class TestSurefireArgLine
    extends TestCase
{

    public void testArgLine()
    {
        String javaLibraryPath = System.getProperty( "java.library.path" );
        assertEquals( "incorrect java.library.path; " +
        		"Surefire should have passed this in correctly",
        		"foo foo/foo/bar/1.0", javaLibraryPath );
    }

}
