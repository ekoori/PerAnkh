#!/usr/bin/env bash
# Installs or updates the Per Ankh API on this server. Run as root from the unpacked bundle:
#
#   sudo bash deploy/install.sh                       # code, service and nginx site
#   sudo bash deploy/install.sh --issue-certificate   # also get the Let's Encrypt certificate
#
# The service runs as its own user, `perankh`, from /opt/perankh (root-owned, read-only to it),
# with its database in /var/lib/perankh. nginx gets its own site file; other sites' files are
# never touched, and a site that fails `nginx -t` is rolled back before nginx is reloaded.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
BUNDLE="$(dirname "$HERE")"
DOMAIN="${PERANKH_DOMAIN:-perankh.hrana.app}"
APP=/opt/perankh
DATA=/var/lib/perankh
ACME=/var/www/perankh-acme
SITE=/etc/nginx/sites-available/perankh
LINK=/etc/nginx/sites-enabled/perankh
LIMITS=/etc/nginx/conf.d/perankh-limits.conf
CERT="/etc/letsencrypt/live/$DOMAIN/fullchain.pem"

[ "$(id -u)" -eq 0 ] || { echo "run as root" >&2; exit 1; }

log() { echo "-- $*"; }

# -- user and directories -------------------------------------------------------------------
if ! id perankh >/dev/null 2>&1; then
    useradd --system --no-create-home --home-dir /nonexistent --shell /usr/sbin/nologin perankh
    log "created system user perankh"
fi
install -d -o root -g root -m 755 "$APP" "$APP/static" "$ACME"
install -d -o perankh -g perankh -m 700 "$DATA"

# -- code -------------------------------------------------------------------------------------
rm -rf "$APP/perankh.new"
cp -r "$BUNDLE/perankh" "$APP/perankh.new"
find "$APP/perankh.new" -name __pycache__ -prune -exec rm -rf {} +
rm -rf "$APP/perankh.old"
[ -d "$APP/perankh" ] && mv "$APP/perankh" "$APP/perankh.old"
mv "$APP/perankh.new" "$APP/perankh"
rm -rf "$APP/perankh.old"
chown -R root:root "$APP"
find "$APP" -type d -exec chmod 755 {} +
find "$APP" -type f -exec chmod 644 {} +
[ -f "$BUNDLE/static/privacy.html" ] && install -o root -g root -m 644 "$BUNDLE/static/privacy.html" "$APP/static/privacy.html"
install -o root -g root -m 755 "$HERE/perankh-reports" /usr/local/bin/perankh-reports
log "code installed in $APP"

# -- service ------------------------------------------------------------------------------------
install -o root -g root -m 644 "$HERE/perankh.service" /etc/systemd/system/perankh.service
systemctl daemon-reload
systemctl enable perankh >/dev/null 2>&1
systemctl restart perankh
for _ in 1 2 3 4 5 6 7 8 9 10; do
    curl -sf http://127.0.0.1:5101/v1/health >/dev/null && break
    sleep 0.5
done
curl -sf http://127.0.0.1:5101/v1/health >/dev/null || { journalctl -u perankh -n 30 --no-pager; exit 1; }
log "service perankh is up on 127.0.0.1:5101"

# -- nginx --------------------------------------------------------------------------------------
apply_site() {
    local template="$1" backup
    backup="$(mktemp -d)"
    [ -f "$SITE" ] && cp -a "$SITE" "$backup/site"
    [ -f "$LIMITS" ] && cp -a "$LIMITS" "$backup/limits"
    local had_link=no
    [ -L "$LINK" ] && had_link=yes
    install -o root -g root -m 644 "$HERE/nginx-limits.conf" "$LIMITS"
    sed "s/__DOMAIN__/$DOMAIN/g" "$HERE/$template" > "$SITE"
    chmod 644 "$SITE"
    ln -sfn "$SITE" "$LINK"
    if nginx -t >/dev/null 2>&1; then
        systemctl reload nginx
        log "nginx site $DOMAIN: $template"
    else
        echo "nginx rejected the new config; restoring the previous one" >&2
        nginx -t || true
        if [ -f "$backup/site" ]; then cp -a "$backup/site" "$SITE"; else rm -f "$SITE"; fi
        if [ -f "$backup/limits" ]; then cp -a "$backup/limits" "$LIMITS"; else rm -f "$LIMITS"; fi
        [ "$had_link" = yes ] || rm -f "$LINK"
        nginx -t && systemctl reload nginx
        exit 1
    fi
}

if [ -f "$CERT" ]; then apply_site nginx-https.conf; else apply_site nginx-http.conf; fi

# -- certificate --------------------------------------------------------------------------------
if [ "${1:-}" = "--issue-certificate" ] && [ ! -f "$CERT" ]; then
    here_ips="$(ip -4 -o addr show scope global | awk '{split($4, a, "/"); print a[1]}')"
    dns_ip="$(getent ahostsv4 "$DOMAIN" | awk 'NR==1 {print $1}')"
    if [ -z "$dns_ip" ] || ! printf '%s\n' "$here_ips" | grep -qx "$dns_ip"; then
        echo "$DOMAIN resolves to '${dns_ip:-nothing}', not this server ($(echo $here_ips))." >&2
        echo "Add a DNS-only A record for $DOMAIN pointing here (Cloudflare: grey cloud), then run again." >&2
        exit 1
    fi
    # No email: Let's Encrypt no longer sends expiry warnings, and renewal is automatic.
    certbot certonly --webroot -w "$ACME" -d "$DOMAIN" --non-interactive --agree-tos \
        --register-unsafely-without-email --deploy-hook "systemctl reload nginx"
    apply_site nginx-https.conf
fi

[ -f "$CERT" ] && log "https://$DOMAIN is live" || log "no certificate yet: run again with --issue-certificate once DNS points here"
