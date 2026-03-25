pipeline {
    agent any

    tools {
        jdk 'JDK_21'
        maven 'maven-3.9.14'
    }

    stages {
        stage('Build') {
            steps {
                echo "JAVA_HOME is $JAVA_HOME"
                sh 'java -version'
                sh 'mvn clean package -DskipTests'
            }
        }
    }
}
