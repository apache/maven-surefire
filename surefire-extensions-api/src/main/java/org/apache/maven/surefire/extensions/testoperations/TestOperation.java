package org.apache.maven.surefire.extensions.testoperations;

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.UniqueID;

public abstract class TestOperation<T extends ReportEntry>
{
    private final long createdAt = System.currentTimeMillis();
    public abstract UniqueID getSourceId();

    public final long createdAt()
    {
        return createdAt;
    }
}
