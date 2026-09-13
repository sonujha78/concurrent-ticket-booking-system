const database = db.getSiblingDB('bookingLogs');

database.createCollection("booking_attempts");

database.booking_attempts.createIndex({ seat_id: 1 });
database.booking_attempts.createIndex({ user_id: 1 });
database.booking_attempts.createIndex({ timestamp: -1 });

database.booking_attempts.insertOne({
  timestamp: new Date(),
  user_id: "sample_user",
  seat_id: 1,
  event_id: 1,
  result: "success",
  message: "Seed record - collection initialized"
});

print("bookingLogs.booking_attempts initialized successfully");
