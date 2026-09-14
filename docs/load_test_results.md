# Load Test Results - Proof of Race-Condition Safety

## Test Setup

- Script: load-test/load_test.py
- Target: 20 simultaneous booking requests for the SAME seat (seat_id=5)
- Each request uses its own login session and a unique booking_ref (UUID)

## Command Used

    python3 load_test.py 5 20

## Result Output

    Firing 20 concurrent booking requests for seat_id=5...

    ======================================================================
    Total requests fired : 20
    Successful bookings  : 1
    Already-booked fails : 19
    Server errors        : 0
    ======================================================================

    --- Successful booking(s) ---
    thread 10, http_status 200, success True, message "Booking confirmed", elapsed_ms 421.5

    --- Sample failure ---
    thread 3, http_status 200, success False, message "Seat already booked, please choose another seat", elapsed_ms 400.2

    RESULT: PASS - race condition correctly prevented duplicate booking

## Database Verification

### PostgreSQL - bookings table (proof of exactly 1 row)

Query used:

    SELECT * FROM bookings WHERE seat_id = 5;

Result:

    booking_id | seat_id |    user_id       |             booking_ref              |   status   |         created_at
    -----------+---------+-------------------+---------------------------------------+-------------+----------------------------
             3 |       5 | loadtest_user_10  | 9d0e7f91-e162-482e-9e36-3ec716516e5c  | confirmed   | 2026-09-13 19:07:28.468413
    (1 row)

Exactly 1 row, as required. No duplicate bookings.

### MongoDB - booking_attempts collection (proof all 20 attempts were logged)

Query used:

    db.booking_attempts.countDocuments({seatId: 5})

Result:

    20

All 20 concurrent attempts were logged - 1 success and 19 already_booked failures.

## Conclusion

The row-level pessimistic lock (SELECT ... FOR UPDATE) in BookingService.bookSeat() correctly serializes concurrent requests for the same seat. Under 20 simultaneous requests:

- Exactly 1 request succeeded
- 19 requests received a clean "already booked" response, not a crash or generic error
- Zero duplicate bookings in the database
- Zero server errors
