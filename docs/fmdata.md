---
title: Client <-> Server Data Sharing
description: Send and receive custom data between server and client with FancyMenu.
---

# FM Data

The "FM Data" system lets you send custom text data between server and client.

Every FM Data message has:

1. A **data identifier** (what kind of message this is)
2. A **data value** (the actual content)

Example idea:

- Identifier: `hud.food`
- Data: `18/20`

# Quick Start

1. Server sends data with `/fmdata send ...`
2. Client receives it with the FancyMenu listener **On FM Data Received**
3. Client can also send data back with the action **Send FM Data To Server**
4. Server can react automatically with `/fmdata listener ...`
5. Server can auto-send data on join with `/fmdata welcome_data ...`

# Server -> Client

Use:

```mcfunction
/fmdata send <target_player> <data_identifier> <string_data>
```

Examples:

```mcfunction
/fmdata send Player761 hud.food 18/20
/fmdata send @a "food value update" "18 of 20"
```

Notes:

- `<target_player>` supports normal player selectors like `@a`, `@p`, `@s`
- Use quotes for values with spaces

# Client: Receive Data

Use FancyMenu listener:

- **On FM Data Received**

Available variables:

- `$$data_identifier`
- `$$data`
- `$$sent_by`

`$$sent_by` is:

- Server IP in multiplayer
- `integrated_server` in singleplayer

Common use cases:

- Update text elements
- Trigger menu actions
- Run logic based on incoming identifier/data

# Client -> Server

Use FancyMenu action:

- **Send FM Data To Server**

The action has 2 inputs:

1. Data Identifier
2. Data

The server can then process incoming data with `/fmdata listener ...`.

# Server Listeners

Server listeners listen for incoming data from clients and can run one or multiple commands when they get triggered.

Server listeners are saved and stay active after restart.

Manage them with:

- `/fmdata listener list`
- `/fmdata listener add ...`
- `/fmdata listener edit ...`
- `/fmdata listener remove ...`

## Add / Edit Syntax

```mcfunction
/fmdata listener add <unique_listener_name> <matching_type_identifier> <matching_type_data> <ignore_case_identifier> <ignore_case_data> <fire_for_player> <listen_for_identifier> <listen_for_data> <commands_to_execute_on_fire>
```

```mcfunction
/fmdata listener edit <listener_name> <matching_type_identifier> <matching_type_data> <ignore_case_identifier> <ignore_case_data> <fire_for_player> <listen_for_identifier> <listen_for_data> <commands_to_execute_on_fire>
```

## Remove Syntax

```mcfunction
/fmdata listener remove <listener_name>
```

## Matching Types

`matching_type_identifier` and `matching_type_data` can be:

- `equals`
- `contains`
- `starts_with`
- `ends_with`

## Matching Rules

- `ignore_case_identifier` and `ignore_case_data` are true/false toggles
- `listen_for_identifier` supports wildcard `*` (always matches)
- `listen_for_data` supports wildcard `*` (always matches)
- `fire_for_player` uses normal player selectors (for example `@a`, `@p`, `Player761`)

## Commands On Fire

`commands_to_execute_on_fire` is one text input.

- Separate multiple commands with `|||`
- Escape a literal separator as `\|\|\|`

You can use two special placeholders here that get replaced right before the commands get executed:

- `%fm_sender%` -> player who sent the FM Data
- `%fm_data%` -> data value received from client

Commands run as server commands.

## Example Commands

React to a button press from any player:

```mcfunction
/fmdata listener add button_ping equals equals false false @a ui.button pressed "tellraw @a {\"text\":\"%fm_sender% pressed the button\"}"
```

Run multiple commands when data contains `gold`:

```mcfunction
/fmdata listener add reward equals contains false true @a reward "gold" "say Reward from %fm_sender%: %fm_data%|||effect give %fm_sender% minecraft:speed 3 1 true"
```

# Welcome Data

Welcome data sends FM Data to matching players when they join.

Manage entries with:

- `/fmdata welcome_data list`
- `/fmdata welcome_data add ...`
- `/fmdata welcome_data edit ...`
- `/fmdata welcome_data remove ...`

## Add / Edit Syntax

```mcfunction
/fmdata welcome_data add <unique_welcome_data_name> <target_player> <data_identifier> <string_data>
```

```mcfunction
/fmdata welcome_data edit <welcome_data_name> <target_player> <data_identifier> <string_data>
```

## Remove Syntax

```mcfunction
/fmdata welcome_data remove <welcome_data_name>
```

Notes:

- `<target_player>` supports normal selectors like `@a`, `@p`, `@s`
- Data is sent to matching players when they join
- Entries are saved and loaded automatically

## Example Commands

Send welcome data to all joining players:

```mcfunction
/fmdata welcome_data add welcome_all @a hud.welcome "Welcome!"
```

Send welcome data only to one player:

```mcfunction
/fmdata welcome_data add welcome_vip Player761 hud.vip "VIP perks enabled"
```

# Best Practices

1. Use clear identifiers like `hud.food`, `menu.shop.open`, `quest.progress`.
2. Keep data format consistent for each identifier.
3. Start simple: test with `/fmdata send` before building complex listeners.
4. Use `@a` only when you really want global behavior.
5. Use `/fmdata listener list` and `/fmdata welcome_data list` to keep configs clean.
