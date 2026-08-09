package junit.runOrder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestA.RandomSeedVerifier.class)
public class TestA
{
    @Test
    public void testTwo()
    {
        System.out.println( "TA" );
    }

    public static class RandomSeedVerifier implements BeforeAllCallback
    {
        @Override
        public void beforeAll( ExtensionContext context )
        {
            String expectedSeed = System.getProperty( "expected.junit.random.seed" );
            if ( expectedSeed != null )
            {
                String actualSeed = context.getConfigurationParameter( "junit.jupiter.execution.order.random.seed" )
                        .orElse( null );
                if ( !expectedSeed.equals( actualSeed ) )
                {
                    throw new AssertionError( "Expected JUnit random seed " + expectedSeed + " but was " + actualSeed );
                }
            }
        }
    }
}
