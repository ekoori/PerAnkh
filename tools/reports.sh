#!/usr/bin/env bash
# Triage Per Ankh tester reports from this machine, on the server, over SSH:
#
#   tools/reports.sh list                  # open reports, newest first
#   tools/reports.sh show 3f2a9c           # one report in full (6+ characters of its id)
#   tools/reports.sh resolve 3f2a9c --in 0.2.1 --note "re-recorded the clip"
#   tools/reports.sh export > open.json    # JSON, to investigate locally
#
# The server's SSH login comes from PERANKH_HOST=user@host, or that one line in server/.host
# (kept out of git).
set -euo pipefail
HOST="${PERANKH_HOST:-$(head -n 1 "$(dirname "$0")/../server/.host" 2>/dev/null | tr -d '\r' || true)}"
[ -n "$HOST" ] || { echo "set PERANKH_HOST=user@host, or put that line in server/.host" >&2; exit 1; }
args=""
for a in "$@"; do args="$args $(printf '%q' "$a")"; done
exec ssh "$HOST" "perankh-reports$args"
