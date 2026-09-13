CREATE TABLE IF NOT EXISTS events (
    event_id     SERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    venue        VARCHAR(200) NOT NULL,
    event_time   TIMESTAMP NOT NULL,
    total_seats  INT NOT NULL CHECK (total_seats > 0)
);

CREATE TABLE IF NOT EXISTS seats (
    seat_id      SERIAL PRIMARY KEY,
    event_id     INT NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    seat_number  VARCHAR(10) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'available'
                 CHECK (status IN ('available', 'locked', 'booked')),
    UNIQUE (event_id, seat_number)
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id   SERIAL PRIMARY KEY,
    seat_id      INT NOT NULL REFERENCES seats(seat_id),
    user_id      VARCHAR(100) NOT NULL,
    booking_ref  UUID NOT NULL UNIQUE,
    status       VARCHAR(20) NOT NULL DEFAULT 'confirmed'
                 CHECK (status IN ('confirmed', 'failed', 'cancelled')),
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_seats_event_status ON seats(event_id, status);
CREATE INDEX IF NOT EXISTS idx_bookings_seat_id ON bookings(seat_id);
