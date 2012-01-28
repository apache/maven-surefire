package org.apache.maven.surefire.junitcore;

import org.junit.runners.model.RunnerScheduler;

/**
 * @author <a href="mailto:kristian@zenior.no">Kristian Rosenvold</a>
 */
class SynchronousRunner
    implements RunnerScheduler
{
    public void schedule( final Runnable childStatement )
    {
        childStatement.run();
    }

    public void finished()
    {
    }
}
