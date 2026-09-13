# Concurrent Ticket Booking System

Race-condition-safe seat booking system demonstrating:
- PostgreSQL row-level locking (SELECT FOR UPDATE)
- Idempotent booking via UUID booking_ref
- Redis-backed distributed sessions across Tomcat nodes
- HAProxy load balancing with active health checks
- MongoDB logging of every booking attempt
- Jenkins CI/CD with rolling zero-downtime deployment

## Status
🚧 Work in progress — building phase by phase.
