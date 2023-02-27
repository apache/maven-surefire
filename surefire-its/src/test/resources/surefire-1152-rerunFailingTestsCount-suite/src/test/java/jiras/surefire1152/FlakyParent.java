package jiras.surefire1152;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

public class FlakyParent
{
    // set of test classes which have previously invoked testFlakyParent
    private static final Set<Class<?>> PREVIOUSLY_RUN = new HashSet<>();

    @Test
    public void testFlakyParent()
    {
        Class<?> clazz = getClass();
        if ( !PREVIOUSLY_RUN.contains( clazz ) )
        {
            PREVIOUSLY_RUN.add( clazz );
            fail( "deliberately flaky test (should pass the next time)" );
        }
    }
}
