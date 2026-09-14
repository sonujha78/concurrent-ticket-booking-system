package com.booking.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    private String name;

    private String venue;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(name = "total_seats")
    private Integer totalSeats;
}
