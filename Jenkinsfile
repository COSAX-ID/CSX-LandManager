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
                    # Detect Java installation
                    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
                        echo "Using JAVA_HOME: $JAVA_HOME"
                        export JAVA="$JAVA_HOME/bin/java"
                    elif [ -x "/usr/bin/java" ]; then
                        echo "Using system Java at /usr/bin/java"
                        export JAVA_HOME=$(readlink -f /usr/bin/java | sed 's:/bin/java::')
                        export JAVA="/usr/bin/java"
                    else
                        echo "Using java from PATH"
                        export JAVA=$(which java)
                        export JAVA_HOME=$(readlink -f $(which java) | sed 's:/bin/java::')
                    fi
                    echo "JAVA_HOME: $JAVA_HOME"
                    echo "JAVA: $JAVA"
                    $JAVA -version
                    mvn clean package -DskipTests
                '''
            }
        }
    }
}
