pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'trial2ot5pk.jfrog.io/clase-docker/java-mariadb-app'
        JFROG_CREDENTIALS = 'jfrog'
        PATH = "/usr/local/bin:${env.PATH}"
    }

    triggers {
        pollSCM('* * * * *')
    }

    stages {
        stage('Test & Build') {
            steps {
                script {
                    image = docker.build(DOCKER_IMAGE, '.')
                }
                /*sh 'mvn clean test'
                sh 'mvn clean package'*/bu
                
            }
        }

        stage('Push') {
            steps {
                script {
                    def img = docker.build(DOCKER_IMAGE, '.')
                    docker.withRegistry('https://trial2ot5pk.jfrog.io', JFROG_CREDENTIALS) {
                        img.push()
                    }
                }
                /*sh 'mvn clean deploy'*/
            }
        }

        stage('Deploy') {
            steps {
                sh 'kubectl apply -f k8s/namespace.yaml'
                sh 'kubectl apply -f k8s/mariadb-pvc.yaml'
                sh 'kubectl apply -f k8s/mariadb-deployment.yaml'
                sh 'kubectl apply -f k8s/mariadb-service.yaml'
                sh 'kubectl apply -f k8s/kafka-deployment.yaml'
                sh 'kubectl apply -f k8s/app-configmap.yaml'
                sh 'kubectl apply -f k8s/app-secret.yaml'
                sh 'kubectl apply -f k8s/app-deployment.yaml'
            }
        }
    }

    post {
        failure {
            mail to: 'toony1908@gmail.com', subject: 'Build failed', body: 'Build failed'
        }

        success {
            mail to: 'toony1908@gmail.com', subject: 'Build success', body: 'Build success'
        }
    }

}