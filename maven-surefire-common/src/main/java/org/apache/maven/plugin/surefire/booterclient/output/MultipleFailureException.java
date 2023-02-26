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
package org.apache.maven.plugin.surefire.booterclient.output;

import java.io.IOException;
import java.util.function.Function;

final class MultipleFailureException extends IOException {
    void addException(Throwable exception) {
        addSuppressed(exception);
    }

    boolean hasNestedExceptions() {
        return getSuppressed().length != 0;
    }

    @Override
    public String getLocalizedMessage() {
        return toMessage(Throwable::getLocalizedMessage);
    }

    @Override
    public String getMessage() {
        return toMessage(Throwable::getMessage);
    }

    private String toMessage(Function<Throwable, String> msg) {
        StringBuilder messages = new StringBuilder();
        for (Throwable exception : getSuppressed()) {
            if (messages.length() != 0) {
                messages.append('\n');
            }
            String message = msg.apply(exception);
            messages.append(message == null ? exception.toString() : message);
        }
        return messages.toString();
    }
}
