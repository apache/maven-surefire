package org.apache.maven.surefire.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SucceedingTest
{
    @Rule
    public TestName name = new TestName();

    @Test
    public void defaultTestValueIs_Value()
    {
        assertThat( new App().getTest(), equalTo( "value" ) );
    }

    @Test
    public void setTestAndRetrieveValue()
    {
        final App app = new App();
        final String val = "foo";

        app.setTest( val );

        assertThat( app.getTest(), equalTo( val ) );
    }

    @After
    public void writeFile()
        throws IOException
    {
        final File f = new File( "target/tests-run", getClass().getName() + ".txt" );
        f.getParentFile().mkdirs();

        FileWriter w = null;

        try
        {
            w = new FileWriter( f, true );
            w.write( name.getMethodName() );
        }
        finally
        {
            if ( w != null )
            {
                try
                {
                    w.close();
                }
                catch ( final IOException e )
                {
                }
            }
        }
    }
}
