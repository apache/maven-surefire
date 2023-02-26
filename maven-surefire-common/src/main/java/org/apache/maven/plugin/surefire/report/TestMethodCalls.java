package org.apache.maven.plugin.surefire.report;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.surefire.api.report.UniqueID;
import org.apache.maven.surefire.extensions.ReportData;
import org.apache.maven.surefire.extensions.testoperations.TestOperation;

import static java.util.stream.Collectors.toMap;

final class TestMethodCalls
{
    private final ReportData reportData = new ReportData();

    void addOperation( TestOperation<?> op )
    {
        reportData.addOperation( op );
    }

    void addRerunOperation( TestOperation<?> op )
    {
        reportData.addRetryOperation( op );
    }

    Map<UniqueID, ReportData> mapTestStats()
    {
        return reportData.getIds()
            .stream()
            .collect( toMap(
                id -> id,
                id ->
                {
                    ReportData rep = new ReportData();
                    reportData.filterOperations( id ).forEach( rep::addOperation );
                    reportData.filterRerunOperations( id ).forEach( rep::addRetryOperation );
                    return rep;
                }, (u, v) -> { throw new IllegalStateException(); }, LinkedHashMap::new ) );
    }
}
