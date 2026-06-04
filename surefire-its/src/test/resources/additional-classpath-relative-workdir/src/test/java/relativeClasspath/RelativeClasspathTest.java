package relativeClasspath;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Verifies that a resource placed in a directory referenced via a relative
 * {@code additionalClasspathElement} is accessible at test runtime.
 *
 * The pom.xml sets {@code workingDirectory} to {@code ${project.basedir}/fork-wd} and
 * {@code additionalClasspathElement} to {@code ../cp-extra}.  The element is relative and
 * must be resolved against the fork's working directory, not against the manifest-JAR
 * location, so that it ends up pointing at {@code ${project.basedir}/cp-extra}.
 */
public class RelativeClasspathTest {

    @Test
    public void relativeClasspathElementMustBeAccessible() {
        assertNotNull(
                "relative-cp-marker.txt must be loadable from the relative additionalClasspathElement",
                getClass().getClassLoader().getResourceAsStream("relative-cp-marker.txt"));
    }
}
