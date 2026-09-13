package com.booking.repository;

import com.booking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Integer> {

    List<Seat> findByEventId(Integer eventId);

    // Row-level pessimistic lock: SELECT ... FOR UPDATE
    // This is the core race-condition guard. Two concurrent transactions
    // requesting the same seat_id will serialize here - the second
    // transaction blocks until the first commits or rolls back.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatId = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") Integer seatId);
}
