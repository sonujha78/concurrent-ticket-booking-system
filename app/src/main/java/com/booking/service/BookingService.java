package com.booking.service;

import com.booking.dto.BookingRequest;
import com.booking.dto.BookingResponse;
import com.booking.entity.Booking;
import com.booking.entity.BookingAttemptLog;
import com.booking.entity.Seat;
import com.booking.repository.BookingAttemptLogRepository;
import com.booking.repository.BookingRepository;
import com.booking.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingAttemptLogRepository logRepository;

    /**
     * Books a seat with full race-condition safety.
     *
     * Flow:
     * 1. Idempotency check - if this booking_ref was already processed
     *    (double-click / network retry), return the existing result
     *    instead of creating a duplicate booking.
     * 2. Row-level lock (SELECT ... FOR UPDATE) on the seat row inside
     *    a DB transaction. If 20 threads hit this concurrently for the
     *    SAME seat, PostgreSQL serializes them here - only one thread
     *    proceeds at a time.
     * 3. Re-check seat status AFTER acquiring the lock (not before) -
     *    this is what actually prevents the double-booking race.
     * 4. Every attempt (success or failure) is logged to MongoDB for
     *    audit / proof-of-correctness under load.
     */
    @Transactional
    public BookingResponse bookSeat(BookingRequest request) {

        // --- Step 1: Idempotency check ---
        Optional<Booking> existing = bookingRepository.findByBookingRef(request.getBookingRef());
        if (existing.isPresent()) {
            Booking b = existing.get();
            logAttempt(request, "success", "Idempotent replay - booking already exists");
            return new BookingResponse(true, "Booking already confirmed (duplicate request ignored)",
                    b.getBookingRef(), null);
        }

        // --- Step 2: Row-level lock on the seat ---
        Optional<Seat> seatOpt = seatRepository.findByIdForUpdate(request.getSeatId());

        if (seatOpt.isEmpty()) {
            logAttempt(request, "invalid_seat", "Seat does not exist");
            return new BookingResponse(false, "Invalid seat", null, null);
        }

        Seat seat = seatOpt.get();

        // --- Step 3: Re-check status AFTER lock acquired ---
        if (!"available".equals(seat.getStatus())) {
            logAttempt(request, "already_booked", "Seat already booked, please choose another seat");
            return new BookingResponse(false, "Seat already booked, please choose another seat", null,
                    seat.getSeatNumber());
        }

        // --- Step 4: Mark seat booked, create booking record ---
        seat.setStatus("booked");
        seatRepository.save(seat);

        Booking booking = new Booking();
        booking.setSeatId(seat.getSeatId());
        booking.setUserId(request.getUserId());
        booking.setBookingRef(request.getBookingRef());
        booking.setStatus("confirmed");
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        logAttempt(request, "success", "Booking confirmed");

        return new BookingResponse(true, "Booking confirmed", booking.getBookingRef(), seat.getSeatNumber());
    }

    private void logAttempt(BookingRequest request, String result, String message) {
        BookingAttemptLog log = new BookingAttemptLog(
                null,
                LocalDateTime.now(),
                request.getUserId(),
                request.getSeatId(),
                null,
                result,
                message
        );
        logRepository.save(log);
    }
}
