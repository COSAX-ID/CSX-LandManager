pipeline {
    agent any
    tools {
        jdk 'JDK_21'             // sesuai Global Tool Configuration
        maven 'maven-3.9.14'     // sesuai Global Tool Configuration
    }
    stages {
        stage('Build') {
            steps {
                sh '''
                    echo "JAVA_HOME is $JAVA_HOME"
                    echo "PATH is $PATH"
                    which java
                    java -version
                    mvn clean package -DskipTests
                '''
            }
        }
    }
}
