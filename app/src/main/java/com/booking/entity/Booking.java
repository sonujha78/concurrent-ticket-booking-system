package com.booking.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "seat_id")
    private Integer seatId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "booking_ref", unique = true)
    private UUID bookingRef;

    // confirmed | failed | cancelled
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
