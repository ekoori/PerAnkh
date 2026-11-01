#!/usr/bin/env bash
# Puts Pl@ntNet's API key on the server, for BushLines' plant identification (/v1/identify).
#
#   server/plantnet-key.sh /path/to/keyfile      # a file holding the bare key, e.g. BushLines' .env
#
# The key goes over SSH on standard input — never on a command line, in the bundle or in git — to
# /etc/perankh/plantnet.key (root:perankh, 640). The service reads it on each request, so no
# restart is needed. Same login as server/deploy.sh: PERANKH_HOST, or the line in server/.host.
set -euo pipefail

cd "$(dirname "$0")"
KEYFILE="${1:-}"
[ -n "$KEYFILE" ] && [ -f "$KEYFILE" ] || { echo "usage: server/plantnet-key.sh /path/to/keyfile" >&2; exit 1; }
HOST="${PERANKH_HOST:-$(head -n 1 .host 2>/dev/null | tr -d '\r' || true)}"
[ -n "$HOST" ] || { echo "set PERANKH_HOST=user@host, or put that line in server/.host" >&2; exit 1; }
# Strip the newline and any spaces: the file holds the key alone.
tr -d ' \r\n\t' < "$KEYFILE" | ssh "$HOST" "set -e
    sudo install -d -o root -g perankh -m 750 /etc/perankh
    sudo sh -c 'umask 137; cat > /etc/perankh/plantnet.key.new'
    sudo chown root:perankh /etc/perankh/plantnet.key.new
    sudo chmod 640 /etc/perankh/plantnet.key.new
    sudo mv /etc/perankh/plantnet.key.new /etc/perankh/plantnet.key
    echo \"key installed: \$(sudo cat /etc/perankh/plantnet.key | wc -c) characters\""
