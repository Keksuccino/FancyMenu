#!/usr/bin/env python3
"""
Very small FancyMenu remote test server (WebSocket / Secure WebSocket).

Protocol expected by FancyMenu remote action:
  request_id=<id>\n<payload>

This server echoes the request_id back and sends simple text responses:
  request_id=<id>\n<response text>

Usage (wss):
  python remote_wss_test_server.py --host 0.0.0.0 --port 8765 --cert cert.pem --key key.pem

Usage (ws, no TLS):
  python remote_wss_test_server.py --host 127.0.0.1 --port 8765
"""

from __future__ import annotations

import argparse
import asyncio
import ssl
from typing import Tuple

import websockets


def parse_incoming_message(message: str) -> Tuple[str, str]:
    prefix = "request_id="
    if message.startswith(prefix):
        newline_index = message.find("\n")
        if newline_index > len(prefix):
            request_id = message[len(prefix):newline_index].strip()
            payload = message[newline_index + 1 :]
            if request_id:
                return request_id, payload
    return "unknown_request_id", message


def build_response(request_id: str, payload: str) -> str:
    body = f"Hello client! | ORIGINAL RECEIVED DATA FROM CLIENT: {payload}"
    return f"request_id={request_id}\n{body}"


async def handle_client(websocket) -> None:
    client = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}" if websocket.remote_address else "unknown_client"
    print(f"[INFO] Connected: {client}")
    try:
        async for raw in websocket:
            if not isinstance(raw, str):
                continue

            request_id, payload = parse_incoming_message(raw)
            print(f"[RX] client={client} request_id={request_id} payload={payload!r}")

            response = build_response(request_id, payload)
            await websocket.send(response)
            print(f"[TX] client={client} request_id={request_id} response={response!r}")
    except websockets.ConnectionClosed as ex:
        print(f"[INFO] Disconnected: {client} code={ex.code} reason={ex.reason!r}")
    except Exception as ex:
        print(f"[ERROR] Client handler crashed for {client}: {ex}")


def create_ssl_context(cert_path: str, key_path: str) -> ssl.SSLContext:
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certfile=cert_path, keyfile=key_path)
    return context


async def run(host: str, port: int, cert: str | None, key: str | None) -> None:
    ssl_context = None
    scheme = "ws"
    if cert and key:
        ssl_context = create_ssl_context(cert, key)
        scheme = "wss"

    async with websockets.serve(
        handle_client,
        host,
        port,
        ssl=ssl_context,
        ping_interval=20,
        ping_timeout=60,
        max_size=2 * 1024 * 1024,
    ):
        print(f"[INFO] FancyMenu test server listening on {scheme}://{host}:{port}")
        if scheme == "wss":
            print("[INFO] TLS is enabled.")
        else:
            print("[WARN] TLS is disabled (ws://).")
        await asyncio.Future()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="FancyMenu remote WebSocket/WSS test server")
    parser.add_argument("--host", default="127.0.0.1", help="Host/IP to bind (default: 127.0.0.1)")
    parser.add_argument("--port", type=int, default=8765, help="Port to bind (default: 8765)")
    parser.add_argument("--cert", default=None, help="Path to TLS certificate (PEM)")
    parser.add_argument("--key", default=None, help="Path to TLS private key (PEM)")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if (args.cert is None) != (args.key is None):
        raise SystemExit("You must provide both --cert and --key for WSS mode.")
    asyncio.run(run(args.host, args.port, args.cert, args.key))


if __name__ == "__main__":
    main()
