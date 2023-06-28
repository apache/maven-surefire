package additionalClasspath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdditionalClasspathDependencyTest
{

    @Test
    public void testDirectDependency() throws ClassNotFoundException
    {
        this.getClass().getClassLoader().loadClass("org.apache.commons.fileupload.FileUpload");
    }

    @Test
    public void testTransitiveCompileDependency() throws ClassNotFoundException
    {
        this.getClass().getClassLoader().loadClass("org.apache.commons.io.IOUtils");
    }

    @Test
    public void testTransitiveProvidedDependency()
    {
        assertThrows(ClassNotFoundException.class, () -> { this.getClass().getClassLoader().loadClass("javax.servlet.http.HttpServletRequest"); });
    }
}
