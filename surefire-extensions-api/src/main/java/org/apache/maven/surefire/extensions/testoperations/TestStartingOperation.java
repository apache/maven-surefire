package org.apache.maven.surefire.extensions.testoperations;

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.UniqueID;

public final class TestStartingOperation extends TestOperation<ReportEntry>
{
    private final ReportEntry event;
    private final UniqueID sourceId;

    public TestStartingOperation( ReportEntry event )
    {
        this.event = event;
        sourceId = event.getUniqueId();
    }

    @Override
    public UniqueID getSourceId()
    {
        return sourceId;
    }
}
