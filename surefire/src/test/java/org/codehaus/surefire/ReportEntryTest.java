package org.codehaus.surefire;

import junit.framework.TestCase;
import org.codehaus.surefire.report.ReportEntry;

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class ReportEntryTest
    extends TestCase
{
    public ReportEntryTest( String name )
    {
        super( name );
    }

    public void testFoo()
    {
        ReportEntry e = new ReportEntry( "one", "two", "three" );
    }
}
