pipeline {
    agent any

    tools {
        maven 'M3.3.9'
    }

    triggers {
        pollSCM('H H/4 * * *')
    }

    stages {
        stage('checkout') {
            steps {
                checkout scm
            }
        }

        stage('build') {
            steps {
                withMaven() {
                    sh 'mvn -e -DskipTests -DincludeSrcJavadocs clean source:jar install'
                }
            }
        }
    }

    post {
        success {
            sh 'curl --data "build=true" -X POST https://registry.hub.docker.com/u/dmadk/ais-view/trigger/433ec984-4492-11e4-8fb3-9a7045258090/'
        }
    }
}
