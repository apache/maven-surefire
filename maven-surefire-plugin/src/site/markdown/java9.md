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

Using JDK Deprecated Modules
========================

Since Java 9 some modules previously bundled with the JDK are disabled by default.

Version 2.20.1 of the plugin added automatically "--add-modules java.se.ee" option to the command line of forked JVMs (unless already specified by user) in order to ease the transition of applications to Java 9.

From 2.21 onwards the plugin will not add that option: the recommended way of using those modules is to add explicit dependencies of the maintained versions of such libraries.

This is a reference of the versions which were bundled with Java 8:

**Commons Annotations**

javax.annotation:javax.annotation-api:1.3.1

**JavaBeans Activation Framework**

javax.activation:javax.activation-api:1.2.0

**Java Transaction API**

javax.transaction:javax.transaction-api:1.2

**JAXB**

javax.xml.bind:jaxb-api:2.3.0

org.glassfish.jaxb:jaxb-runtime:2.3.0 (implementation)

**JAX-WS**

javax.xml.ws:jaxws-api:2.3.0

com.sun.xml.ws:jaxws-rt:2.3.0 (implementation)

The source code for each of these is maintained at [https://github.com/javaee](https://github.com/javaee)


<a name="head3"></a> Selecting JDK by the Toolchains API in plugin configuration
========================

    <configuration>
        [...]
        <jdkToolchain>
            <version>11</version>
            <vendor>sun</vendor>
        </jdkToolchain>
        [...]
    </configuration>

The above example assumes that your **toolchains.xml** contains valid entries with these values.

    <toolchains xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 https://maven.apache.org/xsd/toolchains-1.1.0.xsd">
      <toolchain>
        <type>jdk</type>
        <provides>
          <version>1.8</version>
          <vendor>sun</vendor>
          <id>jdk8</id>
        </provides>
        <configuration>
          <jdkHome>/path/to/openjdk8</jdkHome>
        </configuration>
      </toolchain>
      <toolchain>
        <type>jdk</type>
        <provides>
          <version>11</version>
          <vendor>sun</vendor>
          <id>jdk11</id>
        </provides>
        <configuration>
          <jdkHome>/path/to/openjdk/11</jdkHome>
        </configuration>
      </toolchain>
    </toolchains>


Java 9 in configuration of plugin
========================

The plugin provides you with configuration parameter **jvm** which can point to the path of executable Java in JDK, e.g.:

    <configuration>
        <jvm>/path/to/jdk9/bin/java</jvm>
    </configuration>

Now you can run the build with tests on the top of Java 9.

This is highly unrecommended configuration due to the fact that this solution is directly specifying the path
with the JDK and thus it is not smoothly transferable to another build systems.


Maven Toolchains with JDK 9
========================

Since the version **3.0.0-M5** you can use the standard way to switch the JDK within the execution of the plugin.
For more information see the [chapter 'Selecting JDK by the Toolchains API in plugin configuration'](#head3).


The plugin **maven-toolchains-plugin** should be used along with old versions of Surefire or Failsafe plugin.
In this example you can see how to switch the JDK by Toolchain **(-t ...)** in the entire build (on Windows).

    $ mvn -t /path/to/.m2/toolchains.xml test
    
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

Also see the [full documentation for surefire toolchains](examples/toolchains.html) configuration options.
