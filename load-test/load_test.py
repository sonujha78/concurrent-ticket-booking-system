#!/usr/bin/env python3
"""
Load test: fires N simultaneous booking requests for the SAME seat.
Expected result: exactly 1 success, N-1 "already booked" failures,
zero server errors, zero duplicate bookings in the database.
"""

import requests
import threading
import uuid
import time
import sys

BASE_URL = "http://localhost:8080"
SEAT_ID = int(sys.argv[1]) if len(sys.argv) > 1 else 4  # seat to attack
NUM_REQUESTS = int(sys.argv[2]) if len(sys.argv) > 2 else 20

results = []
results_lock = threading.Lock()


def login_and_get_session(user_suffix):
    session = requests.Session()
    session.post(f"{BASE_URL}/login", data={"userId": f"loadtest_user_{user_suffix}"})
    return session


def book_seat(index):
    session = login_and_get_session(index)
    booking_ref = str(uuid.uuid4())
    payload = {"seatId": SEAT_ID, "bookingRef": booking_ref}

    try:
        start = time.time()
        resp = session.post(f"{BASE_URL}/api/book", json=payload, timeout=10)
        elapsed = time.time() - start
        data = resp.json()
        with results_lock:
            results.append({
                "thread": index,
                "http_status": resp.status_code,
                "success": data.get("success"),
                "message": data.get("message"),
                "elapsed_ms": round(elapsed * 1000, 1)
            })
    except Exception as e:
        with results_lock:
            results.append({
                "thread": index,
                "http_status": "ERROR",
                "success": False,
                "message": str(e),
                "elapsed_ms": None
            })


def main():
    print(f"Firing {NUM_REQUESTS} concurrent booking requests for seat_id={SEAT_ID}...\n")

    threads = []
    for i in range(NUM_REQUESTS):
        t = threading.Thread(target=book_seat, args=(i,))
        threads.append(t)

    # Start all threads as close together as possible
    for t in threads:
        t.start()

    for t in threads:
        t.join()

    # Report
    successes = [r for r in results if r["success"] is True]
    failures = [r for r in results if r["success"] is False]
    errors = [r for r in results if r["http_status"] == "ERROR"]

    print("=" * 70)
    print(f"Total requests fired : {NUM_REQUESTS}")
    print(f"Successful bookings  : {len(successes)}")
    print(f"Already-booked fails : {len([r for r in failures if 'already booked' in (r['message'] or '')])}")
    print(f"Server errors        : {len(errors)}")
    print("=" * 70)

    print("\n--- Successful booking(s) ---")
    for r in successes:
        print(r)

    print("\n--- Sample failure ---")
    if failures:
        print(failures[0])

    if errors:
        print("\n--- ERRORS (should be zero) ---")
        for r in errors:
            print(r)

    print("\nExpected: 1 success, {} already-booked failures, 0 errors".format(NUM_REQUESTS - 1))
    if len(successes) == 1 and len(errors) == 0:
        print("RESULT: PASS - race condition correctly prevented duplicate booking")
    else:
        print("RESULT: FAIL - check output above")


if __name__ == "__main__":
    main()
