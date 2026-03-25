pipeline {
    agent any

    tools {
        maven 'Maven 3.9.14'
        jdk 'JDK 21'
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=./.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME}"
            }
        }

        stage('Build') {
            steps {
                echo 'Running Maven Build...'
                sh 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                }
            }
        }

        stage('Test') {
            steps {
                echo 'Running Maven Tests...'
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Quality') {
            steps {
                echo 'Running Code Analysis...'
                sh 'mvn checkstyle:check -q || true'
            }
        }

        stage('Deploy Snapshot') {
            when {
                branch 'dev'
            }
            steps {
                echo 'Deploying SNAPSHOT version...'
                sh 'mvn deploy -DskipTests -DaltDeploymentRepository=snapshot-repo::default::file://${WORKSPACE}/target/snapshot-repo'
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Build succeeded for ${env.BRANCH_NAME}"
        }
        failure {
            echo "Build failed for ${env.BRANCH_NAME}"
        }
    }
}
