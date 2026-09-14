# Jenkins CI/CD Pipeline Notes

## Trigger Mechanism

The task calls for a webhook-triggered pipeline. This Jenkins instance runs on localhost with no public IP or domain, so GitHub cannot reach it to deliver a webhook. As a practical substitute, the pipeline uses pollSCM('* * * * *') - Jenkins checks GitHub for new commits every minute and triggers automatically. In a real deployment with a publicly reachable Jenkins URL, this would be swapped for an actual GitHub webhook (Settings > Webhooks on the repo, pointing at <jenkins-url>/github-webhook/), which is instant rather than polled.

## Pipeline Stages

1. Checkout - pulls the latest commit from the dev branch on GitHub
2. Unit Tests - runs mvn test, including BookingServiceConcurrencyTest, which fires two simultaneous booking attempts for the same seat against a mocked repository layer (a real Postgres instance is not available during the isolated test stage) and asserts exactly one succeeds
3. Build WAR - mvn clean package -DskipTests produces app/target/booking.war
4. Deploy to Node 1 - copies the WAR into Tomcat Node 1's webapps folder and restarts that node
5. Health Check Node 1 - polls /booking/actuator/health up to 20 times (60s) until it returns HTTP 200 before proceeding
6. Deploy to Node 2 - only runs after Node 1 is confirmed healthy, so Node 2 continues serving traffic during Node 1's restart (rolling deployment)
7. Health Check Node 2 - same health-check pattern, confirms zero-downtime deployment
8. Post-build notification - success or failure is logged to /tmp/jenkins_notifications.log. In a production setup this would be an actual email or Slack notification step (Jenkins Email Extension plugin or Slack Notification plugin); those require SMTP/Slack credentials that are outside the scope of this local demo environment

## Permissions

The jenkins system user was granted a scoped NOPASSWD sudo rule (see /etc/sudoers.d/jenkins-deploy) limited to restarting the two Tomcat services and copying files - not full root access.
