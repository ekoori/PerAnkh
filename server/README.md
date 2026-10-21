# Per Ankh server

A small service that receives tester reports from the app, so they can be triaged, investigated
and resolved in one place. It can share a server with other sites, and shares nothing with them
except nginx: its own system user, code directory, database, systemd unit and nginx site.

```
phone ──HTTPS──▶ nginx (perankh.hrana.app, Let's Encrypt) ──▶ 127.0.0.1:5101 perankh.service ──▶ SQLite
```

`perankh.hrana.app` is a **DNS-only** record (grey cloud in Cloudflare): TLS ends on the server,
so no proxy in between sees a report.

## API

| | |
|---|---|
| `GET /v1/health` | `{"ok": true, "schema": 1}` |
| `POST /v1/reports` | Body `{"app": {"version", "sdk"}, "reports": [{"id", "at", "category", "note", "app", "context"}]}`; replies `{"accepted": [ids], "duplicate": [ids], "rejected": [{"id", "error"}]}` |
| `GET /privacy` | The privacy policy page (static) |

Everything else is 404, other methods 403/405. The version prefix lets later releases add
endpoints (accounts, progress) without breaking apps already installed.

Uploads are idempotent: a report's id is a UUID made on the phone, so a retry after a dropped
connection stores nothing twice. A bad report is rejected on its own; the rest of the batch is
kept.

## Security and privacy

- **Transport**: HTTPS only, TLS 1.2/1.3, HSTS. The app forbids cleartext and trusts only the
  system's certificate authorities (`app/src/main/res/xml/network_security_config.xml`). nginx
  refuses the TLS handshake for any other name on port 443.
- **Nothing identifying is collected**: no account, device id, advertising id or location; the
  body carries the app version and Android API level only. nginx writes no access log for this
  site and does not forward client addresses; the service logs no requests at all. The client
  address is used, in memory, only for rate limiting.
- **Abuse limits**: 10 uploads a minute per address (burst 5) at nginx; 256 KB per request; 50
  reports per upload; strict field validation and length limits; control characters stripped; the
  service stops accepting reports if the database passes 512 MB.
- **No admin surface on the network**: triage happens over SSH on the server. There is nothing
  to log into over HTTP.
- **Least privilege**: the service runs as `perankh` (no shell, no home), can write only
  `/var/lib/perankh` (mode 700, files 600), runs from root-owned read-only code, and is boxed in
  by systemd (`systemd-analyze security perankh` rates it 1.4, "OK").
- **The key never ships**: the app holds no secret. Anyone can call the API, which is why every
  limit is enforced server-side. If abuse appears, the next step is Play Integrity attestation for
  the Play build.

## Data

SQLite in WAL mode, `/var/lib/perankh/reports.db`. Schema version in `PRAGMA user_version`;
`perankh/db.py` migrates on start, each migration in one transaction.

`reports`: id, received/created times, app version, Android API level, category, note, the
indexed context fields (screen, lesson, exercise, item), the full context as JSON, triage status
(`new`, `triaged`, `in_progress`, `fixed`, `released`, `wontfix`, `duplicate`, `invalid`),
resolution, the version that fixes it, duplicate link. `report_log`: every status change.

Retention follows the privacy policy: `perankh-reports prune --closed-days 365` deletes closed
reports untouched for a year. Open reports are never pruned.

## Triage

From a development machine, over SSH (`tools/reports.sh` runs `perankh-reports` on the server):

```bash
tools/reports.sh list                       # open reports, newest first
tools/reports.sh list --item s:G17          # everything about one sign
tools/reports.sh show 3f2a9c                # one report in full: note, screen, answer, clip
tools/reports.sh status 3f2a9c in_progress
tools/reports.sh resolve 3f2a9c --in 0.2.1 --note "clip re-recorded"
tools/reports.sh duplicate 77ab01 --of 3f2a9c
tools/reports.sh export > open.json         # JSON for investigating locally
tools/reports.sh stats
```

## Deploying

```bash
server/deploy.sh                       # tests, ship, install, restart; nginx -t before reload
server/deploy.sh --issue-certificate   # once: Let's Encrypt for perankh.hrana.app
```

`server/deploy.sh` and `tools/reports.sh` reach the server over SSH, as a user with sudo. They
read that login, `user@host`, from `PERANKH_HOST` or from `server/.host`, a one-line file kept
out of git.

`deploy/install.sh` is idempotent and rolls back its own nginx files if `nginx -t` fails, so a
mistake here cannot take down the other sites on the server. Certificates renew automatically
(certbot's timer), reloading nginx on renewal.

Back up the database with `sqlite3 /var/lib/perankh/reports.db ".backup /path/backup.db"`, which
is safe while the service runs.

## Later: accounts and progress sync

Designed for, not built. When people can sign in to keep their progress across devices:

- **Sign-in without passwords**: an emailed one-time link or Google sign-in through Android's
  Credential Manager. The server stores no passwords.
- **Sessions**: random 256-bit bearer tokens, stored only as SHA-256 hashes, with expiry and a
  last-used time; revoked on sign-out.
- **Tables** (migration 2): `accounts` (id, email, created, deleted), `sessions` (token hash,
  account, expiry), `progress` (account, revision, updated, the Progress JSON). `reports.account_id`
  stays NULL unless a signed-in tester chooses to attach their account to a report.
- **Sync**: `GET /v1/progress` returns `{revision, data}`; `PUT /v1/progress` with the revision
  it started from. On a conflict (409) the app merges (per item, the memory with the later review
  wins; awards and counters take the union and the maximum) and retries. The progress document is
  the app's existing `Progress`, so nothing new has to be modelled.
- **Deletion and export**: Google Play requires deleting an account both in the app and from a
  web page; `DELETE /v1/account` and a page under `/account/delete`, plus `GET /v1/account/export`.
- **Before that ships**: update PRIVACY.md and the Play Data safety form (email address; app
  activity, for progress), schedule encrypted off-site database backups, and add per-account rate
  limits.
