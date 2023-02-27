package surefireparallel;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
@RunWith( Suite.class )
@Suite.SuiteClasses(
    {
        Waiting1Test.class,
        Waiting2Test.class,
        Waiting3Test.class,
        Waiting4Test.class
    })
public class Suite1Test
{
}
