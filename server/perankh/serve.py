"""Runs the API on a loopback port, behind nginx.

    python3 -m perankh.serve --db /var/lib/perankh/reports.db --port 5101

Only the standard library: a threaded WSGI server is plenty for a reports endpoint, and nginx in
front handles TLS, slow clients, body limits and rate limiting. Nothing about a request is logged.
"""
from __future__ import annotations

import argparse
import socketserver
import sys
from wsgiref.simple_server import WSGIRequestHandler, WSGIServer, make_server

from .app import Api


class QuietHandler(WSGIRequestHandler):
    # A hung client must not hold a thread forever.
    timeout = 20

    def log_message(self, format, *args):  # noqa: A002 - the base class's name
        """No access log: the service keeps no record of who sent what."""


class ThreadingServer(socketserver.ThreadingMixIn, WSGIServer):
    daemon_threads = True


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="Per Ankh report API")
    parser.add_argument("--db", required=True, help="SQLite database file")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=5101)
    args = parser.parse_args(argv)
    if args.host not in ("127.0.0.1", "::1", "localhost"):
        sys.exit("refusing to listen beyond loopback: nginx is the only way in")
    api = Api(args.db)
    with make_server(args.host, args.port, api, server_class=ThreadingServer, handler_class=QuietHandler) as httpd:
        print(f"perankh api on http://{args.host}:{args.port}, database {args.db}", flush=True)
        httpd.serve_forever()


if __name__ == "__main__":
    main()
