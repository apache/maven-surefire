pipeline {
    agent {
        label env.NIX_LABEL
    }
    stages {
        stage('Unix Build') {
            tools {
                maven 'Maven 3.3.9'
                jdk 'JDK 1.8.0_102'
            }
            steps {
                sh 'mvn clean install jacoco:report -B -U -e -fae -V -Prun-its,embedded,jenkins -Dsurefire.useFile=false -Dfailsafe.useFile=false -Dintegration-test-port=8084'
            }
            post {
                success {
                    junit '**/target/surefire-reports/**/*.xml,**/target/failsafe-reports/**/*.xml' 
                }
            }
        }
        stage('Windows Build') {
            agent {
                label env.WIN_LABEL
            }
            tools {
                maven 'Maven 3.3.9 (Windows)'
                jdk 'JDK 1.8_121 (Windows Only)'
            }
            steps {
                bat 'mvn clean install jacoco:report -B -U -e -fae -V -Prun-its,embedded,jenkins -Dsurefire.useFile=false -Dfailsafe.useFile=false -Dintegration-test-port=8084'
            }
            post {
                success {
                    junit '**/target/surefire-reports/**/*.xml,**/target/failsafe-reports/**/*.xml' 
                }
            }
        }
    }
}
