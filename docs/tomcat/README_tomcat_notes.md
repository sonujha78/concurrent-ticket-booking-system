# Tomcat Dual-Node Setup Notes

## Setup Summary

Since this task runs on a single Ubuntu machine (not multiple physical/VM nodes), two Tomcat instances were run on the same machine using separate CATALINA_BASE directories and different ports, simulating the two-node architecture the task describes. In a real multi-server deployment, each node would run on its own machine with the same port (8080) and be reached over the network; here, the port offset (8081 / 8082) exists only to avoid a local conflict on one host.

    Node-1: HTTP port 8081, shutdown port 8006, AJP port 8010
    Node-2: HTTP port 8082, shutdown port 8007, AJP port 8011

CATALINA_HOME (shared binaries): /opt/tomcat
CATALINA_BASE (per-node config/webapps/logs): /opt/tomcat-node1 and /opt/tomcat-node2

## Deployment

The WAR file is deployed as booking.war in each node's webapps/ folder, giving the context path /booking as required by the task (http://<name>/booking).

    sudo cp app/target/booking.war /opt/tomcat-node1/webapps/booking.war
    sudo cp app/target/booking.war /opt/tomcat-node2/webapps/booking.war

## Known Issue: Permission Denied on startup.sh (203/EXEC)

Root cause: useradd -m -d /opt/tomcat created /opt/tomcat as the tomcat user's home directory with restrictive permissions, and a corrupted/incomplete tar.gz download (caused by curl not following a redirect) left /opt/tomcat empty, so startup.sh did not exist yet when systemd tried to run it.

Fix: re-downloaded Tomcat with curl -L -f (follow redirects, fail on HTTP errors) from archive.apache.org, re-extracted cleanly, and reset ownership/permissions with chown -R tomcat:tomcat and chmod +x on all .sh files.

## Known Issue: sudo cp with wildcard silently failing

Running "sudo cp /opt/tomcat/conf/* /opt/tomcat-node1/conf/" failed with "No such file or directory" even though the source files existed. Cause: the * wildcard is expanded by the calling user's shell BEFORE sudo takes effect, and the calling user did not have read permission on /opt/tomcat/conf (owned by the tomcat user). 

Fix: wrapped the whole sequence in "sudo bash -c '...'" so the wildcard expansion happens inside the root shell, which has permission to read the source directory.

## Verification

    curl http://localhost:8081/booking/actuator/health
    curl http://localhost:8082/booking/actuator/health

Both return {"status":"UP", ...} confirming both nodes are independently healthy and connected to PostgreSQL, MongoDB, and Redis.
