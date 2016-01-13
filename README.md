[![Built with Maven](http://maven.apache.org/images/logos/maven-feather.png)](https://maven.apache.org/surefire/)
[![CI](https://img.shields.io/badge/CI-Jenkins-red.svg?style=flat-square)](https://jenkins-ci.org/)

[![dependencies](https://www.versioneye.com/java/org.apache.maven.plugins:maven-surefire-plugin/badge.svg?style=flat)](https://builds.apache.org/job/maven-surefire/depgraph-view/) Maven 2.2.1 Plugin API

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.maven.surefire/surefire/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.apache.maven.surefire/surefire)
[![license](http://img.shields.io/:license-apache-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![coverage](https://img.shields.io/jenkins/c/https/builds.apache.org/maven-surefire.svg)](https://builds.apache.org/job/maven-surefire/jacoco/)
[![tests](https://img.shields.io/jenkins/t/https/builds.apache.org/maven-surefire.svg)](https://img.shields.io/jenkins/t/https/builds.apache.org/maven-surefire.svg)
[![Build Status](https://builds.apache.org/job/maven-surefire/badge/icon)](https://builds.apache.org/job/maven-surefire)
[![Build Status](https://builds.apache.org/job/maven-surefire-windows/badge/icon)](https://builds.apache.org/job/maven-surefire-windows)
[![Build Status](https://builds.apache.org/job/maven-surefire-mvn-2.2.1/badge/icon)](https://builds.apache.org/job/maven-surefire-mvn-2.2.1)

Surefire needs Maven 3.0.5 and JDK 1.6+ to be built.
But in order to run IT tests, you can do:
* -DmavenHomeUsed= path to a Maven 2.x home
* or -Pmaven-2.2.1, this profile will download Maven 2.2.1 distribution and use it for integration tests.
In order to run tests for a release check during the vote the following memory requirements are needed:
export MAVEN_OPTS="-Xmx768m -XX:MaxPermSize=1g -XX:SoftRefLRUPolicyMSPerMB=50 -Djava.awt.headless=true"

Deploying web site
------------------

see http://maven.apache.org/developers/website/deploy-component-reference-documentation.html
