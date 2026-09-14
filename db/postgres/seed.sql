INSERT INTO events (name, venue, event_time, total_seats)
VALUES ('Race Condition Test Show', 'Main Hall', now() + interval '2 days', 20);

INSERT INTO seats (event_id, seat_number, status)
SELECT 1, 'A' || generate_series(1, 20), 'available';
