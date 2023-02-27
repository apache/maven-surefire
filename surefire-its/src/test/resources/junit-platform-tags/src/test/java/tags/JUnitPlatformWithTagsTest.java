package tags;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class JUnitPlatformWithTagsTest
{

    @Test
    @Tag("run")
    void run()
    {
    }

    @Test
    @Tag("don't")
    @Tag("run")
    void dontRun()
    {
        fail( "unexpected to call" );
    }

    @Test
    @Tag("don't")
    @Tag("run")
    @Tag("forced")
    void doRun()
    {
    }

    @Test
    void tagless()
    {
        fail( "unexpected to call" );
    }
}
