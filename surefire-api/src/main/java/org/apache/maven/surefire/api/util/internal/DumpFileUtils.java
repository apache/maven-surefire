/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.api.util.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Dumps a text or exception in dump file.
 * Each call logs a date when it was written to the dump file.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class DumpFileUtils {
    private DumpFileUtils() {
        throw new IllegalStateException("no instantiable constructor");
    }

    /**
     * New dump file. Synchronized object appears in main memory and perfectly visible in other threads.
     *
     * @param reportsDir    only report directory
     * @param dumpFileName    dump file name
     */
    public static synchronized File newDumpFile(File reportsDir, String dumpFileName) {
        reportsDir.mkdirs();
        return new File(reportsDir, dumpFileName);
    }

    public static void dumpException(Throwable t, File dumpFile) {
        dumpException(t, null, dumpFile);
    }

    public static void dumpException(Throwable t, String msg, File dumpFile) {
        try {
            if (t != null && dumpFile != null && (dumpFile.exists() || mkdirs(dumpFile) && dumpFile.createNewFile())) {
                try (PrintWriter pw = new PrintWriter(createWriter(dumpFile))) {
                    if (msg != null) {
                        pw.append(msg).append(StringUtils.NL);
                    }
                    t.printStackTrace(pw);
                    pw.append(StringUtils.NL).append(StringUtils.NL);
                }
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    public static void dumpText(String msg, File dumpFile) {
        try {
            if (msg != null
                    && dumpFile != null
                    && (dumpFile.exists() || mkdirs(dumpFile) && dumpFile.createNewFile())) {
                try (Writer writer = createWriter(dumpFile)) {
                    writer.append(msg).append(StringUtils.NL).append(StringUtils.NL);
                }
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    public static String newFormattedDateFileName() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss_SSS").format(new Date());
    }

    private static Writer createWriter(File dumpFile) throws IOException {
        return new OutputStreamWriter(new FileOutputStream(dumpFile, true), UTF_8)
                .append("# Created at ")
                .append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()))
                .append(StringUtils.NL);
    }

    private static boolean mkdirs(File dumpFile) {
        File dir = dumpFile.getParentFile();
        return dir.exists() || dir.mkdirs();
    }
}
