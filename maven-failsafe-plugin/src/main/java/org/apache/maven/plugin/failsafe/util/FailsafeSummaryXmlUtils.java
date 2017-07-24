package org.apache.maven.plugin.failsafe.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.failsafe.xmlsummary.ErrorType;
import org.apache.maven.plugin.failsafe.xmlsummary.FailsafeSummary;
import org.apache.maven.surefire.suite.RunResult;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.apache.maven.surefire.util.internal.StringUtils.UTF_8;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class FailsafeSummaryXmlUtils
{
    private FailsafeSummaryXmlUtils()
    {
        throw new IllegalStateException( "No instantiable constructor." );
    }

    public static RunResult toRunResult( File failsafeSummaryXml ) throws JAXBException
    {
        FailsafeSummary failsafeSummary = JAXB.unmarshal( failsafeSummaryXml, FailsafeSummary.class );

        return new RunResult( failsafeSummary.getCompleted(), failsafeSummary.getErrors(),
                                    failsafeSummary.getFailures(), failsafeSummary.getSkipped(),
                                    failsafeSummary.getFailureMessage(), failsafeSummary.isTimeout()
        );
    }

    public static void fromRunResultToFile( RunResult fromRunResult, File toFailsafeSummaryXml )
            throws JAXBException, IOException
    {
        fromRunResultToFile( fromRunResult, toFailsafeSummaryXml, UTF_8 );
    }

    public static void fromRunResultToFile( RunResult fromRunResult, File toFailsafeSummaryXml, Charset encoding )
            throws JAXBException, IOException
    {
        FailsafeSummary summary = new FailsafeSummary();
        summary.setCompleted( fromRunResult.getCompletedCount() );
        summary.setFailureMessage( fromRunResult.getFailure() );
        summary.setErrors( fromRunResult.getErrors() );
        summary.setFailures( fromRunResult.getFailures() );
        summary.setSkipped( fromRunResult.getSkipped() );
        summary.setTimeout( fromRunResult.isTimeout() );
        Integer errorCode = fromRunResult.getFailsafeCode();
        summary.setResult( errorCode == null ? null : ErrorType.fromValue( String.valueOf( errorCode ) ) );

        JAXB.marshal( summary, encoding, toFailsafeSummaryXml );
    }

    public static void writeSummary( RunResult mergedSummary, File mergedSummaryFile, boolean inProgress,
                                     Charset encoding )
            throws IOException, JAXBException
    {
        if ( !mergedSummaryFile.getParentFile().isDirectory() )
        {
            //noinspection ResultOfMethodCallIgnored
            mergedSummaryFile.getParentFile().mkdirs();
        }

        if ( mergedSummaryFile.exists() && inProgress )
        {
            RunResult runResult = toRunResult( mergedSummaryFile );
            mergedSummary = mergedSummary.aggregate( runResult );
        }

        fromRunResultToFile( mergedSummary, mergedSummaryFile, encoding );
    }
}
