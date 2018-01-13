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
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '0', daysToKeepStr: env.BRANCH_NAME == 'master' ? '30' : '7', numToKeepStr: '5')),
        disableConcurrentBuilds()
    ]
)

def oses = ['windows']  // ['linux', 'windows']
def mavens = env.BRANCH_NAME == 'master' ? ['3.2.x', '3.3.x', '3.5.x'] : ['3.5.x']
def jdks = ['9'] // env.BRANCH_NAME == 'master' ? ['7', '8', '9', '10'] : ['10']

def cmd = ['mvn']
def options = ['-e', '-V', '-nsu']
def optionsITs = ['-e', '-V', '-nsu', '-P', 'run-its']
def goals = ['clean', 'install' , 'jacoco:report']
def goalsITs = ['verify']
def Map stages = [:]

oses.eachWithIndex { os, indexOfOs ->

    mavens.eachWithIndex { maven, indexOfMaven ->

        jdks.eachWithIndex { jdk, indexOfJdk ->

            def String label = jenkinsEnv.labelForOS(os);
            def String jdkTestName = jenkinsEnv.jdkFromVersion(os, jdk)
            def String jdkName = jenkinsEnv.jdkFromVersion(os, '8')
            def String mvnName = jenkinsEnv.mvnFromVersion(os, maven)

            def stageKey = "${os}-jdk${jdk}-maven${maven}"

            if (label == null || jdkTestName == null || mvnName == null) {
                println "Skipping ${stageKey} as unsupported by Jenkins Environment."
                return;
            }

            println "${stageKey}  ==>  Label: ${label}, JDK: ${jdkTestName}, Maven: ${mvnName}."


            stages[stageKey] = {
                if (os == 'windows') {
                    node("${env.WIN_LABEL}") {
                        try {
                            println "Basedir = ${pwd()}."

                            def mvnLocalRepoDir

                            dir('.repository') {
                                mvnLocalRepoDir = "${pwd()}"
                            }

                            println "Maven Local Repository = ${mvnLocalRepoDir}."

                            dir('build') {
                                stage("checkout ${stageKey}") {
                                    checkout scm
                                }

                                def jdkTestHome = resolveToolNameToJavaPath(jdkTestName, mvnName)
                                def properties = ["\"-Djdk.home=${jdkTestHome}\"", "-Djacoco.skip=true"]
                                println("Setting JDK for testing ${properties[0]}")

                                stage("build ${stageKey}") {
                                    withMaven(jdk: jdkName, maven: mvnName,
                                        mavenLocalRepo: mvnLocalRepoDir, mavenOpts: '-Xmx512m',
                                        options: [
                                            artifactsPublisher(disabled: true),
                                    ]) {
                                        def script = cmd + options + goals + properties
                                        bat script.join(' ')
                                    }
                                }

                                def propertiesITs = properties

                                stage("build-failsafe-it ${stageKey}") {
//                                    lock('maven-surefire-its') {
//                                        timeout(time: 15, unit: 'MINUTES') {
//                                            withMaven(jdk: jdkName, maven: mvnName,
//                                                mavenLocalRepo: mvnLocalRepoDir, mavenOpts: '-Xmx512m',
//                                                options: [
//                                                    invokerPublisher(),
//                                                    artifactsPublisher(disabled: true)
//                                            ]) {
//                                                def script = cmd + optionsITs + goalsITs + propertiesITs
//                                                bat script.join(' ')
//                                            }
//                                        }
//                                    }
                                }
                            }
                        } finally {
//                            Wait for INFRA installation of Pipeline Utils, use fileExists()
//                            if (fileExists('build/maven-failsafe-plugin/target/it') {
//                                zip(zipFile: "it--maven-failsafe-plugin--${stageKey}.zip", dir: 'build/maven-failsafe-plugin/target/it', archive: true)
//                            }
//
//                            if (fileExists('build/surefire-integration-tests/target') {
//                                zip(zipFile: "it--surefire-integration-tests--${stageKey}.zip", dir: 'build/surefire-integration-tests/target', archive: true)
//                            }

//                            archiveArtifacts(artifacts: 'build/surefire-integration-tests/target/**/log.txt', allowEmptyArchive: true, fingerprint: true, onlyIfSuccessful: false)

                            stage("cleanup ${stageKey}") {
                                // clean up after ourselves to reduce disk space
                                cleanWs()
                            }
                        }
                    }
                } else {
                    node("${env.NIX_LABEL}") {
                        try {
                            println "Basedir = ${pwd()}."

                            def mvnLocalRepoDir

                            dir('.repository') {
                                mvnLocalRepoDir = "${pwd()}"
                            }

                            println "Maven Local Repository = ${mvnLocalRepoDir}."

                            dir('build') {
                                stage("checkout ${stageKey}") {
                                    checkout scm
                                }

                                def jdkTestHome = resolveToolNameToJavaPath(jdkTestName, mvnName)
                                //https://github.com/jacoco/jacoco/issues/629
                                def skipPlugins = jdk != '9'
                                def properties = ["\"-Djdk.home=${jdkTestHome}\"", "-Djacoco.skip=${skipPlugins}"]
                                println("Setting JDK for testing ${properties[0]}")

                                stage("build ${stageKey}") {
                                    withMaven(jdk: jdkName, maven: mvnName,
                                        mavenLocalRepo: mvnLocalRepoDir, mavenOpts: '-Xmx1g',
                                        options: [
                                            findbugsPublisher(disabled: skipPlugins),
                                            openTasksPublisher(disabled: skipPlugins, ignoreCase: true,
                                                               pattern: sourcesPatternCsv(),
                                                               low: tasksViolationLow(),
                                                               normal: tasksViolationNormal(),
                                                               high: tasksViolationHigh()),
                                            artifactsPublisher(disabled: true)
                                    ]) {
                                        def script = cmd + options + goals + properties
                                        sh script.join(' ')
                                    }
                                }

                                def propertiesITs = [properties[0], '-Djacoco.skip=true']

                                stage("build-failsafe-it ${stageKey}") {
//                                    lock('maven-surefire-its') {
//                                        timeout(time: 15, unit: 'MINUTES') {
//                                            withMaven(jdk: jdkName, maven: mvnName,
//                                                mavenLocalRepo: mvnLocalRepoDir, mavenOpts: '-Xmx1g',
//                                                options: [
//                                                    invokerPublisher(),
//                                                    openTasksPublisher(disabled: true, ignoreCase: true,
//                                                                       pattern: sourcesPatternCsv(),
//                                                                       low: tasksViolationLow(),
//                                                                       normal: tasksViolationNormal(),
//                                                                       high: tasksViolationHigh()),
//                                                    artifactsPublisher(disabled: true)
//                                            ]) {
//                                                def script = cmd + optionsITs + goalsITs + propertiesITs
//                                                sh script.join(' ')
//                                            }
//                                        }
//                                    }
                                }
                            }
                        } finally {
                            if (indexOfMaven == mavens.size() - 1 && jdk == '9') {
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

//                            Wait for INFRA installation of Pipeline Utils, use fileExists()
//                            if (fileExists('build/maven-failsafe-plugin/target/it') {
//                                zip(zipFile: "it--maven-failsafe-plugin--${stageKey}.zip", dir: 'build/maven-failsafe-plugin/target/it', archive: true)
//                            }
//
//                            if (fileExists('build/surefire-integration-tests/target') {
//                                zip(zipFile: "it--surefire-integration-tests--${stageKey}.zip", dir: 'build/surefire-integration-tests/target', archive: true)
//                            }
//
//                            sh 'tar czvf it1.tgz build/maven-failsafe-plugin/target/it'
//                            sh 'tar czvf it2.tgz build/surefire-integration-tests/target'
//                            archiveArtifacts(artifacts: '**/*.tgz', allowEmptyArchive: true, fingerprint: true, onlyIfSuccessful: false)
//                            archiveArtifacts(artifacts: '*.tgz', allowEmptyArchive: true, fingerprint: true, onlyIfSuccessful: false)

                            stage("cleanup ${stageKey}") {
                                // clean up after ourselves to reduce disk space
                                cleanWs()
                            }
                        }
                    }
                }
            }
        }
    }
}

timeout(time: 18, unit: 'HOURS') {
    try {
        parallel(stages)
        // JENKINS-34376 seems to make it hard to detect the aborted builds
    } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
        // this ambiguous condition means a user probably aborted
        if (e.causes.size() == 0) {
            currentBuild.result = "ABORTED"
        } else {
            currentBuild.result = "FAILURE"
        }
        throw e
    } catch (hudson.AbortException e) {
        // this ambiguous condition means during a shell step, user probably aborted
        if (e.getMessage().contains('script returned exit code 143')) {
            currentBuild.result = "ABORTED"
        } else {
            currentBuild.result = "FAILURE"
        }
        throw e
    } catch (InterruptedException e) {
        currentBuild.result = "ABORTED"
        throw e
    } catch (Throwable e) {
        currentBuild.result = "FAILURE"
        throw e
    } finally {
        stage("notifications") {
            //jenkinsNotify()
        }
    }
}

