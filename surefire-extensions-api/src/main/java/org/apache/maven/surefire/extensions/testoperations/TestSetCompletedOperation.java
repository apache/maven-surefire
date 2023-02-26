package org.apache.maven.surefire.extensions.testoperations;

import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.report.UniqueID;

public final class TestSetCompletedOperation extends TestOperation<TestSetReportEntry>
{
    private final TestSetReportEntry event;
    private final UniqueID sourceId;

    public TestSetCompletedOperation( TestSetReportEntry event )
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
