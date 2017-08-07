<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

Java 9 in JAVA_HOME
========================

    $ export JAVA_HOME=/path/to/jdk9
    $ mvn test

The plugin will automatically add `--add-modules ALL-SYSTEM` on JVM argument in CLI (unless already specified by user)
and all Java 9 API is provided to run your tests.


Java 9 in configuration of plugin
========================

The plugin provides you with configuration parameter `jvm` which can point to path of executable Java in JDK, e.g.:

    <configuration>
        <jvm>/path/to/jdk9/bin/java</jvm>
    </configuration>

Now you can run the build with tests on the top of Java 9.


Maven Toolchains with JDK 9
========================

This is an example on Windows to run unit tests with custom path to Toolchain **(-t ...)**.

    $ mvn -t D:\.m2\toolchains.xml test
    
Without **(-t ...)** the Toolchain should be located in **${user.home}/.m2/toolchains.xml**.

The content of **toolchains.xml** would become as follows however multiple different JDKs can be specified.

    <toolchains xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 https://maven.apache.org/xsd/toolchains-1.1.0.xsd">
      <toolchain>
        <type>jdk</type>
        <provides>
          <version>9</version>
          <vendor>oracle</vendor>
          <id>jdk9</id>
        </provides>
        <configuration>
          <jdkHome>/path/to/jdk9</jdkHome>
        </configuration>
      </toolchain>
    </toolchains>

Your POM should specify the plugin which activates only particular JDK in *toolchains.xml* which specifies version **9**:

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-toolchains-plugin</artifactId>
        <version>1.1</version>
        <executions>
          <execution>
            <phase>validate</phase>
            <goals>
              <goal>toolchain</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <toolchains>
            <jdk>
              <version>9</version>
            </jdk>
          </toolchains>
        </configuration>
    </plugin>

Now you can run the build with tests on the top of Java 9.
