package com.booking.repository;

import com.booking.entity.BookingAttemptLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookingAttemptLogRepository extends MongoRepository<BookingAttemptLog, String> {
}
