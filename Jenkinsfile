pipeline {

    agent any
    tools {
    maven 'Maven-3.9.15'
}

    stages {

        stage('Build') {
            steps {
                bat 'mvn clean install'
            }
        }

        stage('Test') {
            steps {
                // Ejraye test ha
                bat 'mvn test'
            }
        }

        stage('Code Coverage') {
            steps {
                // Sakhtane JaCoCo report
                bat 'mvn jacoco:report'
            }
        }

        stage('Publish Test Results') {
            steps {
                // Namayeshe natije test ha dar Jenkins
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('Publish Coverage Results') {
            steps {
                // Namayeshe coverage dar Jenkins
                jacoco execPattern: '**/target/jacoco.exec',
                       classPattern: '**/target/classes',
                       sourcePattern: '**/src/main/java',
                       exclusionPattern: '**/target/**'
            }
        }

       stage('Docker Build') {
    steps {
        // Sakhtane Docker image
        bat 'docker build -t minooyh/tempconverter:latest .'
    }
}
stage('Docker Push') {
    steps {
        // Login be Docker Hub va push kardane image
        withCredentials([usernamePassword(
            credentialsId: 'dockerhub-credentials',
            usernameVariable: 'DOCKER_USERNAME',
            passwordVariable: 'DOCKER_TOKEN'
        )]) {
            bat 'echo %DOCKER_TOKEN% | docker login -u %DOCKER_USERNAME% --password-stdin'
            bat 'docker push minooyh/tempconverter:latest'
        }
    }
}
    }
}