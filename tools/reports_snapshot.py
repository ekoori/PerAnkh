"""A read-only snapshot of the report database, as JSON on stdout, for tools/reports_xlsx.py.

Runs on the server as the service user, sent over SSH on stdin, so nothing has to be deployed:

    ssh HOST "sudo -n -u perankh /usr/bin/python3 -I -" < tools/reports_snapshot.py
    python tools/reports_snapshot.py some/reports.db      # a copy of the database, locally

The database is opened read-only and read in one transaction, so every table comes from the same
moment, and nothing is written. Every table in sqlite_master is read, with its columns, foreign
keys and indexes, so a table that a later migration adds comes along without changing this.
Standard library only, and Python 3.8 or later: it runs on the server's own Python.
"""
import json
import sqlite3
import sys
import time

DB = sys.argv[1] if len(sys.argv) > 1 else "/var/lib/perankh/reports.db"


def quote(name):
    return '"' + name.replace('"', '""') + '"'


def value(v):
    return {"$blob": v.hex()} if isinstance(v, bytes) else v


conn = sqlite3.connect("file:" + DB + "?mode=ro", uri=True, timeout=10, isolation_level=None)
conn.execute("PRAGMA busy_timeout=10000")
conn.execute("BEGIN")
snapshot = {
    "database": DB,
    "sqlite_version": sqlite3.sqlite_version,
    "user_version": conn.execute("PRAGMA user_version").fetchone()[0],
    "journal_mode": conn.execute("PRAGMA journal_mode").fetchone()[0],
    "objects": [{"type": t, "name": n, "table": tb, "sql": s} for t, n, tb, s in
                conn.execute("SELECT type, name, tbl_name, sql FROM sqlite_master ORDER BY rowid")],
    "tables": {},
}
for (name,) in conn.execute("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY rowid").fetchall():
    columns = [dict(zip(("cid", "name", "type", "notnull", "default", "pk"), row))
               for row in conn.execute("PRAGMA table_info(" + quote(name) + ")")]
    foreign_keys = [dict(zip(("id", "seq", "table", "from", "to", "on_update", "on_delete", "match"), row))
                    for row in conn.execute("PRAGMA foreign_key_list(" + quote(name) + ")")]
    indexes = []
    for _, index, unique, origin, partial in conn.execute("PRAGMA index_list(" + quote(name) + ")").fetchall():
        indexed = [c for _, _, c in conn.execute("PRAGMA index_info(" + quote(index) + ")")]
        indexes.append({"name": index, "unique": unique, "origin": origin, "partial": partial, "columns": indexed})
    try:
        cursor = conn.execute("SELECT * FROM " + quote(name) + " ORDER BY rowid")
    except sqlite3.OperationalError:  # a WITHOUT ROWID table
        cursor = conn.execute("SELECT * FROM " + quote(name))
    snapshot["tables"][name] = {"columns": columns, "foreign_keys": foreign_keys, "indexes": indexes,
                                "rows": [[value(v) for v in row] for row in cursor]}
snapshot["taken_at_ms"] = int(time.time() * 1000)
conn.execute("ROLLBACK")
conn.close()
json.dump(snapshot, sys.stdout)
