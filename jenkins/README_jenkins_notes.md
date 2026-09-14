# Jenkins CI/CD Pipeline Notes

## Trigger Mechanism

The task calls for a webhook-triggered pipeline. This Jenkins instance runs on localhost with no public IP or domain, so GitHub cannot reach it to deliver a webhook. As a practical substitute, the pipeline uses pollSCM('* * * * *') - Jenkins checks GitHub for new commits every minute and triggers automatically. In a real deployment with a publicly reachable Jenkins URL, this would be swapped for an actual GitHub webhook (Settings > Webhooks on the repo, pointing at <jenkins-url>/github-webhook/), which is instant rather than polled.

## Pipeline Stages

1. Checkout - pulls the latest commit from the dev branch on GitHub
2. Unit Tests - runs mvn test, including BookingServiceConcurrencyTest, which fires two simultaneous booking attempts for the same seat against a mocked repository layer and asserts exactly one succeeds
3. Build WAR - mvn clean package -DskipTests produces app/target/booking.war
4. Deploy to Node 1 - copies the WAR into Tomcat Node 1's webapps folder and restarts that node
5. Health Check Node 1 - polls /booking/actuator/health, retrying up to 30 times (90s) until HTTP 200
6. Deploy to Node 2 - only runs after Node 1 is confirmed healthy, so Node 2 continues serving traffic during Node 1's restart (rolling deployment)
7. Health Check Node 2 - same retry pattern, confirms zero-downtime deployment
8. Post-build notification - success or failure is logged to /tmp/jenkins_notifications.log. In a production setup this would be an actual email or Slack notification step (Jenkins Email Extension or Slack Notification plugin), which require SMTP/Slack credentials outside the scope of this local demo environment

## Issues Encountered and Fixed

### 1. Jenkins apt repository signing key rotated
Jenkins rotated its Debian package signing key; the old jenkins.io-2023.key URL returned a key that no longer matched the repository's actual signature (NO_PUBKEY error). Fixed by using the current jenkins.io-2026.key from Jenkins' official install instructions.

### 2. sudoers path mismatch (absolute vs relative)
The sudoers rule allowed an absolute path (/var/lib/jenkins/workspace/.../booking.war), but the Jenkinsfile originally used a relative path (app/target/booking.war). sudo requires an exact string match, so the command was rejected ("I'm sorry jenkins, I'm afraid I can't do that") even though the rule looked correct. Fixed by using ${WORKSPACE}/app/target/booking.war in the Jenkinsfile so the path always matches what's granted in /etc/sudoers.d/jenkins-deploy.

### 3. Health-check curl aborting the pipeline on first failure
The first health-check attempt right after sudo systemctl restart hit "connection refused" (curl exit code 7) while Tomcat was still starting up. Jenkins' sh step treats any non-zero exit as a failure by default, so the retry loop never got a second attempt. Fixed by appending || true to the curl command and using returnStdout instead of relying on the exit code, so a failed connection just produces an empty/000 status that the loop can retry against.

## Permissions

The jenkins system user was granted a scoped NOPASSWD sudo rule (see /etc/sudoers.d/jenkins-deploy), limited to exact cp/chown/systemctl commands for the two Tomcat webapp paths - not full root access.

## Verified Result

Build #4 completed with status SUCCESS: unit tests passed, WAR built, deployed to Node 1 with a healthy check, then deployed to Node 2 with a healthy check - a true rolling, zero-downtime deployment.
