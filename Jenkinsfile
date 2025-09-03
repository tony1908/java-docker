pipeline {
    agent any
    
    triggers {
        pollSCM('H/2 * * * *')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'SubversionSCM',
                    locations: [[
                        remote: 'YOUR_SVN_URL_HERE',
                        credentialsId: 'YOUR_SVN_CREDENTIALS_ID'
                    ]],
                    workspaceUpdater: [$class: 'UpdateUpdater']
                ])
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    def image = docker.build("javadb:${env.BUILD_NUMBER}")
                }
            }
        }
        
        stage('Tag Image') {
            steps {
                script {
                    docker.image("javadb:${env.BUILD_NUMBER}").tag('latest')
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Build completed successfully!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}