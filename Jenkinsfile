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
                    $JAVA_HOME/bin/java -version
                    $JAVA_HOME/bin/java -version 2>&1 | head -1
                    mvn clean package -DskipTests
                '''
            }
        }
    }
}
