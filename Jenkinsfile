pipeline {

    agent any

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

        stage('Deploy') {
            steps {
                // Docker Hub deployment dar marhale baad ezafe mishavad
                echo 'Deploying...'
            }
        }
    }
}