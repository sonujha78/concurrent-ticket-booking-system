package com.booking.service;

import com.booking.dto.BookingRequest;
import com.booking.dto.BookingResponse;
import com.booking.entity.Booking;
import com.booking.entity.Seat;
import com.booking.repository.BookingAttemptLogRepository;
import com.booking.repository.BookingRepository;
import com.booking.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simulates two concurrent booking attempts for the SAME seat.
 * A ReentrantLock inside the mocked repository stands in for PostgreSQL's
 * row-level "SELECT ... FOR UPDATE" lock, since a live database is not
 * available during the Jenkins unit-test stage. The assertion mirrors the
 * production expectation: exactly one thread succeeds, the other is
 * correctly rejected as "already booked".
 */
class BookingServiceConcurrencyTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingAttemptLogRepository logRepository;

    @InjectMocks
    private BookingService bookingService;

    private final ReentrantLock seatLock = new ReentrantLock();
    private Seat sharedSeat;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sharedSeat = new Seat();
        sharedSeat.setSeatId(99);
        sharedSeat.setEventId(1);
        sharedSeat.setSeatNumber("TEST1");
        sharedSeat.setStatus("available");
    }

    @Test
    void onlyOneOfTwoConcurrentBookingsShouldSucceed() throws InterruptedException {
        // Idempotency lookup: no existing booking for either request's ref
        org.mockito.Mockito.when(bookingRepository.findByBookingRef(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());

        // Row-level lock simulation: only one thread can read+modify the
        // shared seat at a time, mirroring PESSIMISTIC_WRITE behavior.
        org.mockito.Mockito.when(seatRepository.findByIdForUpdate(99)).thenAnswer(invocation -> {
            seatLock.lock();
            try {
                return Optional.of(sharedSeat);
            } finally {
                // lock released only after the caller finishes reading+writing status,
                // simulated below via the save() call releasing the lock
            }
        });

        org.mockito.Mockito.when(seatRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            Seat s = invocation.getArgument(0);
            try {
                return s;
            } finally {
                if (seatLock.isHeldByCurrentThread()) {
                    seatLock.unlock();
                }
            }
        });

        org.mockito.Mockito.when(bookingRepository.save(org.mockito.ArgumentMatchers.any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable attempt = () -> {
            try {
                BookingRequest req = new BookingRequest();
                req.setSeatId(99);
                req.setUserId("concurrent_test_user");
                req.setBookingRef(UUID.randomUUID());
                BookingResponse resp = bookingService.bookSeat(req);
                if (resp.isSuccess()) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        };

        Thread t1 = new Thread(attempt);
        Thread t2 = new Thread(attempt);
        t1.start();
        t2.start();
        latch.await();

        assertEquals(1, successCount.get(), "Exactly one concurrent booking attempt should succeed");
        assertEquals(1, failureCount.get(), "Exactly one concurrent booking attempt should be rejected as already booked");
    }
}
