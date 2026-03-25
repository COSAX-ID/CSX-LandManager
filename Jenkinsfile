pipeline {
    agent any
    tools {
        jdk 'JDK_21'             // sesuai Global Tool Configuration
        maven 'maven-3.9.14'     // sesuai Global Tool Configuration
    }
    stages {
        stage('Build') {
            steps {
                withEnv([
                    "JAVA_HOME=/usr/lib/jvm/java-21-openjdk",
                    "PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH"
                ]) {
                    sh 'echo JAVA_HOME is $JAVA_HOME'
                    sh 'java -version'
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
    }
}
