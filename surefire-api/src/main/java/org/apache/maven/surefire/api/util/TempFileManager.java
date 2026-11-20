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
package org.apache.maven.surefire.api.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Management of temporary files in surefire with support for sub directories of the system's directory
 * of temporary files.<br>
 * The {@link File#createTempFile(String, String)} API creates rather meaningless file names and
 * only in the system's temp directory.<br>
 * This class creates temp files from a prefix, a unique date/timestamp, a short file id and suffix.
 * It also helps you organize temporary files into sub-directories,
 * and thus avoid bloating the temp directory root.<br>
 * Apart from that, this class works in much the same way as {@link File#createTempFile(String, String)}
 * and {@link File#deleteOnExit()}, and can be used as a drop-in replacement.
 *
 * @author Markus Spann
 */
public final class TempFileManager {

    private static final Map<File, TempFileManager> INSTANCES = new LinkedHashMap<>();
    /** An id unique across all file managers used as part of the temporary file's base name. */
    private static final AtomicInteger FILE_ID = new AtomicInteger(1);

    private static final String SUFFIX_TMP = ".tmp";

    private static Thread shutdownHook;

    /** The temporary directory or sub-directory under which temporary files are created. */
    private final File tempDir;
    /** The fixed base name used between prefix and suffix of temporary files created by this class. */
    private final String baseName;

    /** List of successfully created temporary files. */
    private final List<File> tempFiles;

    /** Temporary files are delete on JVM exit if true. */
    private boolean deleteOnExit;

    private TempFileManager(File tempDir) {
        this.tempDir = tempDir;
        this.baseName = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
        this.tempFiles = new ArrayList<>();
    }

    private static File calcTempDir(String subDirName) {
        String tempDirName = subDirName == null ? null : subDirName.trim().isEmpty() ? null : subDirName.trim();
        File tempDirCandidate =
                tempDirName == null ? new File(getJavaIoTmpDir()) : new File(getJavaIoTmpDir(), tempDirName);
        return tempDirCandidate;
    }

    public static TempFileManager instance() {
        return instance((File) null);
    }

    /**
     * Creates an instance using a subdirectory of the system's temporary directory.
     *
     * @param subDirName name of subdirectory
     * @return instance
     */
    public static TempFileManager instance(String subDirName) {
        return instance(calcTempDir(subDirName));
    }

    public static synchronized TempFileManager instance(File tempDir) {

        // on creation of the first instance, register a shutdown hook to delete temporary files of all managers
        if (shutdownHook == null) {
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            while (tg.getParent() != null) {
                tg = tg.getParent();
            }
            shutdownHook = new Thread(
                    tg, TempFileManager::shutdownAll, TempFileManager.class.getSimpleName() + "-ShutdownHook");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        return INSTANCES.computeIfAbsent(tempDir == null ? new File(getJavaIoTmpDir()) : tempDir, TempFileManager::new);
    }

    public synchronized void deleteAll() {
        tempFiles.forEach(File::delete);
        tempFiles.clear();
        tempDir.delete();
    }

    static void shutdownAll() {
        INSTANCES.values().stream().filter(TempFileManager::isDeleteOnExit).forEach(TempFileManager::deleteAll);
    }

    /**
     * Returns the temporary directory or sub-directory under which temporary files are created.
     *
     * @return temporary directory
     */
    public File getTempDir() {
        return tempDir;
    }

    /**
     * Creates a new, uniquely-named temporary file in this object's {@link #tempDir}
     * with user-defined prefix and suffix (both may be null or empty and won't be trimmed).
     * <p>
     * This method behaves similarly to {@link java.io.File#createTempFile(String, String)} and
     * may be used as a drop-in replacement.<br>
     * This method is {@code synchronized} to help ensure uniqueness of temporary files.
     *
     * @param prefix optional file name prefix
     * @param suffix optional file name suffix
     * @return file object
     */
    public synchronized File createTempFile(String prefix, String suffix) {
        // use only the file name from the supplied prefix
        prefix = prefix == null ? "" : (new File(prefix)).getName();
        suffix = suffix == null ? "" : suffix;

        String name = String.join("-", prefix, baseName + "_" + FILE_ID.getAndIncrement()) + suffix;
        File tempFile = new File(tempDir, name);
        if (!name.equals(tempFile.getName())) {
            throw new UncheckedIOException(new IOException("Unable to create temporary file " + tempFile));
        }

        if (tempFiles.contains(tempFile)) {
            // already handed out in this JVM? Try another name
            return createTempFile(prefix, suffix);
        }

        // create temporary directory with owner-only permissions where supported
        if (!tempDir.exists()) {
            try {
                Files.createDirectories(tempDir.toPath());
                setOwnerOnlyPermissions(tempDir.toPath(), "rwx------");
            } catch (IOException ex) {
                throw new UncheckedIOException(
                        new IOException("Unable to create temporary directory " + tempDir.getAbsolutePath(), ex));
            }
        }

        Path path = tempFile.toPath();
        try {
            Files.createFile(path);
            setOwnerOnlyPermissions(path, "rw-------");
        } catch (FileAlreadyExistsException ex) {
            // A plain file or a symlink already occupies the predicted name; never reuse it
            return createTempFile(prefix, suffix);
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to create temporary file " + tempFile.getAbsolutePath(), ex);
        }

        tempFiles.add(tempFile);
        return tempFile;
    }

    private static void setOwnerOnlyPermissions(Path path, String permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        } catch (UnsupportedOperationException e) {
            // Non-POSIX filesystem: the platform's default permissions apply
        }
    }

    public File createTempFile(String prefix) {
        return createTempFile(prefix, SUFFIX_TMP);
    }

    public boolean isDeleteOnExit() {
        return deleteOnExit;
    }

    /**
     * Instructs this file manager to delete its temporary files during JVM shutdown.<br>
     * This status can be turned on and off unlike {@link File#deleteOnExit()}.
     *
     * @param deleteOnExit delete the file in a shutdown hook on JVM exit
     */
    public void setDeleteOnExit(boolean deleteOnExit) {
        this.deleteOnExit = deleteOnExit;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[tempDir=%s, deleteOnExit=%s, baseName=%s, tempFiles=%d]",
                getClass().getSimpleName(), tempDir, deleteOnExit, baseName, tempFiles.size());
    }

    /**
     * Returns the value of system property {@code java.io.tmpdir}, the system's temp directory.
     *
     * @return path to system temp directory
     */
    public static String getJavaIoTmpDir() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (!tmpDir.endsWith(File.separator)) {
            tmpDir += File.separatorChar;
        }
        return tmpDir;
    }
}
