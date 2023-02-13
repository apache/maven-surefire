package testng.suiteXml;

import org.testng.annotations.Test;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class ShouldNotRunTest {

    @Test
    public void shouldNotRun()
    {
        System.out.println( getClass().getSimpleName() + "#shouldNotRun()" );
    }
}
