package com.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private boolean success;

    private String message;

    private UUID bookingRef;

    private String seatNumber;
}
