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

Build Docker image and run tests
========================

In current `.` directory is your project which starts with root POM.

(including `.` at the end of next line)

For Windows/macOS Users:

    $ docker build --no-cache -t my-image:1 -f ./Dockerfile .
    $ docker run -it --rm my-image:1 /bin/sh

For Linux Users:

    $ sudo docker build --no-cache -t my-image:1 -f ./Dockerfile .
    $ sudo docker run -it --rm my-image:1 /bin/sh

Run the command `mvn test` in the shell console of docker.

Dockerfile in current directory
========================

    FROM maven:3.5.3-jdk-8-alpine
    COPY ./. /

The test
========================

Location in current directory.

    src/test/java/MyTest.java

Simple test waiting 3 seconds:

    import org.junit.Test;
    
    public class MyTest {
        @Test
        public void test() throws InterruptedException {
            Thread.sleep(3000L);
        }
    }


POM
========================

The `pom.xml`:

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
    
        <groupId>x</groupId>
        <artifactId>y</artifactId>
        <version>1.0</version>
    
        <dependencies>
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>4.13</version>
            </dependency>
        </dependencies>
    
        <build>
            <plugins>
                <plugin>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>latest plugin version here</version>
                </plugin>
            </plugins>
        </build>
    
    </project>

