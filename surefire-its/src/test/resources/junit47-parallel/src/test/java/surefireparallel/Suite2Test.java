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
        Waiting5Test.class,
        Waiting6Test.class,
        Waiting7Test.class,
        Waiting8Test.class
    })
public class Suite2Test
{
}
