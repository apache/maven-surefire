[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)][license]
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.surefire/surefire.svg?label=Maven%20Central)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.apache.maven.plugins%22%20AND%20a%3A%22maven-surefire-plugin%22)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/builds.apache.org/job/maven-box/job/maven-surefire/job/master.svg?style=plastic)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/builds.apache.org/job/maven-box/job/maven-surefire/job/master.svg?style=plastic)][test-results]
[![CI](https://img.shields.io/badge/CI-Jenkins-red.svg?style=plastic)](https://jenkins-ci.org/)
[![forks](https://img.shields.io/github/forks/apache/maven-surefire.svg?style=social&label=Fork)](https://github.com/apache/maven-surefire/)


# The Maven Community

[![slack](https://img.shields.io/badge/slack-18/1138-pink.svg)](https://the-asf.slack.com)
[![chat](https://www.irccloud.com/invite-svg?channel=maven&amp;hostname=irc.freenode.net&amp;port=6697&amp;ssl=1)](https://maven.apache.org/community.html) [Join us @ irc://freenode/maven] or [Webchat with us @channel maven]


# Project Documentation

[![Maven 3.0 Plugin API](https://img.shields.io/badge/maven%20site-documentation-blue.svg?style=plastic)](https://maven.apache.org/surefire/)

Usage of [maven-surefire-plugin], [maven-failsafe-plugin], [maven-surefire-report-plugin]


# Development Information

In order to build Surefire project use **Maven 3.1.0+** and **JDK 1.8**.   

But in order to run IT tests, you can do:   

* In order to run tests for a release check during the vote the following memory requirements are needed:   
  **(on Linux/Unix)** *export MAVEN_OPTS="-server -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true -Dhttps.protocols=TLSv1"*  
  **(on Windows)** *set MAVEN_OPTS="-server -Xmx256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true -Dhttps.protocols=TLSv1"*    
* In order to run the build with **JDK 9** **on Windows** (**on Linux/Unix modify system property jdk.home**):  
  *mvn install site site:stage -P reporting,run-its "-Djdk.home=e:\Program Files\Java\jdk9\"*
* In order to run the build with **JDK 11**:
  *mvn install site site:stage -P reporting,run-its "-Djdk.home=e:\Program Files\Java\jdk11\"*
  

### Deploying web site

See http://maven.apache.org/developers/website/deploy-component-reference-documentation.html

[![Built with Maven](http://maven.apache.org/images/logos/maven-feather.png)](https://maven.apache.org/surefire/)



[Join us @ irc://freenode/maven]: https://www.irccloud.com/invite?channel=maven&amp;hostname=irc.freenode.net&amp;port=6697&amp;ssl=1
[Webchat with us @channel maven]: http://webchat.freenode.net/?channels=%23maven
[JIRA Change Log]: https://issues.apache.org/jira/browse/SUREFIRE/?selectedTab=com.atlassian.jira.jira-projects-plugin:changelog-panel
[maven-surefire-plugin]: https://maven.apache.org/surefire/maven-surefire-plugin/usage.html
[maven-failsafe-plugin]: https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html
[maven-surefire-report-plugin]: https://maven.apache.org/surefire/maven-surefire-report-plugin/usage.html
