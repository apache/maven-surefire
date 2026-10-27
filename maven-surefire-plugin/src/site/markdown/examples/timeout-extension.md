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

Since **3.6.0**, when a forked test JVM runs longer than `forkedProcessTimeoutInSeconds` and gets killed,
Surefire and Failsafe can run extensions first. Use them to grab a thread dump, a heap dump or anything else
from the hung JVM before it's gone.

Extensions only run when a fork actually times out, so `forkedProcessTimeoutInSeconds` must be set.

## Thread dumps with jstack

Surefire ships with a `jstack` extension. It's off by default. Turn it on in the POM:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <forkedProcessTimeoutInSeconds>600</forkedProcessTimeoutInSeconds>
    <forkedProcessTimeoutExtensionContext>
      <jstack.enabled>true</jstack.enabled>
      <!-- optional, defaults to the reports directory -->
      <jstack.output.location>${project.build.directory}/jstacks</jstack.output.location>
    </forkedProcessTimeoutExtensionContext>
  </configuration>
</plugin>
```

or for a single build:

```bash
mvn verify -Dsurefire.timeout.jstack.enabled=true
```

Either one turns it on, for Surefire and Failsafe alike. The dump is written just before the fork is killed, to
`surefire-timeout-jstack-<forkNumber>-<pid>.txt` in the reports directory (or `jstack.output.location` if set).

Good to know:

- `jstack` is looked up in the JDK running Maven, then in `JAVA_HOME`, then on the `PATH`.
- The fork's PID isn't available on Java 8, so no dump is taken there.
- If `jstack` is missing, fails, or takes more than 20 seconds, you get a warning and the build carries on as usual.

## Writing your own extension

### 1. Depend on the API

```xml
<dependency>
  <groupId>org.apache.maven.surefire</groupId>
  <artifactId>surefire-extensions-api</artifactId>
  <version>3.6.0</version>
  <scope>provided</scope>
</dependency>
```

The plugin supplies these classes at runtime, hence `provided`.

### 2. Implement `ForkedProcessTimeoutExtension`

```java
public interface ForkedProcessTimeoutExtension {
    // Called before the fork is killed, the JVM is still alive.
    void onTimeoutDetected(ForkedProcessTimeoutContext context) throws Exception;

    // Called after the timed-out fork has exited.
    void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) throws Exception;
}
```

The context gives you:

| Method | What it returns |
|--------|-----------------|
| `getPid()` | PID of the forked JVM, or `-1` if unknown (Java 8) |
| `getForkNumber()` | Fork number, starting at 1 |
| `getJavaExecutable()` | The `java` binary used by the fork, may be `null` |
| `getReportsDirectory()` | The reports directory, a good place for your files |
| `getTimeoutSeconds()` | The configured `forkedProcessTimeoutInSeconds` |
| `getConsoleLogger()` | Logger for the Maven console |
| `getExtensionContext()` | The `forkedProcessTimeoutExtensionContext` map from the POM |

For example, an extension taking a heap dump with `jcmd`:

```java
public class HeapDumpTimeoutExtension implements ForkedProcessTimeoutExtension {

    @Override
    public void onTimeoutDetected(ForkedProcessTimeoutContext context) throws Exception {
        long pid = context.getPid();
        if (pid <= 0) {
            context.getConsoleLogger().warning("Unknown PID, no heap dump for fork " + context.getForkNumber());
            return;
        }
        String dir = context.getExtensionContext()
                .getOrDefault("heapdump.output.location", context.getReportsDirectory().getAbsolutePath());
        File dump = new File(dir, "heap-fork-" + context.getForkNumber() + "-" + pid + ".hprof");
        dump.getParentFile().mkdirs();
        new ProcessBuilder("jcmd", Long.toString(pid), "GC.heap_dump", dump.getAbsolutePath())
                .inheritIO()
                .start()
                .waitFor();
    }

    @Override
    public void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) {
        // nothing to do here, but you could upload the dump or send a notification
    }
}
```

### 3. Register it

Add the file
`src/main/resources/META-INF/services/org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension`
with your class name:

```
com.example.HeapDumpTimeoutExtension
```

### 4. Add it to the plugin

Add your jar as a dependency of the plugin, not as a test dependency of the project. Settings for your extension
go in `forkedProcessTimeoutExtensionContext`; use your own key prefix so they don't clash with other extensions.

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
      <groupId>com.example</groupId>
      <artifactId>my-timeout-extension</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
</plugin>
```

It works the same way with `maven-failsafe-plugin`.

## How extensions are called

- All registered extensions run, including the `jstack` one when it's enabled.
- Each callback runs once per timed-out fork.
- When several forks time out together, their callbacks run at the same time, so an extension must be safe to call
  from several threads. For a single fork, extensions still run one after the other, before it's killed.
- Each callback gets 30 seconds, then it's cancelled.
- If an extension throws, Surefire logs a warning. The test results don't change.
