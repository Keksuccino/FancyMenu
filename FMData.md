---
title: FMData
description: Detailed documentation for FancyMenu's FMData system (server -> client, client -> server, listeners, and welcome data).
published: true
date: 2026-03-02T00:00:00.000Z
tags: networking, commands, listeners, automation
editor: markdown
dateCreated: 2026-03-02T00:00:00.000Z
---

# FMData

FMData is FancyMenu's custom data bridge between server and client.

It supports:

1. Server -> Client data sending via `/fmdata send`
2. Client-side reaction via the `On FM Data Received` listener
3. Client -> Server data sending via the `Send FM Data To Server` action
4. Persistent server-side FMData listeners via `/fmdata listener ...`
5. Persistent server-side FMData welcome data via `/fmdata welcome_data ...`

> [!INFO]
> FMData is string-based by design: both identifier and payload are plain strings.
> This makes it easy to integrate with command logic, placeholders, and rich text formats.

# Permissions

The `/fmdata` command requires permission level `2` (operator-level command access).

# Quick Overview

## Data Flow A: Server -> Client

1. A server/admin runs `/fmdata send <target_player> <data_identifier> <string_data>`
2. FancyMenu sends the data to each targeted FancyMenu client
3. Client-side FancyMenu listener `On FM Data Received` fires
4. You can use `$$data_identifier`, `$$data`, `$$sent_by` in listener actions/requirements

## Data Flow B: Client -> Server

1. A client executes FancyMenu action `Send FM Data To Server`
2. FancyMenu sends the data to the server
3. Server receives it and evaluates all configured `/fmdata listener` entries
4. Matching listeners execute one or multiple server commands

## Data Flow C: Server Welcome Data

1. Server admin configures entries with `/fmdata welcome_data add ...`
2. A FancyMenu client joins the server
3. Server evaluates all welcome-data entries for the joining player
4. Matching entries are sent to that joining client as normal FMData

# Server -> Client Sending

## Command

```mcfunction
/fmdata send <target_player> <data_identifier> <string_data>
```

### `<target_player>`

Real built-in Minecraft player selector argument.

Examples:

- `Player761`
- `@a`
- `@p`
- `@s`
- `@a[distance=..50]`

### `<data_identifier>`

`StringArgumentType.string()`:

- single word: `food_update`
- quoted string: `"food value update"`

### `<string_data>`

`StringArgumentType.greedyString()`:

- can contain spaces and rich text content
- can also be wrapped in quotes if preferred

> [!WARNING]
> Delivery is only effective for clients with FancyMenu installed

# Client-Side Receive Listener

## `On FM Data Received` Listener

This listener fires when the client receives FMData from the server.

## Provided Custom Variables

1. `$$data_identifier`
2. `$$data`
3. `$$sent_by`

## Typical Use Cases

1. Update UI text on server events
2. Trigger menu actions from command blocks/functions
3. Build lightweight server -> client state sync for menus

> [!WARNING]
> `On FM Data Received` is client-side FancyMenu logic.  
> It does not execute server commands by itself.

# Client -> Server Sending

## `Send FM Data To Server` Action

The action sends data to the server, which can then listen to this data via **FMData listeners**.

**For this to work:**

1. Server must run FancyMenu
2. Client must run FancyMenu
3. The connection must be fully established

## Server-Side FMData Listeners

Server-side listeners are persistent rules that react to incoming client FMData.

### `/fmdata listener list`

Shows all listeners in formatted chat output with metadata.

### `/fmdata listener add`

Add new listeners via the `add` sub-command.

```mcfunction
/fmdata listener add <unique_listener_name> <matching_type_identifier> <matching_type_data> <ignore_case_identifier> <ignore_case_data> <fire_for_player> <listen_for_identifier> <listen_for_data> <commands_to_execute_on_fire>
```

### `/fmdata listener edit`

Edit existing listeners via the `edit` sub-command.

### `/fmdata listener remove`

Remove existing listeners via the `remove` sub-command.

## Wildcard Matching

For `listen_for_identifier` and `listen_for_data`:

- if value is exactly `*`, it always matches

This wildcard behavior is only for matching these fields.

## Case Control

Two separate booleans:

1. `ignore_case_identifier`
2. `ignore_case_data`

So identifier and payload matching can be configured independently.

## `fire_for_player` Selector

`fire_for_player` uses Minecraft's normal player selector format.

Examples:

- `@a` (any player)
- `@p`
- `Player761`
- `@a[team=blue]`