/**
 * It is used instead of tool(${jdkTestName}).
 */
def resolveToolNameToJavaPath(jdkToolName, mvnName) {
    def javaHome = null
    try {
        withMaven(jdk: jdkToolName, maven: mvnName) {
            javaHome = isUnix() ? sh(script: 'echo -en $JAVA_HOME', returnStdout: true) : bat(script: '@echo %JAVA_HOME%', returnStdout: true)
        }

        if (javaHome != null) {
            javaHome = javaHome.trim()
            def exec = javaHome + (isUnix() ? '/bin/java' : '\\bin\\java.exe')
            if (!fileExists(exec)) {
                println "The ${exec} does not exist in jdkToolName=${jdkToolName}."
                javaHome = null
            }
        }
    } catch(e) {
        println "Caught an exception while resolving 'jdkToolName' ${jdkToolName} via 'mvnName' ${mvnName}: ${e}"
        javaHome = null;
    }
    assert javaHome != null : "Could not resolve ${jdkToolName} to JAVA_HOME."
    return javaHome
}

@NonCPS
def sourcesPatternCsv() {
    return '**/maven-failsafe-plugin/src/main/java,' +
            '**/maven-surefire-common/src/main/java,' +
            '**/maven-surefire-plugin/src/main/java,' +
            '**/maven-surefire-report-plugin/src/main/java,' +
            '**/surefire-api/src/main/java,' +
            '**/surefire-booter/src/main/java,' +
            '**/surefire-grouper/src/main/java,' +
            '**/surefire-integration-tests/src/main/java,' +
            '**/surefire-logger-api/src/main/java,' +
            '**/surefire-providers/**/src/main/java,' +
            '**/surefire-report-parser/src/main/java'
}

