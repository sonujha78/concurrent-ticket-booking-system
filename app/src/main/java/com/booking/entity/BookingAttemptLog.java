package com.booking.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "booking_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingAttemptLog {

    @Id
    private String id;

    private LocalDateTime timestamp;

    private String userId;

    private Integer seatId;

    private Integer eventId;

    // success | already_booked | invalid_seat | error
    private String result;

    private String message;
}
