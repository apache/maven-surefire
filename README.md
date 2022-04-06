<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<img src="https://maven.apache.org/images/maven-logo-black-on-white.png" alt="Maven"/>

Contributing to [Apache Maven Surefire](https://maven.apache.org/surefire/)
======================

[![ASF Jira](https://img.shields.io/endpoint?url=https%3A%2F%2Fmaven.apache.org%2Fbadges%2Fasf_jira-SUREFIRE.json&style=for-the-badge)][jira]
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.surefire/surefire.svg?label=Maven%20Central&style=for-the-badge)](https://search.maven.org/artifact/org.apache.maven.plugins/maven-surefire-plugin)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License&style=for-the-badge)][license]

[![CI](https://img.shields.io/badge/CI-Jenkins-blue.svg?style=for-the-badge)](https://jenkins-ci.org/)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-surefire/job/master.svg?style=for-the-badge)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-surefire/job/master.svg?style=for-the-badge)][test-results]
[![Jenkins JaCoCo](https://img.shields.io/jenkins/coverage/jacoco/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-surefire/job/master.svg?style=for-the-badge&color=green)](https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-surefire/job/master/lastBuild/jacoco/)

[![Verify](https://github.com/apache/maven-surefire/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-surefire/actions/workflows/maven-verify.yml)

# The Maven Community

[![slack](https://img.shields.io/badge/slack-18/1138-pink.svg?style=for-the-badge)](https://the-asf.slack.com)
[![forks](https://img.shields.io/github/forks/apache/maven-surefire.svg?style=for-the-badge&label=Fork)](https://github.com/apache/maven-surefire/)


# Project Documentation

[![Maven 3.0 Plugin API](https://img.shields.io/badge/maven%20site-documentation-blue.svg?style=for-the-badge)](https://maven.apache.org/surefire/)

Usage of [maven-surefire-plugin], [maven-failsafe-plugin], [maven-surefire-report-plugin]


# Development Information

Build the Surefire project using **Maven 3.2.5+** and **JDK 1.8+**.  

* In order to run tests for a release check during the Vote, the following memory requirements are needed:   

  On Linux/Unix:
  ```
  export MAVEN_OPTS="-server -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true"
  ```
  On Windows:
  ```
  set MAVEN_OPTS="-server -Xmx256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true"
  ```

* In order to build and run the tests:  
  ```
  mvn install site site:stage -P reporting,run-its
  ```

* To set up the project in [Eclipse IDE](https://www.eclipse.org/), please follow these steps:

  * Build module `surefire-shared-utils` with profile `ide-development` and install it into the local maven repository using this Maven command:
    ```
    mvn install -P ide-development -f surefire-shared-utils/pom.xml
    ```
  * Build module `surefire-grouper` in order to generate and compile sources into `target/generated-sources/javacc` using this Maven command:
    ```
    mvn compile -f surefire-grouper/pom.xml
    ```
  * In Eclipse, select _File > Import ... > Maven Project_

     * Select all projects (poms) except `surefire-shared-utils`,
       enter profile `ide-development` in _Advanced -> Profiles_
     * Check module `surefire-grouper` has source folder `target/generated-sources/javacc`.
       If not, add it manually in the module's project properties

* Setup for development in [IntelliJ IDEA](https://www.jetbrains.com/idea/) should work out of the box.

### Deploying web site

See http://maven.apache.org/developers/website/deploy-component-reference-documentation.html

[![Built with Maven](http://maven.apache.org/images/logos/maven-feather.png)](https://maven.apache.org/surefire/)


[jira]: https://issues.apache.org/jira/browse/SUREFIRE/
[license]: https://www.apache.org/licenses/LICENSE-2.0
[build]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-surefire/job/master/
[test-results]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-surefire/job/master/lastCompletedBuild/testReport/
[Join us @ irc://freenode/maven]: https://www.irccloud.com/invite?channel=maven&amp;hostname=irc.freenode.net&amp;port=6697&amp;ssl=1
[Webchat with us @channel maven]: http://webchat.freenode.net/?channels=%23maven
[JIRA Change Log]: https://issues.apache.org/jira/browse/SUREFIRE/?selectedTab=com.atlassian.jira.jira-projects-plugin:changelog-panel
[maven-surefire-plugin]: https://maven.apache.org/surefire/maven-surefire-plugin/usage.html
[maven-failsafe-plugin]: https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html
[maven-surefire-report-plugin]: https://maven.apache.org/surefire/maven-surefire-report-plugin/usage.html
