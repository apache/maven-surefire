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
                sh 'mvn -v'
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
                bat 'mvn -v'
            }
        }
    }
}
