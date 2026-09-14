# HAProxy Configuration Notes

## Purpose

HAProxy load-balances incoming traffic across the two Tomcat nodes using round-robin, with active health checks against the Spring Boot actuator health endpoint. Because sessions are stored in Redis (not in-memory on each Tomcat node), plain round-robin is safe here - there is no need for sticky sessions, since either node can serve any request and still see the same session data.

## Setup

    sudo apt install -y haproxy

The config is at /etc/haproxy/haproxy.cfg (a copy is kept in this folder as haproxy.cfg).

## Key Config Details

- Frontend listens on port 80 and forwards all traffic to the booking_backend pool
- Backend balances roundrobin across tomcat-node1 (127.0.0.1:8081) and tomcat-node2 (127.0.0.1:8082)
- Health check: option httpchk GET /booking/actuator/health with http-check expect status 200
- A node is marked down after 3 consecutive failed checks (fall 3) and marked back up after 2 consecutive successful checks (rise 2), checked every 5 seconds (inter 5s)
- A stats page is exposed on port 9000 at /haproxy-stats for visual confirmation of node health

## Verification

    curl http://localhost/booking/actuator/health

Returns {"status":"UP", ...} - confirms the request was routed through HAProxy to a healthy Tomcat node.

## Access URL (per task requirement)

The application is reachable at:

    http://<machine-name>/booking

This works because HAProxy listens on port 80 (the default HTTP port) and forwards /booking/* to whichever Tomcat node is healthy.

## Node Failover Test (to be documented separately with screenshots)

1. Confirm both nodes are UP on the stats page (port 9000)
2. Stop one Tomcat node (e.g. sudo systemctl stop tomcat-node1)
3. HAProxy marks it down after 3 failed health checks (~15 seconds)
4. Traffic continues to flow via the remaining healthy node, with zero downtime
5. Because the session lives in Redis, a user who was logged in stays logged in even though their request now lands on a different node
