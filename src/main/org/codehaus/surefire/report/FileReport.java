package org.codehaus.surefire.report;

import java.io.IOException;

public class FileReport
    extends OutputStreamReport
{
    private String file;

    public FileReport( String filename )
        throws IOException
    {
        super( filename );

        this.file = filename;
    }

    public String getFile()
    {
        return file;
    }
}
