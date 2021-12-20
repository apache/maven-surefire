#!/usr/bin/env groovy

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

properties(
    [
        buildDiscarder(logRotator(artifactDaysToKeepStr: env.BRANCH_NAME == 'master' ? '14' : '7',
                                  artifactNumToKeepStr: '50',
                                  daysToKeepStr: env.BRANCH_NAME == 'master' ? '30' : '14',
                                  numToKeepStr: env.BRANCH_NAME == 'master' ? '20' : '10')
        ),
        disableConcurrentBuilds()
    ]
)

final def oses = ['linux':'ubuntu', 'windows':'Windows']
//final def mavens = env.BRANCH_NAME == 'master' ? ['3.6.x', '3.2.x'] : ['3.6.x']
final def mavens = ['3.6.x', '3.2.x']
// all non-EOL versions and the first EA
final def jdks = [17, 8, 7]

final def options = ['-e', '-V', '-B', '-nsu', '-P', 'run-its']
final def goals = ['clean', 'install']
final def goalsDepl = ['clean', 'deploy', 'jacoco:report']
final Map stages = [:]

oses.eachWithIndex { osMapping, indexOfOs ->
    mavens.eachWithIndex { maven, indexOfMaven ->
        jdks.eachWithIndex { jdk, indexOfJdk ->
            def os = osMapping.key
            def label = osMapping.value
            final String jdkTestName = jenkinsEnv.jdkFromVersion(os, jdk.toString())
            final String jdkName = jenkinsEnv.jdkFromVersion(os, '8')
            final String mvnName = jenkinsEnv.mvnFromVersion(os, maven)
            final String stageKey = "${os}-jdk${jdk}-maven${maven}"

// Referenses for TLS:
// https://central.sonatype.org/articles/2018/May/04/discontinued-support-for-tlsv11-and-below/?__hstc=31049440.ab2fd229e7f8b6176196d9f78621e1f5.1534324377408.1534324377408.1534324377408.1&__hssc=31049440.1.1534324377409&__hsfp=2729160845
            def mavenOpts = '-Xms64m -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
            mavenOpts += (os == 'linux' ? ' -Xmx1g' : ' -Xmx256m')

            if (label == null || jdkTestName == null || mvnName == null) {
                println "Skipping ${stageKey} as unsupported by Jenkins Environment."
                return
            }

            println "${stageKey}  ==>  Label: ${label}, JDK: ${jdkTestName}, Maven: ${mvnName}."

            stages[stageKey] = {
                node(label) {
                    timestamps {
                        boolean first = indexOfOs == 0 && indexOfMaven == 0 && indexOfJdk == 0
                        def failsafeItPort = 8000 + 100 * indexOfMaven + 10 * indexOfJdk
                        def allOptions = options + ['-Djava.awt.headless=true', "-Dfailsafe-integration-test-port=${failsafeItPort}", "-Dfailsafe-integration-test-stop-port=${1 + failsafeItPort}"]
                        if (jdk == 7) {
                            allOptions += '-Dhttps.protocols=TLSv1.2'
                        }
                        if (!maven.startsWith('3.2') && !maven.startsWith('3.3') && !maven.startsWith('3.5')) {
                            allOptions += '--no-transfer-progress'
                        }
                        ws(dir: "${os == 'windows' ? "${TEMP}\\${BUILD_TAG}" : pwd()}") {
                            buildProcess(stageKey, jdkName, jdkTestName, mvnName, first ? goalsDepl : goals, allOptions, mavenOpts, first)
                        }
                    }
                }
            }
        }
    }
}

timeout(time: 12, unit: 'HOURS') {
    try {
        parallel(stages)
        // JENKINS-34376 seems to make it hard to detect the aborted builds
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
        println "org.jenkinsci.plugins.workflow.steps.FlowInterruptedException: ${e}"
        // this ambiguous condition means a user probably aborted
        if (e.causes.size() == 0) {
            currentBuild.result = 'ABORTED'
        } else {
            currentBuild.result = 'FAILURE'
        }
        throw e
    } catch (hudson.AbortException e) {
        println "hudson.AbortException: ${e}"
        // this ambiguous condition means during a shell step, user probably aborted
        if (e.getMessage().contains('script returned exit code 143')) {
            currentBuild.result = 'ABORTED'
        } else {
            currentBuild.result = 'FAILURE'
        }
        throw e
    } catch (InterruptedException e) {
        println "InterruptedException: ${e}"
        currentBuild.result = 'ABORTED'
        throw e
    } catch (Throwable e) {
        println "Throwable: ${e}"
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        jenkinsNotify()
    }
}

