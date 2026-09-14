pipeline {
    agent any

    // No public endpoint is available on this local machine for a real GitHub
    // webhook, so polling is used as the trigger mechanism instead. In a real
    // deployment with a publicly reachable Jenkins URL, this would be replaced
    // with a GitHub webhook (Manage Jenkins > System > GitHub, push events).
    triggers {
        pollSCM('* * * * *')
    }

    environment {
        NODE1_WEBAPP = '/opt/tomcat-node1/webapps/booking.war'
        NODE2_WEBAPP = '/opt/tomcat-node2/webapps/booking.war'
        NODE1_HEALTH = 'http://localhost:8081/booking/actuator/health'
        NODE2_HEALTH = 'http://localhost:8082/booking/actuator/health'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Unit Tests') {
            steps {
                dir('app') {
                    sh 'mvn test'
                }
            }
        }

        stage('Build WAR') {
            steps {
                dir('app') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Deploy to Tomcat Node 1') {
            steps {
                sh """
                    sudo cp ${WORKSPACE}/app/target/booking.war ${NODE1_WEBAPP}
                    sudo chown tomcat:tomcat ${NODE1_WEBAPP}
                    sudo systemctl restart tomcat-node1
                """
            }
        }

        stage('Health Check Node 1') {
            steps {
                script {
                    def healthy = false
                    for (int i = 0; i < 20; i++) {
                        def code = sh(script: "curl -s -o /dev/null -w '%{http_code}' ${NODE1_HEALTH}", returnStdout: true).trim()
                        if (code == '200') {
                            healthy = true
                            break
                        }
                        sleep(3)
                    }
                    if (!healthy) {
                        error("Node 1 did not become healthy after deployment - aborting rolling deploy")
                    }
                    echo "Node 1 is healthy - proceeding to Node 2"
                }
            }
        }

        stage('Deploy to Tomcat Node 2') {
            steps {
                sh """
                    sudo cp ${WORKSPACE}/app/target/booking.war ${NODE2_WEBAPP}
                    sudo chown tomcat:tomcat ${NODE2_WEBAPP}
                    sudo systemctl restart tomcat-node2
                """
            }
        }

        stage('Health Check Node 2') {
            steps {
                script {
                    def healthy = false
                    for (int i = 0; i < 20; i++) {
                        def code = sh(script: "curl -s -o /dev/null -w '%{http_code}' ${NODE2_HEALTH}", returnStdout: true).trim()
                        if (code == '200') {
                            healthy = true
                            break
                        }
                        sleep(3)
                    }
                    if (!healthy) {
                        error("Node 2 did not become healthy after deployment")
                    }
                    echo "Node 2 is healthy - rolling deployment complete with zero downtime"
                }
            }
        }
    }

    post {
        success {
            sh 'echo "[$(date)] BUILD SUCCESS - build #${BUILD_NUMBER} deployed to both nodes" >> /tmp/jenkins_notifications.log'
            echo "BUILD SUCCESS - notification logged (see docs/jenkins_notes.md for how this maps to email/Slack in production)"
        }
        failure {
            sh 'echo "[$(date)] BUILD FAILURE - build #${BUILD_NUMBER}" >> /tmp/jenkins_notifications.log'
            echo "BUILD FAILURE - notification logged"
        }
    }
}
