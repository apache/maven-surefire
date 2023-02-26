package org.apache.maven.surefire.extensions.testoperations;

import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.report.UniqueID;

public final class TestSetStartingOperation extends TestOperation<TestSetReportEntry>
{
    private final TestSetReportEntry event;
    private final UniqueID sourceId;

    public TestSetStartingOperation( TestSetReportEntry event )
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