def buildProcess(String stageKey, String jdkName, String jdkTestName, String mvnName, goals, options, mavenOpts, boolean makeReports) {
    cleanWs()
    try {
        def mvnLocalRepoDir
        if (isUnix()) {
            sh 'mkdir -p .m2'
            mvnLocalRepoDir = "${pwd()}/.m2"
        } else {
            bat 'mkdir .m2'
            mvnLocalRepoDir = "${pwd()}\\.m2"
        }

        println "Maven Local Repository = ${mvnLocalRepoDir}."
        assert mvnLocalRepoDir != null : 'Local Maven Repository is undefined.'

        def properties = ["-Djacoco.skip=${!makeReports}", "\"-Dmaven.repo.local=${mvnLocalRepoDir}\""]
        println "Setting JDK for testing ${jdkTestName}"
        def cmd = ['mvn'] + goals + options + properties
        def errorStatus = -99;

        stage("build ${stageKey}") {

             println "NODE_NAME = ${env.NODE_NAME}"

             checkout scm

            if (isUnix()) {
                withEnv(["JAVA_HOME=${tool(jdkName)}",
                         "JAVA_HOME_IT=${tool(jdkTestName)}",
                         "MAVEN_OPTS=${mavenOpts}",
                         "PATH+MAVEN=${tool(mvnName)}/bin:${tool(jdkName)}/bin"
                ]) {
                    sh '$JAVA_HOME_IT/bin/java -version'
                    sh 'echo JAVA_HOME=$JAVA_HOME, JAVA_HOME_IT=$JAVA_HOME_IT, PATH=$PATH'
                    def script = cmd + ['\"-DjdkHome=$JAVA_HOME_IT\"']
                    errorStatus = sh(returnStatus: true, script: script.join(' '))
                }
            } else {
                withEnv(["JAVA_HOME=${tool(jdkName)}",
                         "JAVA_HOME_IT=${tool(jdkTestName)}",
                         "MAVEN_OPTS=${mavenOpts}",
                         "PATH+MAVEN=${tool(mvnName)}\\bin;${tool(jdkName)}\\bin"
                ]) {
                    bat '%JAVA_HOME_IT%\\bin\\java -version'
                    bat 'echo JAVA_HOME=%JAVA_HOME%, JAVA_HOME_IT=%JAVA_HOME_IT%, PATH=%PATH%'
                    def script = cmd + ['\"-DjdkHome=%JAVA_HOME_IT%\"']
                    errorStatus = bat(returnStatus: true, script: script.join(' '))
                }
            }

            if ( errorStatus != 0 )
            {
                currentBuild.result = 'FAILURE'
                unstable(" executing command status= " + errorStatus)
            }
        }
    } finally {
        try {
            if (makeReports) {
                jacoco(changeBuildStatus: false,
                        execPattern: '**/*.exec',
                        sourcePattern: sourcesPatternCsv(),
                        classPattern: classPatternCsv())

                junit(healthScaleFactor: 0.0,
                        allowEmptyResults: true,
                        keepLongStdio: true,
                        testResults: testReportsPatternCsv())

                if (currentBuild.result == 'UNSTABLE') {
                    currentBuild.result = 'FAILURE'
                }
            }

            if (currentBuild.result != null && currentBuild.result != 'SUCCESS') {
                if (fileExists('maven-failsafe-plugin/target/it')) {
                    zip(zipFile: "maven-failsafe-plugin--${stageKey}.zip", dir: 'maven-failsafe-plugin/target/it', archive: true)
                }

                if (fileExists('surefire-its/target')) {
                    zip(zipFile: "surefire-its--${stageKey}.zip", dir: 'surefire-its/target', archive: true)
                }

                archiveArtifacts(artifacts: "*--${stageKey}.zip", allowEmptyArchive: true, onlyIfSuccessful: false)
            }
        } finally {
            // clean up after ourselves to reduce disk space
            cleanWs()
        }
    }
}

@NonCPS
static def sourcesPatternCsv() {
    return '**/maven-failsafe-plugin/src/main/java,' +
            '**/maven-surefire-common/src/main/java,' +
            '**/maven-surefire-plugin/src/main/java,' +
            '**/maven-surefire-report-plugin/src/main/java,' +
            '**/surefire-api/src/main/java,' +
            '**/surefire-booter/src/main/java,' +
            '**/surefire-extensions-api/src/main/java,' +
            '**/surefire-extensions-spi/src/main/java,' +
            '**/surefire-grouper/src/main/java,' +
            '**/surefire-its/src/main/java,' +
            '**/surefire-logger-api/src/main/java,' +
            '**/surefire-providers/**/src/main/java,' +
            '**/surefire-report-parser/src/main/java'
}

@NonCPS
static def classPatternCsv() {
    return '**/maven-failsafe-plugin/target/classes,' +
            '**/maven-surefire-common/target/classes,' +
            '**/maven-surefire-plugin/target/classes,' +
            '**/maven-surefire-report-plugin/target/classes,' +
            '**/surefire-api/target/classes,' +
            '**/surefire-booter/target/classes,' +
            '**/surefire-extensions-api/target/classes,' +
            '**/surefire-extensions-spi/target/classes,' +
            '**/surefire-grouper/target/classes,' +
            '**/surefire-its/target/classes,' +
            '**/surefire-logger-api/target/classes,' +
            '**/surefire-providers/**/target/classes,' +
            '**/surefire-report-parser/target/classes'
}

@NonCPS
static def testReportsPatternCsv() {
    return '**/maven-failsafe-plugin/target/surefire-reports/*.xml,' +
            '**/maven-surefire-common/target/surefire-reports/*.xml,' +
            '**/maven-surefire-plugin/target/surefire-reports/*.xml,' +
            '**/maven-surefire-report-plugin/target/surefire-reports/*.xml,' +
            '**/surefire-api/target/surefire-reports/*.xml,' +
            '**/surefire-booter/target/surefire-reports/*.xml,' +
            '**/surefire-grouper/target/surefire-reports/*.xml,' +
            '**/surefire-its/target/surefire-reports/*.xml,' +
            '**/surefire-logger-api/target/surefire-reports/*.xml,' +
            '**/surefire-providers/**/target/surefire-reports/*.xml,' +
            '**/surefire-report-parser/target/surefire-reports/*.xml,' +
            '**/surefire-its/target/failsafe-reports/*.xml'
}
