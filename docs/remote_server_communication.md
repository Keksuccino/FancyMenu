---
title: Remote Server Communication
description: Send and receive custom text data between FancyMenu clients and external servers.
---

# Remote Server Communication

The "Remote Server Communication" system lets FancyMenu clients communicate with external servers using WebSocket connections.

All data is text-based:

- Plain text is supported
- JSON is supported (as normal text)

Each server URL gets one cached **request ID** during runtime.  
FancyMenu uses this ID to track the connection and expose it in listener variables.

# Quick Start

1. Add action **Connect To Remote Server** (optional, but useful to open early)
2. Add action **Send Data To Remote Server** with the same URL
3. Add listener **On Remote Server Data Received** to react to replies
4. Use **On Remote Server Connected** / **On Remote Server Connection Closed** for connection-state logic
5. Close connections when needed with close actions

# Actions

## Connect To Remote Server

Initializes a remote server connection without sending payload data.

Input:

- Remote Server URL

## Send Data To Remote Server

Connects (or reuses an existing connection) and sends text data.

Inputs:

1. Remote Server URL
2. Data

## Close Remote Server Connection

Closes one connection by request ID.

Input:

- Connection Request ID

## Close All Remote Server Connections

Closes all currently active remote server connections.

# Listeners

## On Remote Server Connected

Triggers when a remote server connection gets initialized.

Variables:

- `$$request_id`
- `$$remote_server_url`

## On Remote Server Data Received

Triggers when data is received from a connected remote server.

Variables:

- `$$request_id`
- `$$remote_server_url`
- `$$data`

## On Remote Server Connection Closed

Triggers when a remote server connection closes.

Variables:

- `$$request_id`
- `$$remote_server_url`
- `$$intentionally_closed`
- `$$crashed`
- `$$unknown_close_reason`

# Connection Behavior

- Connections are **client-initiated**
- FancyMenu keeps connections active in the background
- If a connection crashes or times out, FancyMenu retries every 10 seconds
- When a crashed connection is restored, FancyMenu logs a restore message
- Outgoing unsent messages are queued with a **max age of 30 seconds**
- Queued messages older than 30 seconds are dropped

# URL Modes

- `wss://` = secure (TLS), recommended
- `ws://` = unencrypted, useful for local testing

Example local URL:

- `ws://127.0.0.1:8765`

# Best Practices

1. Use one stable URL per backend service.
2. Keep payload format consistent for each use case.
3. Handle closed/crashed connections with fallback UI logic.
4. Use close actions when your flow is finished.
5. Use `wss://` for production setups.
