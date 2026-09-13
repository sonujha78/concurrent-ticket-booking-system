# Redis Configuration Notes

## Purpose

Redis is used as a distributed session store for the Spring Boot application. Since two Tomcat nodes sit behind HAProxy, sessions cannot live in each node's local memory — if HAProxy routes a user's next request to a different node, an in-memory session would be lost and the user would appear logged out.

Spring Session (spring-session-data-redis) is used to persist HttpSession data in Redis instead of Tomcat's in-memory session store. Both Tomcat nodes point to the same Redis instance, so session state is shared regardless of which node serves a given request.

## Install

```bash
sudo apt update
sudo apt install -y redis-server
sudo systemctl enable --now redis-server
```

## Known Issue: Port Conflict with Docker

A pre-existing Docker container (`pm_redis`) was already bound to port 6379, causing the native `redis-server` service to fail with:
Warning: Could not create server TCP listening socket 127.0.0.1:6379: bind: Address already in use

### Fix Applied

```bash
docker stop pm_redis
docker rm pm_redis
docker volume rm docker_redis-data
sudo systemctl restart redis-server
```

## Verification

```bash
redis-cli ping
```

Expected: `PONG`

## Connection Details (for Spring Boot application.properties)
spring.redis.host=localhost
spring.redis.port=6379
spring.session.store-type=redis

## Proof of Session Persistence (documented separately with screenshots)

1. Log in through the app (session created, stored in Redis)
2. Kill the Tomcat node currently serving that session
3. Refresh — user remains logged in because the session lives in Redis, not on the node
