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
        buildDiscarder(logRotator(artifactNumToKeepStr: env.BRANCH_NAME == 'master' ? '15' : '5',
                                  daysToKeepStr: env.BRANCH_NAME == 'master' ? '30' : '14',
                                  numToKeepStr: env.BRANCH_NAME == 'master' ? '20' : '10')
        )//,
        //disableConcurrentBuilds()
    ]
)
// final def oses = ['linux':'ubuntu && maven', 'windows':'windows-he']
final def oses = ['linux':'ubuntu']
final def mavens = env.BRANCH_NAME == 'master' ? ['3.x.x', '3.6.3'] : ['3.x.x']
// all non-EOL versions and the first EA
final def jdks = [21, 17, 11, 8]

final def options = ['-e', '-V', '-B', '-nsu', '-P', 'run-its']
final def goals = ['clean', 'install']
final def goalsDepl = ['clean', 'deploy']
final Map stages = [:]

oses.eachWithIndex { osMapping, indexOfOs ->
    mavens.eachWithIndex { maven, indexOfMaven ->
        jdks.eachWithIndex { jdk, indexOfJdk ->
            def os = osMapping.key
            def label = osMapping.value
            final String jdkName = jenkinsEnv.jdkFromVersion(os, jdk.toString())
            final String mvnName = jenkinsEnv.mvnFromVersion(os, maven)
            final String stageKey = "${os}-jdk${jdk}-maven${maven}"

            def mavenOpts = '-Xms64m -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
            mavenOpts += (os == 'linux' ? ' -Xmx1g' : ' -Xmx256m')

            if (label == null || jdkName == null || mvnName == null) {
                println "Skipping ${stageKey} as unsupported by Jenkins Environment."
                return
            }

            println "${stageKey}  ==>  Label: ${label}, JDK: ${jdkName}, Maven: ${mvnName}."

            stages[stageKey] = {
                node(label) {
                    timestamps {
                        boolean first = indexOfOs == 0 && indexOfMaven == 0 && indexOfJdk == 0
                        def failsafeItPort = 8000 + 100 * indexOfMaven + 10 * indexOfJdk
                        def allOptions = options + ['-Djava.awt.headless=true', "-Dfailsafe-integration-test-port=${failsafeItPort}", "-Dfailsafe-integration-test-stop-port=${1 + failsafeItPort}"]

                        if (!maven.startsWith('3.2') && !maven.startsWith('3.3') && !maven.startsWith('3.5')) {
                            allOptions += '--no-transfer-progress'
                        }
                        ws(dir: "${os == 'windows' ? "${TEMP}\\${BUILD_TAG}" : pwd()}") {
                            buildProcess(stageKey, jdkName, mvnName,
                                first  && env.BRANCH_NAME == 'master' ? goalsDepl : goals,
                                allOptions, mavenOpts, first)
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
        if (env.BRANCH_NAME == 'master') {
            jenkinsNotify()            
        }    
    }
}

def buildProcess(String stageKey, String jdkName, String mvnName, goals, options, mavenOpts, boolean makeReports) {
    cleanWs()
    def errorStatus = -99
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

        def properties = ["-Papache.ci -Djacoco.skip=${!makeReports}", "\"-Dmaven.repo.local=${mvnLocalRepoDir}\""]
        def cmd = ['mvn'] + goals + options + properties

        stage("build ${stageKey}") {

             println "NODE_NAME = ${env.NODE_NAME}"

             checkout scm

            if (isUnix()) {
                withEnv(["JAVA_HOME=${tool(jdkName)}",
                         "MAVEN_OPTS=${mavenOpts}",
                         "PATH+MAVEN=${tool(mvnName)}/bin:${tool(jdkName)}/bin"
                ]) {
                    sh 'echo JAVA_HOME=$JAVA_HOME, PATH=$PATH'
                    sh '$JAVA_HOME/bin/java -version'
                    errorStatus = sh(returnStatus: true, script: cmd.join(' '))
                }
            } else {
                withEnv(["JAVA_HOME=${tool(jdkName)}",
                         "MAVEN_OPTS=${mavenOpts}",
                         "PATH+MAVEN=${tool(mvnName)}\\bin;${tool(jdkName)}\\bin"
                ]) {
                    bat 'echo JAVA_HOME=%JAVA_HOME%, PATH=%PATH%'
                    bat '%JAVA_HOME%\\bin\\java -version'
                    errorStatus = bat(returnStatus: true, script: cmd.join(' '))
                }
            }

            if ( errorStatus != 0 )
            {
                currentBuild.result = 'FAILURE'
                unstable(" executing command status= " + errorStatus)
            }
        }

    } catch (Throwable e) {
        println "Throwable: ${e}"
        throw e
    } finally {
        try {
            if (makeReports) {
                jacoco(changeBuildStatus: false,
                        execPattern: '**/target/jacoco*.exec',
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

            if (errorStatus != 0) {
                println "errorStatus=${errorStatus} we are going to archive.."
                zip(zipFile: "maven-failsafe-plugin--${stageKey}.zip", dir: 'maven-failsafe-plugin/target/it', archive: true)
                zip(zipFile: "surefire-its--${stageKey}.zip", dir: 'surefire-its/target', archive: true)
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
