package com.booking.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seats")
@Data
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Integer seatId;

    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "seat_number")
    private String seatNumber;

    // available | locked | booked
    private String status;
}
