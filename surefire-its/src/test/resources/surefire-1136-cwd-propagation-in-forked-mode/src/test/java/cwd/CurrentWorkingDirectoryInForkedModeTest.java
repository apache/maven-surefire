package cwd;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CurrentWorkingDirectoryInForkedModeTest
{

    @Test
    public void testCurrentWorkingDirectoryPropagation()
        throws Exception
    {

        File projectDirectory = new File( System.getProperty( "maven.project.base.directory" ) );
        File forkDirectory = new File( projectDirectory, "cwd_1" );
        forkDirectory.deleteOnExit();

        // user.dir and current working directory must be aligned, base directory is not affected
        assertEquals( projectDirectory.getCanonicalPath(), System.getProperty( "basedir" ) );
        assertEquals( forkDirectory.getCanonicalPath(), System.getProperty( "user.dir" ) );
        assertEquals( forkDirectory.getCanonicalPath(), new File( "." ).getCanonicalPath() );

        // original working directory (before variable expansion) should not be created
        assertFalse( new File( "cwd_${surefire.forkNumber}" ).exists() );

    }

}
