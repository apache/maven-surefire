package org.codehaus.surefire.util;

import java.io.PrintStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class TeeStream
    extends PrintStream
{
    PrintStream out;

    public TeeStream( PrintStream out1, PrintStream out2 )
    {
        super( out1 );

        this.out = out2;
    }

    public void write( byte buf[], int off, int len )
    {
        try
        {
            super.write( buf, off, len );

            out.write( buf, off, len );
        }
        catch ( Exception e )
        {
        }
    }

    public void close()
    {
        super.close();

        out.close();
    }

    public void flush()
    {
        super.flush();

        out.flush();
    }
}

