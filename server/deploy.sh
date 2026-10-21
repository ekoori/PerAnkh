#!/usr/bin/env bash
# Ships the Per Ankh API to its server and installs it.
#
#   server/deploy.sh                       # code, service and nginx site
#   server/deploy.sh --issue-certificate   # also get the TLS certificate (DNS must point there)
#
# Needs SSH access as a user with sudo: PERANKH_HOST=user@host, or that one line in server/.host
# (kept out of git).
set -euo pipefail

cd "$(dirname "$0")"
HOST="${PERANKH_HOST:-$(head -n 1 .host 2>/dev/null | tr -d '\r' || true)}"
[ -n "$HOST" ] || { echo "set PERANKH_HOST=user@host, or put that line in server/.host" >&2; exit 1; }
# The first Python that actually runs (on Windows, `python3` may be a Store placeholder).
PYTHON=""
for candidate in python3 python; do
    if "$candidate" -c "import sys" >/dev/null 2>&1; then PYTHON="$candidate"; break; fi
done
[ -n "$PYTHON" ] || { echo "no working Python found" >&2; exit 1; }

# Never ship code whose tests fail.
"$PYTHON" -m unittest discover -s tests >/dev/null 2>&1 || { "$PYTHON" -m unittest discover -s tests; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
tar --exclude='__pycache__' -czf "$work/bundle.tar.gz" perankh deploy static
scp -q "$work/bundle.tar.gz" "$HOST:/tmp/perankh-bundle.tar.gz"
ssh "$HOST" "set -e
    rm -rf /tmp/perankh-bundle && mkdir /tmp/perankh-bundle
    tar -xzf /tmp/perankh-bundle.tar.gz -C /tmp/perankh-bundle
    sudo bash /tmp/perankh-bundle/deploy/install.sh $*
    rm -rf /tmp/perankh-bundle /tmp/perankh-bundle.tar.gz"
