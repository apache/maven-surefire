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

# Forked Process Timeout Extension

Since **3.6.0**, Surefire and Failsafe expose an extension point that is
invoked when a forked test JVM is killed because it exceeded
`forkedProcessTimeoutInSeconds`. The primary motivation is to allow capturing
diagnostic information (such as a `jstack` thread dump) about a hung test
process before it is destroyed.

## SPI

Implement
`org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension` from the
`surefire-extensions-api` artifact:

```java
public interface ForkedProcessTimeoutExtension {
    // Called before the KILL signal — the forked JVM is still alive.
    void onTimeoutDetected(ForkedProcessTimeoutContext context) throws Exception;

    // Called after the forked JVM has exited.
    void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) throws Exception;
}
```

The `ForkedProcessTimeoutContext` exposes:

| Method                | Description                                               |
|-----------------------|-----------------------------------------------------------|
| `getPid()`            | OS PID of the forked JVM, or `-1` if unavailable (Java 8) |
| `getForkNumber()`     | 1-based fork number assigned by Surefire                  |
| `getJavaExecutable()` | Path to the `java` binary used by the fork                |
| `getReportsDirectory()` | Surefire reports directory (good place to write dumps)  |
| `getTimeoutSeconds()` | Configured `forkedProcessTimeoutInSeconds`                |
| `getConsoleLogger()`  | Logger to write to the Maven console                      |
| `getExtensionContext()` | User-supplied `Map<String,String>` from the Mojo (see below) |

Callback failures (any `Throwable`) are caught and logged by Surefire — they
never affect the test result. Each callback is bounded by an internal
30-second time limit so a misbehaving extension cannot stall test execution.

## Passing configuration to extensions

Extensions often need user-supplied configuration (output directory,
toggles, etc.). Surefire and Failsafe expose a single Mojo parameter
`forkedProcessTimeoutExtensionContext` (a `Map<String,String>`) that is
made available to every registered extension via
`ForkedProcessTimeoutContext.getExtensionContext()`:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
    <forkedProcessTimeoutExtensionContext>
      <jstack.output.location>${project.build.directory}/jstacks</jstack.output.location>
      <my.custom.key>my-value</my.custom.key>
    </forkedProcessTimeoutExtensionContext>
  </configuration>
</plugin>
```

Keys are implementation-specific; pick a unique prefix for your extension.

## Registration

Extensions are discovered via the standard `ServiceLoader` mechanism from the
**plugin classpath**. Two steps:

1. Add a file
   `META-INF/services/org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension`
   to your extension JAR, containing the fully qualified class name of your
   implementation.
2. Declare the extension JAR as a `<dependency>` of `maven-surefire-plugin`
   (or `maven-failsafe-plugin`) in the project POM:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>my-timeout-extension</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
</plugin>
```

## Built-in `jstack` extension

Surefire ships with a built-in `JstackTimeoutExtension` that captures a
`jstack` thread dump of the forked JVM and writes it to
`<reportsDirectory>/surefire-timeout-jstack-<forkNumber>-<pid>.txt` just
before the JVM is killed.

It is **disabled by default**. Enable it either via system property:

```bash
mvn test -Dsurefire.timeout.jstack.enabled=true
```

…or directly in the POM through the `forkedProcessTimeoutExtensionContext`
map (no command-line property needed):

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

### Supported extension-context keys

| Key                       | Description                                                          |
|---------------------------|----------------------------------------------------------------------|
| `jstack.enabled`          | Set to `true` to enable the extension from the POM.                  |
| `jstack.output.location`  | Directory where the `surefire-timeout-jstack-*.txt` files are written. When unset, defaults to the Surefire reports directory. |

`jstack` is resolved from `JAVA_HOME/bin/jstack`, falling back to the
`jstack` binary on `PATH`. The thread dump is best-effort: if `jstack` is
missing, the PID is unknown (Java 8), or the call fails, a warning is logged
and the test result is unaffected.
