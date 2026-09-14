package com.booking.repository;

import com.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // Idempotency check: if this booking_ref was already processed,
    // return the existing row instead of creating a duplicate.
    Optional<Booking> findByBookingRef(UUID bookingRef);
}
