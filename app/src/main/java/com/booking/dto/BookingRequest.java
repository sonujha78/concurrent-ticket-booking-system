package com.booking.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class BookingRequest {

    private Integer seatId;

    private String userId;

    // idempotency key: client generates this once per booking attempt,
    // and resends the SAME value on retry (double-click, network retry)
    private UUID bookingRef;
}