If you want "all players", use `@a`.

## Executed Commands on Fire

`commands_to_execute_on_fire` is a greedy string.

Multiple commands are separated by:

```text
|||
```

If you need literal `|||` in command text, escape it as:

```text
\|\|\|
```

It will be restored to normal `|||` before execution.

## Listener Command Placeholders

Before command execution:

1. `%fm_sender%` -> replaced with sender player name (`sender.getScoreboardName()`)
2. `%fm_data%` -> replaced with incoming client FMData payload

## Command Execution Context

Listener commands run as server commands, not as client chat commands.

# Welcome Data

Welcome data is persistent server config that sends FMData to joining FancyMenu clients.

Data file:

```text
FancyMenu.MOD_DIR/fmdata_welcome_data.json
```

## Commands

### List

```mcfunction
/fmdata welcome_data list
```

### Add

```mcfunction
/fmdata welcome_data add <unique_welcome_data_name> <target_player> <data_identifier> <string_data>
```

### Edit

```mcfunction
/fmdata welcome_data edit <welcome_data_name> <target_player> <data_identifier> <string_data>
```

### Remove

```mcfunction
/fmdata welcome_data remove <welcome_data_name>
```

`welcome_data_name` has tab-complete suggestions for edit/remove.

## Field Meaning

1. `unique_welcome_data_name`: unique entry key (resource-name style)
2. `target_player`: selector defining who should receive this entry on join
3. `data_identifier`: FMData identifier to send
4. `string_data`: FMData payload to send

## Target Logic

`target_player` is evaluated against the joining player.

Examples:

- `@a` -> every joining FancyMenu client gets this entry
- `Player761` -> only that player gets it
- `@a[team=vip]` -> only VIP team joiners get it

## Join Timing

Welcome data is sent shortly after the player joins.

Flow:

1. Player joins
2. FancyMenu checks all welcome-data entries
3. Matching entries are sent to that player

# Name Validation Rules

These names use resource-name-style validation:

1. `listener_name`
2. `unique_listener_name`
3. `welcome_data_name`
4. `unique_welcome_data_name`

Allowed:

- lowercase letters `a-z`
- numbers `0-9`
- `_`
- `-`

# String and Quoting Notes

## For `StringArgumentType.string()` fields

You can use:

- simple word: `hello`
- quoted phrase: `"hello world"`

## For `StringArgumentType.greedyString()` fields

You can pass rich text with spaces directly.

Quoted full values are also accepted and normalized by command logic (outer quotes removed, escapes handled).

# Practical Examples

## 1) Send from server to all players

```mcfunction
/fmdata send @a "food value update" "{\"food\":18,\"max\":20}"
```

## 2) Add listener that reacts to all players and any identifier

```mcfunction
/fmdata listener add food_sync contains contains false false @a "*" "hunger=" "tellraw %fm_sender% \"Server got hunger value: %fm_data%\""
```

## 3) Add listener with multiple commands

```mcfunction
/fmdata listener add demo equals contains false true @a "event_demo" "start" "say Triggered by %fm_sender% with %fm_data%|||effect give %fm_sender% minecraft:speed 3 1 true"
```

## 4) Add welcome data for all players

```mcfunction
/fmdata welcome_data add default_intro @a "welcome" "{\"text\":\"Welcome to the server!\"}"
```

## 5) Add welcome data only for one player

```mcfunction
/fmdata welcome_data add vip_message Player761 "vip_info" "You have VIP perks enabled."
```

# Troubleshooting

## FMData send reports targets but client does nothing

Check:

1. target really has FancyMenu installed
2. client-side listener `On FM Data Received` is configured
3. your listener logic is attached to the right layout/screen context

## Client action does not reach server listeners

Check:

1. server runs FancyMenu
2. client runs FancyMenu
3. `/fmdata listener list` actually contains matching listener entries
4. matching mode / ignore-case settings are correct
5. `fire_for_player` selector includes sender

## Listener exists but commands do not execute

Check:

1. `commands_to_execute_on_fire` not empty
2. multiple commands correctly separated by `|||`
3. placeholder usage is valid (`%fm_sender%`, `%fm_data%`)
4. resulting command is valid server command syntax

# Final Notes

FMData is intentionally flexible and string-based.  
For robust setups, use clear identifier conventions, keep payload formats consistent (for example JSON-like strings), and split responsibilities:

1. identifier routes logic
2. data carries payload
3. listeners/welcome-data apply server automation
