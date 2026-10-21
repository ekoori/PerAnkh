"""perankh-reports: triage tester reports on the server.

    perankh-reports list [--all | --status S ...] [--category C] [--item ID] [--limit N]
    perankh-reports show ID                 # a unique prefix of 6+ characters is enough
    perankh-reports status ID STATUS [--note TEXT]
    perankh-reports resolve ID --in VERSION [--note TEXT]
    perankh-reports duplicate ID --of OTHER
    perankh-reports export [--all | --status S ...]   # JSON, for investigating elsewhere
    perankh-reports stats
    perankh-reports prune --closed-days N   # delete closed reports untouched for N days

The database is $PERANKH_DB, by default /var/lib/perankh/reports.db. From a development machine,
tools/reports.sh runs this over SSH.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys

from . import db

DEFAULT_DB = "/var/lib/perankh/reports.db"


def when(ms: int | None) -> str:
    if not ms:
        return "-"
    return dt.datetime.fromtimestamp(ms / 1000, dt.timezone.utc).strftime("%Y-%m-%d %H:%M")


def one_line(text: str, width: int) -> str:
    text = " ".join((text or "").split())
    return text if len(text) <= width else text[: width - 1] + "…"


def require(conn, prefix: str):
    row = db.find(conn, prefix)
    if row is None:
        sys.exit(f"no single report matches {prefix!r} (give at least 6 characters of its id)")
    return row


def as_dict(row) -> dict:
    out = dict(row)
    out["context"] = json.loads(out["context"])
    return out


def cmd_list(conn, args) -> None:
    statuses = None if args.all else (args.status or list(db.OPEN))
    rows = db.query(conn, statuses, args.category, args.item, args.limit)
    if not rows:
        print("no reports")
        return
    for r in rows:
        where = " ".join(x for x in (r["lesson"], r["exercise"], r["item"]) if x)
        print(f"{r['id'][:8]}  {when(r['received_at'])}  {r['status']:<11} {r['category']:<22} "
              f"{one_line(where, 40):<40}  {one_line(r['note'], 60)}")


def cmd_show(conn, args) -> None:
    r = as_dict(require(conn, args.id))
    print(f"id:        {r['id']}")
    print(f"status:    {r['status']}" + (f"  ({r['resolution']})" if r["resolution"] else ""))
    if r["resolved_in"]:
        print(f"fixed in:  {r['resolved_in']}")
    if r["duplicate_of"]:
        print(f"duplicate: {r['duplicate_of']}")
    print(f"category:  {r['category']}")
    print(f"saved:     {when(r['created_at'])} (phone)   received: {when(r['received_at'])}")
    print(f"app:       {r['app_version'] or '-'}   Android API {r['android_sdk'] or '-'}")
    print(f"note:      {r['note'] or '-'}")
    print("context:")
    width = max((len(k) for k in r["context"]), default=0)
    for key, value in r["context"].items():
        print(f"  {key:<{width}}  {value}")
    print("history:")
    for h in db.history(conn, r["id"]):
        print(f"  {when(h['at'])}  {h['action']}" + (f": {h['detail']}" if h["detail"] else ""))


def cmd_status(conn, args) -> None:
    r = require(conn, args.id)
    db.set_status(conn, r["id"], args.status, note=args.note)
    print(f"{r['id'][:8]} -> {args.status}")


def cmd_resolve(conn, args) -> None:
    r = require(conn, args.id)
    db.set_status(conn, r["id"], "fixed", note=args.note, resolved_in=getattr(args, "in"))
    print(f"{r['id'][:8]} -> fixed in {getattr(args, 'in')}")


def cmd_duplicate(conn, args) -> None:
    r = require(conn, args.id)
    other = require(conn, args.of)
    db.set_status(conn, r["id"], "duplicate", duplicate_of=other["id"])
    print(f"{r['id'][:8]} -> duplicate of {other['id'][:8]}")


def cmd_export(conn, args) -> None:
    statuses = None if args.all else (args.status or list(db.OPEN))
    rows = db.query(conn, statuses, None, None, args.limit)
    json.dump([as_dict(r) for r in rows], sys.stdout, ensure_ascii=False, indent=1)
    print()


def cmd_stats(conn, args) -> None:
    for status, count in conn.execute("SELECT status, COUNT(*) FROM reports GROUP BY status ORDER BY status"):
        print(f"{status:<12} {count}")
    print("open by category:")
    for category, count in conn.execute(
            f"SELECT category, COUNT(*) FROM reports WHERE status IN ({','.join('?' * len(db.OPEN))}) "
            "GROUP BY category ORDER BY COUNT(*) DESC", db.OPEN):
        print(f"  {category:<24} {count}")


def cmd_prune(conn, args) -> None:
    cutoff = db.now_ms() - args.closed_days * 86_400_000
    print(f"deleted {db.prune(conn, cutoff)} closed reports untouched since {when(cutoff)}")


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(prog="perankh-reports", description="Triage Per Ankh tester reports")
    parser.add_argument("--db", default=os.environ.get("PERANKH_DB", DEFAULT_DB))
    sub = parser.add_subparsers(dest="command", required=True)

    def filters(p, limit=50):
        p.add_argument("--status", action="append", choices=db.STATUSES)
        p.add_argument("--all", action="store_true", help="every status, not just open ones")
        p.add_argument("--limit", type=int, default=limit)

    p = sub.add_parser("list", help="open reports, newest first")
    filters(p)
    p.add_argument("--category")
    p.add_argument("--item", help="an item id such as w:nfr or s:G17")
    p.set_defaults(func=cmd_list)

    p = sub.add_parser("show", help="one report in full")
    p.add_argument("id")
    p.set_defaults(func=cmd_show)

    p = sub.add_parser("status", help="move a report to another status")
    p.add_argument("id")
    p.add_argument("status", choices=db.STATUSES)
    p.add_argument("--note")
    p.set_defaults(func=cmd_status)

    p = sub.add_parser("resolve", help="mark fixed, with the version that fixes it")
    p.add_argument("id")
    p.add_argument("--in", required=True, metavar="VERSION")
    p.add_argument("--note")
    p.set_defaults(func=cmd_resolve)

    p = sub.add_parser("duplicate", help="mark as a duplicate of another report")
    p.add_argument("id")
    p.add_argument("--of", required=True)
    p.set_defaults(func=cmd_duplicate)

    p = sub.add_parser("export", help="reports as JSON")
    filters(p, limit=1000)
    p.set_defaults(func=cmd_export)

    sub.add_parser("stats", help="counts by status and category").set_defaults(func=cmd_stats)

    p = sub.add_parser("prune", help="delete closed reports untouched for N days")
    p.add_argument("--closed-days", type=int, required=True)
    p.set_defaults(func=cmd_prune)

    args = parser.parse_args(argv)
    if not os.path.exists(args.db):
        sys.exit(f"no database at {args.db}")
    conn = db.connect(args.db)
    try:
        args.func(conn, args)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
