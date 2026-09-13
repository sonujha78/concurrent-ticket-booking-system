# MongoDB Setup Notes

## Known Issue: Ubuntu 26.04 (kernel 6.19+) crashes MongoDB 8.0 on startup

MongoDB 8.0+ has a known incompatibility (MongoDB Jira SERVER-121912) with
Linux kernel 6.19 and newer, caused by a TCMalloc/rseq ABI conflict. mongod
exits immediately on start with:
MongoDB cannot start: Linux kernel versions 6.19 and newer has a known
incompatibility with this version of MongoDB.

### Fix applied
A systemd override was added to set GLIBC_TUNABLES, which resolves the crash:
/etc/systemd/system/mongod.service.d/rseq.conf
[Service]
Environment=GLIBC_TUNABLES=glibc.pthread.rseq=1

Applied via:
```bash
sudo systemctl daemon-reload
sudo systemctl restart mongod
```

## Init script note
Used `db.getSiblingDB('bookingLogs')` instead of the legacy `use bookingLogs;`
statement, since `use` does not reliably switch context when running mongosh
in script/file mode (silently no-ops in some versions).
