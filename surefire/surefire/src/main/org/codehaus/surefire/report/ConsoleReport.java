package org.codehaus.surefire.report;

/*
 * Copyright (C) 2001-2003 Artima Software, Inc. All rights reserved.
 * Licensed under the Open Software License version 1.0.
 *
 * A copy of the Open Software License version 1.0 is available at:
 *     http://www.artima.com/surefire/osl10.html
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Artima Software, Inc. For more
 * information on Artima Software, Inc., please see:
 *     http://www.artima.com/
 */

public class ConsoleReport
    extends OutputStreamReport
{
    public ConsoleReport()
    {
        super( System.out );
    }

    public void dispose()
    {
    }
}
