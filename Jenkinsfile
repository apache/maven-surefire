pipeline {
    agent none
    stages {
        stage('Unix Build') {
            agent {
                label env.NIX_LABEL
            }
            tools {
                maven 'Maven 3.5.0'
                jdk 'JDK 1.8.0_144'
            }
            steps {
                sh "mvn clean install jacoco:report -B -U -e -fae -V -P run-its,jenkins -Dsurefire.useFile=false -Dfailsafe.useFile=false -Dintegration-test-port=8084   \\\"-Djdk.home=${tool('JDK 9 b181')}\\\""
                jacoco changeBuildStatus: false, execPattern: '**/*.exec'
            }
            post {
                always {
                    junit healthScaleFactor: 0.0, allowEmptyResults: true, keepLongStdio: true, testResults: '**/surefire-integration-tests/target/failsafe-reports/**/*.xml,**/surefire-integration-tests/target/surefire-reports/**/*.xml,**/maven-*/target/surefire-reports/**/*.xml,**/surefire-*/target/surefire-reports/**/*.xml,**/common-*/target/surefire-reports/**/*.xml'
                }
            }
        }
        stage('Windows Build') {
            agent {
                label env.WIN_LABEL
            }
            tools {
                maven 'Maven 3.5.0 (Windows)'
                jdk 'JDK 1.8_121 (Windows Only)'
            }
            steps {
                bat "mvn clean install jacoco:report -B -U -e -fae -V -P run-its,jenkins -Dsurefire.useFile=false -Dfailsafe.useFile=false -Dintegration-test-port=8084   \\\"-Djdk.home=${tool('JDK 9 b181')}\\\""
                jacoco changeBuildStatus: false, execPattern: '**/*.exec'
            }
            post {
                always {
                    junit healthScaleFactor: 0.0, allowEmptyResults: true, keepLongStdio: true, testResults: '**/surefire-integration-tests/target/failsafe-reports/**/*.xml,**/surefire-integration-tests/target/surefire-reports/**/*.xml,**/maven-*/target/surefire-reports/**/*.xml,**/surefire-*/target/surefire-reports/**/*.xml,**/common-*/target/surefire-reports/**/*.xml'
                }
            }
        }
    }
    options {
        buildDiscarder(logRotator(numToKeepStr:'3'))
        timeout(time: 10, unit: 'HOURS')
    }
}
