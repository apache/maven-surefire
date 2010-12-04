package org.apache.maven.surefire.junitcore;

import org.apache.maven.surefire.booter.ProviderDetector;
import org.apache.maven.surefire.providerapi.ProviderFactory;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Kristian Rosenvold
 */
public class SpiTest
{
    @Test
    public void detectionOfProvider()
        throws IOException
    {
        ProviderDetector providerDetector = new ProviderDetector();
        final Object[] objects =
            ProviderDetector.loadServices( ProviderFactory.class, this.getClass().getClassLoader() );
        assertNotNull( objects);
    }
}
