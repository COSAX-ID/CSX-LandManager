pipeline {
    agent any
    tools {
        jdk 'JDK_21'
        maven 'maven-3.9.14'
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
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test || true'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Quality') {
            steps {
                sh 'mvn checkstyle:check -q || true'
            }
        }

        stage('Security Scan - OWASP Dependency Check') {
            steps {
                sh '''
                    echo "Running OWASP Dependency-Check for vulnerability scanning..."
                    mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 -DskipProvidedScope=true || true
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/dependency-check-report.html', fingerprint: true, allowEmptyArchive: true
                    archiveArtifacts artifacts: 'target/dependency-check-report.xml', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Security Scan - SpotBugs') {
            steps {
                sh '''
                    echo "Running SpotBugs for static code analysis and security vulnerabilities..."
                    mvn com.github.spotbugs:spotbugs-maven-plugin:check -q || true
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/spotbugsXml.xml', fingerprint: true, allowEmptyArchive: true
                    archiveArtifacts artifacts: 'target/spotbugs.html', fingerprint: true, allowEmptyArchive: true
                }
            }
        }

        stage('Security Scan - Semgrep') {
            steps {
                sh '''
                    echo "Running Semgrep for security vulnerability detection..."
                    # Install semgrep if not available
                    if ! command -v semgrep &> /dev/null; then
                        echo "Installing semgrep..."
                        pip3 install semgrep || true
                    fi
                    # Run semgrep with security rules
                    semgrep --config auto --error --quiet src/ || true
                '''
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
        unstable {
            echo "Build unstable for ${env.BRANCH_NAME}"
        }
    }
}
