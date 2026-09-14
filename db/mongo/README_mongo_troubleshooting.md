# MongoDB Setup Notes

## Known Issue: Ubuntu 26.04 (kernel 6.19+) crashes MongoDB 8.0 on startup

MongoDB 8.0+ has a known incompatibility (MongoDB Jira SERVER-121912) with Linux kernel 6.19 and newer, caused by a TCMalloc/rseq ABI conflict.

`mongod` exits immediately on start with the following error:
MongoDB cannot start: Linux kernel versions 6.19 and newer has a known incompatibility with this version of MongoDB.

This affects all MongoDB packages — official website downloads, apt/package managers, and Docker images alike.

---

## Fix Applied

A systemd override was created to set the `GLIBC_TUNABLES` environment variable, which resolves the crash.

**File:** `/etc/systemd/system/mongod.service.d/rseq.conf`

```ini
[Service]
Environment=GLIBC_TUNABLES=glibc.pthread.rseq=1
```

**Commands used to apply the fix:**

```bash
sudo mkdir -p /etc/systemd/system/mongod.service.d

sudo tee /etc/systemd/system/mongod.service.d/rseq.conf << 'EOF'
[Service]
Environment=GLIBC_TUNABLES=glibc.pthread.rseq=1
EOF

sudo systemctl daemon-reload
sudo systemctl restart mongod
```

**Verification:**

```bash
sudo systemctl status mongod --no-pager
mongosh --eval "db.version()"
```

---

## Init Script Note

Used `db.getSiblingDB('bookingLogs')` instead of the legacy `use bookingLogs;` statement inside `init_booking_logs.js`.

Reason: the `use <db>` statement does not reliably switch database context when running `mongosh` in script/file mode — it can silently no-op in some `mongosh` versions, causing the script to run against the wrong database.

```javascript
const database = db.getSiblingDB('bookingLogs');
database.createCollection("booking_attempts");
```

---

## Collections Created

| Database    | Collection        | Indexes                                  |
|-------------|--------------------|--------------------------------------------|
| bookingLogs | booking_attempts   | seat_id, user_id, timestamp (descending)   |

Every booking attempt (success and failure) is logged here with `timestamp`, `user_id`, `seat_id`, and `result`.
