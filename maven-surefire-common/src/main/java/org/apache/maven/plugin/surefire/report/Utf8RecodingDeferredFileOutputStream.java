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
package org.apache.maven.plugin.surefire.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.apache.maven.surefire.api.util.SureFireFileManager;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.notExists;
import static java.nio.file.Files.size;
import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;

/**
 * A deferred file output stream decorator that encodes the string from the VM to UTF-8.
 * <br>
 * The deferred file is temporary file, and it is created at the first {@link #write(String, boolean) write}.
 * The {@link #writeTo(OutputStream) reads} can be called anytime.
 * It is highly recommended to {@link #commit() commit} the cache which would close the file stream
 * and subsequent reads may continue.
 * The {@link #free()} method would commit and delete the deferred file.
 *
 * @author Andreas Gudian
 */
final class Utf8RecodingDeferredFileOutputStream {
    private static final byte[] NL_BYTES = NL.getBytes(UTF_8);
    public static final int CACHE_SIZE = 64 * 1024;

    private final String channel;
    private Path file;
    private RandomAccessFile storage;
    private boolean closed;
    private SoftReference<byte[]> largeCache;
    private ByteBuffer cache;
    private boolean isDirty;

    Utf8RecodingDeferredFileOutputStream(String channel) {
        this.channel = requireNonNull(channel);
    }

    public synchronized void write(String output, boolean newLine) throws IOException {
        if (closed) {
            return;
        }

        if (storage == null) {

            file = SureFireFileManager.createTempFile(channel, "deferred").toPath();
            storage = new RandomAccessFile(file.toFile(), "rw");
        }

        if (output == null) {
            output = "null";
        }

        if (cache == null) {
            cache = ByteBuffer.allocate(CACHE_SIZE);
        }

        isDirty = true;

        byte[] decodedString = output.getBytes(UTF_8);
        int newLineLength = newLine ? NL_BYTES.length : 0;
        if (cache.remaining() >= decodedString.length + newLineLength) {
            cache.put(decodedString);
            if (newLine) {
                cache.put(NL_BYTES);
            }
        } else {
            ((Buffer) cache).flip();
            int minLength = cache.remaining() + decodedString.length + NL_BYTES.length;
            byte[] buffer = getLargeCache(minLength);
            int bufferLength = 0;
            System.arraycopy(
                    cache.array(),
                    cache.arrayOffset() + ((Buffer) cache).position(),
                    buffer,
                    bufferLength,
                    cache.remaining());
            bufferLength += cache.remaining();
            ((Buffer) cache).clear();

            System.arraycopy(decodedString, 0, buffer, bufferLength, decodedString.length);
            bufferLength += decodedString.length;

            if (newLine) {
                System.arraycopy(NL_BYTES, 0, buffer, bufferLength, NL_BYTES.length);
                bufferLength += NL_BYTES.length;
            }

            storage.write(buffer, 0, bufferLength);
        }
    }

    public synchronized long getByteCount() {
        try {
            sync();
            if (storage != null && !closed) {
                storage.getFD().sync();
            }
            return file == null ? 0 : size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    @SuppressWarnings("checkstyle:innerassignment")
    public synchronized void writeTo(OutputStream out) throws IOException {
        if (file != null && notExists(file)) {
            storage = null;
        }

        if ((storage == null && file != null)
                || (storage != null && !storage.getChannel().isOpen())) {
            storage = new RandomAccessFile(file.toFile(), "rw");
        }

        if (storage != null) {
            sync();
            final long currentFilePosition = storage.getFilePointer();
            storage.seek(0L);
            try {
                byte[] buffer = new byte[CACHE_SIZE];
                for (int readCount; (readCount = storage.read(buffer)) != -1; ) {
                    out.write(buffer, 0, readCount);
                }
            } finally {
                storage.seek(currentFilePosition);
            }
        }
    }

    public synchronized void commit() {
        if (storage == null) {
            return;
        }

        try {
            sync();
            storage.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            storage = null;
            cache = null;
            largeCache = null;
        }
    }

    public synchronized void free() {
        if (!closed) {
            closed = true;
            if (file != null) {
                try {
                    commit();
                    // todo delete( file ); uncomment L485 assertThat( file ).doesNotExist() in StatelessXmlReporterTest
                } catch (/*todo uncomment IOException |*/ UncheckedIOException e) {
                    /*todo uncomment file.toFile()
                    .deleteOnExit();*/
                } finally {
                    // todo should be removed after uncommented delete( file )
                    file.toFile().deleteOnExit();
                }

                storage = null;
                cache = null;
                largeCache = null;
            }
        }
    }

    private void sync() throws IOException {
        if (!isDirty) {
            return;
        }

        isDirty = false;

        if (storage != null && cache != null) {
            ((Buffer) cache).flip();
            byte[] array = cache.array();
            int offset = cache.arrayOffset() + ((Buffer) cache).position();
            int length = cache.remaining();
            ((Buffer) cache).clear();
            storage.write(array, offset, length);
            // the data that you wrote with the mode "rw" may still only be kept in memory and may be read back
            // storage.getFD().sync();
        }
    }

    @SuppressWarnings("checkstyle:innerassignment")
    private byte[] getLargeCache(int minLength) {
        byte[] buffer;
        if (largeCache == null || (buffer = largeCache.get()) == null || buffer.length < minLength) {
            buffer = new byte[minLength];
            largeCache = new SoftReference<>(buffer);
        }
        return buffer;
    }
}
