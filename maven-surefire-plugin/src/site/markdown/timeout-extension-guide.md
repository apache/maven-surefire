<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->

# Forked Process Timeout Extension Guide

Since **3.6.0**, Surefire and Failsafe expose an extension point that fires
when a forked test JVM is killed because it exceeded
`forkedProcessTimeoutInSeconds`. Use it to capture diagnostic information
(thread dumps, heap dumps, JFR recordings, notifications, …) about a hung
test process *before* it is destroyed.

This page walks through:

1. [Enabling the built-in `jstack` extension](#built-in-jstack-extension)
2. [Writing your own extension](#writing-your-own-extension)
3. [Passing configuration to extensions](#passing-configuration-to-extensions)
4. [Lifecycle, threading and error handling](#lifecycle-threading-and-error-handling)

A concise API reference is also available at
[examples/timeout-extension.html](examples/timeout-extension.html).

---

## Built-in `jstack` extension

Surefire ships with a built-in `JstackTimeoutExtension`. When a forked JVM
hits the timeout it captures a `jstack` thread dump of that process and
writes it to a `.txt` file just **before** the kill signal is sent — so the
dump reflects the actual hung state.

### Step 1 — set a sensible timeout

The extension only runs when the kill path is triggered, so make sure a
timeout is configured:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
  </configuration>
</plugin>
```

### Step 2 — enable it

The extension is registered via `META-INF/services` but is **disabled by
default**. Enable it either through the POM…

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
    <forkedProcessTimeoutExtensionContext>
      <jstack.enabled>true</jstack.enabled>
    </forkedProcessTimeoutExtensionContext>
  </configuration>
</plugin>
```

…or per-build via a system property:

```bash
mvn verify -Dsurefire.timeout.jstack.enabled=true
```

Either source enables the extension; if both are set, the property still
wins (it is processed first).

### Step 3 — (optional) choose where dumps land

By default the file is written under the configured reports directory as

```
target/surefire-reports/surefire-timeout-jstack-<forkNumber>-<pid>.txt
```

To override the destination, add the `jstack.output.location` key to the
same map:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
    <forkedProcessTimeoutExtensionContext>
      <jstack.enabled>true</jstack.enabled>
      <jstack.output.location>${project.build.directory}/jstacks</jstack.output.location>
    </forkedProcessTimeoutExtensionContext>
  </configuration>
</plugin>
```

### Requirements

* `jstack` must be available — Surefire looks for it in `${java.home}/bin/`,
  the parent JDK `bin/`, `$JAVA_HOME/bin/` and finally on `PATH`. Building
  with a JRE (Java 8) will fail to find it.
* The PID of the forked JVM must be resolvable; on **Java 8** there is no
  public `Process.pid()` API and the extension will log a warning and skip
  the dump.
* When the call fails for any reason (timeout > 20 s, non-zero exit, etc.)
  a warning is logged and the test result is unaffected.

---

## Writing your own extension

The SPI is a single interface in the `surefire-extensions-api` artifact:

```java
package org.apache.maven.surefire.extensions;

public interface ForkedProcessTimeoutExtension {

    // Called BEFORE the KILL signal — the forked JVM is still alive,
    // ideal place to run jstack, jcmd, capture a JFR snapshot, …
    void onTimeoutDetected(ForkedProcessTimeoutContext context) throws Exception;

    // Called AFTER the forked JVM has exited — good place to upload
    // artifacts, send notifications, etc.
    void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) throws Exception;
}
```

### Step 1 — create the extension module

A standalone Maven module is the simplest distribution unit:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example.surefire</groupId>
  <artifactId>my-timeout-extension</artifactId>
  <version>1.0.0</version>

  <dependencies>
    <dependency>
      <groupId>org.apache.maven.surefire</groupId>
      <artifactId>surefire-extensions-api</artifactId>
      <version>3.6.0</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</project>
```

`scope=provided` keeps the dependency out of the published JAR — the SPI
classes are always supplied by the running Surefire plugin.

### Step 2 — implement the interface

```java
package com.example.surefire;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutContext;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension;

public class HeapDumpTimeoutExtension implements ForkedProcessTimeoutExtension {

    private static final String OUTPUT_KEY = "heapdump.output.location";

    @Override
    public void onTimeoutDetected(ForkedProcessTimeoutContext context) throws Exception {
        ConsoleLogger log = context.getConsoleLogger();
        long pid = context.getPid();
        if (pid <= 0L) {
            log.warning("HeapDumpTimeoutExtension: unknown PID, skipping fork " + context.getForkNumber());
            return;
        }
        String dir = context.getExtensionContext().getOrDefault(
                OUTPUT_KEY, context.getReportsDirectory().getAbsolutePath());
        Path out = Path.of(dir, "heap-fork-" + context.getForkNumber() + "-" + pid + ".hprof");
        Files.createDirectories(out.getParent());

        // Spawn jcmd ${pid} GC.heap_dump
        new ProcessBuilder("jcmd", Long.toString(pid), "GC.heap_dump", out.toString())
                .redirectErrorStream(true)
                .inheritIO()
                .start()
                .waitFor();

        log.info("HeapDumpTimeoutExtension: wrote " + out);
    }

    @Override
    public void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) {
        // optional: upload to S3, post to Slack, etc.
    }
}
```

### Step 3 — register via `ServiceLoader`

Create the file
`src/main/resources/META-INF/services/org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension`
with one fully-qualified class name per line:

```
com.example.surefire.HeapDumpTimeoutExtension
```

### Step 4 — declare the extension in the consumer project

Add the extension JAR as a `<dependency>` of `maven-surefire-plugin` (or
`maven-failsafe-plugin`) — **not** as a project test dependency. Surefire
discovers extensions on the *plugin* classpath:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
    <forkedProcessTimeoutExtensionContext>
      <heapdump.output.location>${project.build.directory}/heap-dumps</heapdump.output.location>
    </forkedProcessTimeoutExtensionContext>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>com.example.surefire</groupId>
      <artifactId>my-timeout-extension</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
</plugin>
```

That's it — when a fork is killed for timeout, both your extension and any
other registered extension (including the built-in `jstack` one if enabled)
are invoked.

---

## Passing configuration to extensions

Surefire and Failsafe expose a single Mojo parameter for **all** timeout
extensions:

```xml
<forkedProcessTimeoutExtensionContext>
  <jstack.output.location>${project.build.directory}/jstacks</jstack.output.location>
  <heapdump.output.location>${project.build.directory}/heap-dumps</heapdump.output.location>
  <slack.webhook>https://hooks.slack.com/services/…</slack.webhook>
</forkedProcessTimeoutExtensionContext>
```

The map is exposed to every extension via
`ForkedProcessTimeoutContext.getExtensionContext()`. Keys are
implementation-specific — pick a unique prefix for each extension to avoid
collisions.

### Context API

| Method                  | Description                                                          |
|-------------------------|----------------------------------------------------------------------|
| `getPid()`              | OS PID of the forked JVM, or `-1` if unavailable (Java 8)            |
| `getForkNumber()`       | 1-based fork number assigned by Surefire                             |
| `getJavaExecutable()`   | Path to the `java` binary used by the fork (may be `null`)           |
| `getReportsDirectory()` | Surefire reports directory                                           |
| `getTimeoutSeconds()`   | Configured `forkedProcessTimeoutInSeconds`                           |
| `getConsoleLogger()`    | Logger writing to the Maven console                                  |
| `getExtensionContext()` | User-supplied `Map<String,String>` from the Mojo parameter           |

### Supported built-in keys

| Key                       | Used by                       | Description                                                                 |
|---------------------------|-------------------------------|-----------------------------------------------------------------------------|
| `jstack.enabled`          | `JstackTimeoutExtension`      | Set to `true` to enable the built-in jstack extension from the POM.         |
| `jstack.output.location`  | `JstackTimeoutExtension`      | Directory for `surefire-timeout-jstack-*.txt`. Defaults to the reports dir. |

---

## Lifecycle, threading and error handling

* **Order of callbacks** — `onTimeoutDetected` is invoked synchronously
  the first time Surefire decides to kill a fork, *before* the KILL
  signal is dispatched. `onForkExited` is invoked once the OS has reaped
  the process.
* **Single fire per fork** — both callbacks fire at most once per forked
  JVM, even if the timeout poll observes the condition repeatedly.
* **Bounded execution** — each callback is invoked on an internal cached
  thread pool and cancelled after **30 seconds**. A misbehaving extension
  cannot stall test execution.
* **Isolation** — any `Throwable` thrown by an extension is logged at
  warn-level and never affects the test result.
* **Classpath** — extensions are loaded with
  `ServiceLoader.load(ForkedProcessTimeoutExtension.class, ForkedProcessTimeoutExtension.class.getClassLoader())`,
  i.e. the **plugin classloader**, not the project test classpath.
* **Failsafe** — the SPI works identically for `maven-failsafe-plugin`.
  Register the extension as a dependency of the failsafe plugin and use
  the same `forkedProcessTimeoutExtensionContext` parameter.