@NonCPS
def classPatternCsv() {
    return '**/maven-failsafe-plugin/target/classes,' +
            '**/maven-surefire-common/target/classes,' +
            '**/maven-surefire-plugin/target/classes,' +
            '**/maven-surefire-report-plugin/target/classes,' +
            '**/surefire-api/target/classes,' +
            '**/surefire-booter/target/classes,' +
            '**/surefire-grouper/target/classes,' +
            '**/surefire-integration-tests/target/classes,' +
            '**/surefire-logger-api/target/classes,' +
            '**/surefire-providers/**/target/classes,' +
            '**/surefire-report-parser/target/classes'
}

@NonCPS
def tasksViolationLow() {
    return '@SuppressWarnings'
}

@NonCPS
def tasksViolationNormal() {
    return 'TODO,FIXME,@deprecated'
}

@NonCPS
def tasksViolationHigh() {
    return 'finalize(),Locale.setDefault,TimeZone.setDefault,\
System.out,System.err,System.setOut,System.setErr,System.setIn,System.exit,System.gc,System.runFinalization,System.load'
}

@NonCPS
def testReportsPatternCsv() {
    return '**/maven-failsafe-plugin/target/surefire-reports/*.xml,' +
            '**/maven-surefire-common/target/surefire-reports/*.xml,' +
            '**/maven-surefire-plugin/target/surefire-reports/*.xml,' +
            '**/maven-surefire-report-plugin/target/surefire-reports/*.xml,' +
            '**/surefire-api/target/surefire-reports/*.xml,' +
            '**/surefire-booter/target/surefire-reports/*.xml,' +
            '**/surefire-grouper/target/surefire-reports/*.xml,' +
            '**/surefire-integration-tests/target/surefire-reports/*.xml,' +
            '**/surefire-logger-api/target/surefire-reports/*.xml,' +
            '**/surefire-providers/**/target/surefire-reports/*.xml,' +
            '**/surefire-report-parser/target/surefire-reports/*.xml,' +
            '**/surefire-integration-tests/target/failsafe-reports/*.xml'
}
