# Concurrent Ticket Booking System

Race-condition-safe movie/event ticket booking system demonstrating:

- PostgreSQL row-level locking (SELECT ... FOR UPDATE)
- Idempotent booking via UUID booking_ref
- Redis-backed distributed sessions shared across two Tomcat nodes
- HAProxy load balancing with active health checks
- MongoDB logging of every booking attempt (success and failure)
- Jenkins CI/CD pipeline with rolling, zero-downtime deployment

Note: GitHub was used in place of GitLab for source control. Both provide equivalent DVCS and Merge Request / Pull Request workflows, so all task requirements around branching and code review are still met.

## Architecture

    Browser --> HAProxy (port 80) --> Tomcat Node 1 (8081) --+
                                   --> Tomcat Node 2 (8082) --+
                                                               |
                        PostgreSQL (bookingdb) <--------------+
                        MongoDB (bookingLogs)  <---------------+
                        Redis (sessions)       <---------------+

Both Tomcat nodes run on the same machine (CATALINA_BASE per node, different ports) to simulate a two-node deployment, since only one physical machine was available for this task. In a real deployment each node would run on its own host on the same port 8080, reached over the network.

## Repository Structure

    app/        Spring Boot application source (Java, Maven, WAR packaging)
    db/postgres/  PostgreSQL schema and seed data
    db/mongo/     MongoDB init script for the booking_attempts collection
    redis/        Redis setup notes
    haproxy/      HAProxy config
    jenkins/      Jenkins pipeline notes
    load-test/    Concurrent load test script (Python)
    docs/         Proof documents: load test results, session persistence proof, Tomcat notes
    Jenkinsfile   CI/CD pipeline definition

## Proof Points (see docs/ for full detail)

- docs/load_test_results.md - 20 concurrent requests for the same seat: exactly 1 success, 19 clean "already booked" failures, 0 errors, 0 duplicate rows in the database
- docs/session_persistence_proof.md - a login session survives a Tomcat node being killed mid-session, because the session lives in Redis, not on the node
- docs/tomcat/README_tomcat_notes.md - dual Tomcat node setup and troubleshooting
- jenkins/README_jenkins_notes.md - CI/CD pipeline stages and issues fixed along the way

## Branching

Development happens on the dev branch. Completed work is merged into main via Pull Request rather than pushed directly, per the task's Git workflow requirement.

## Access URL

    http://<machine-name>/booking

HAProxy listens on port 80 and forwards to whichever Tomcat node is currently healthy.
