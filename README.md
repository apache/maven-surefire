[![Built with Maven](http://maven.apache.org/images/logos/maven-feather.png)](https://maven.apache.org/surefire/) [![CI](https://img.shields.io/badge/CI-Jenkins-red.svg?style=flat-square)](https://jenkins-ci.org/)

[![forks](https://img.shields.io/github/forks/apache/maven-surefire.svg?style=social&label=Fork)](https://github.com/apache/maven-surefire/)

# The Maven Community

[![chat](https://www.irccloud.com/invite-svg?channel=maven&amp;hostname=irc.freenode.net&amp;port=6697&amp;ssl=1)](https://maven.apache.org/community.html) [Join us @ irc://freenode/maven] or [Webchat with us @channel maven]

# Release Notes

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.maven.surefire/surefire/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/org.apache.maven.surefire/surefire)

[JIRA Change Log]

Usage of [maven-surefire-plugin], [maven-failsafe-plugin], [maven-surefire-report-plugin].

# Project Documentation

[![documentation](https://img.shields.io/badge/maven%20site-documentation-blue.svg?style=plastic)](https://maven.apache.org/surefire/)

# Build Status

[![dependencies](https://www.versioneye.com/java/org.apache.maven.plugins:maven-surefire-plugin/badge.svg?style=plastic)](https://builds.apache.org/job/maven-wip/job/maven-surefire/depgraph-view/) Maven 2.2.1 Plugin API

[![license](http://img.shields.io/:license-apache-red.svg?style=plastic)](http://www.apache.org/licenses/LICENSE-2.0.html) [![tests](https://img.shields.io/jenkins/t/https/builds.apache.org/job/maven-box/job/maven-surefire/job/master.svg?style=plastic)](https://builds.apache.org/job/maven-box/job/maven-surefire/job/master/lastBuild/testReport/) [![Build Status](https://builds.apache.org/job/maven-box/job/maven-surefire/job/master/badge/icon?style=plastic)](https://builds.apache.org/job/maven-box/job/maven-surefire/job/master/)

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

[Join us @ irc://freenode/maven]: https://www.irccloud.com/invite?channel=maven&amp;hostname=irc.freenode.net&amp;port=6697&amp;ssl=1
[Webchat with us @channel maven]: http://webchat.freenode.net/?channels=%23maven
[JIRA Change Log]: https://issues.apache.org/jira/browse/SUREFIRE/?selectedTab=com.atlassian.jira.jira-projects-plugin:changelog-panel
[maven-surefire-plugin]: https://maven.apache.org/surefire/maven-surefire-plugin/usage.html
[maven-failsafe-plugin]: https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html
[maven-surefire-report-plugin]: https://maven.apache.org/surefire/maven-surefire-report-plugin/usage.html
