---
title: Schedulers
description: Run FancyMenu action scripts on a timer, even in the background.
---

# Schedulers

Schedulers run an action script on a timer.

They are global (not tied to one specific screen), so they can keep running even when no GUI is open.

Use schedulers when you want automation over time instead of a one-time action.
They are useful for repeated tasks, delayed tasks, and background logic.

Common examples:

- Update variables or text elements every few seconds (for example a custom clock/status display).
- Run periodic checks and trigger actions when conditions are met.
- Start menu effects, sounds, or other scripted behavior on a timed loop.
- Delay an action and run it later without needing a screen to stay open.

# Where To Find Them

Open FancyMenu's **menu bar** while **not** in the layout editor, then **Customization -> Manage Schedulers**.

# Quick Start

1. Open **Customization -> Manage Schedulers**.
2. Click **Add Scheduler**.
3. Build the scheduler's **Action Script** (this is what runs once per scheduler tick).
4. Select the scheduler and click **Edit Settings**.
5. Configure:
   - **Scheduler ID** (unique scheduler name; used by Start/Stop actions and requirements; allowed: `a-z`, `0-9`, `.`, `_`, `-`)
   - **Start Delay (ms)** (wait time before the first tick runs)
   - **Tick Delay (ms)** (wait time between ticks; `0` = every game tick)
   - **Ticks to Run** (how many ticks to run before auto-stop; `0` = permanent)
   - **Start on Launch** (automatically starts this scheduler when FancyMenu loads)
6. Use **Start Now** to run it immediately.
7. Use **Stop Now** to stop it.

# Control And Observe Schedulers

There are actions and requirements to control schedulers and check their running state.

## Actions 

- **Start Scheduler** takes the scheduler's ID and starts it if not already running.
- **Stop Scheduler** also takes the scheduler's ID and stops it.
 
## Requirement

To check if a scheduler is currently running, use the **Scheduler Is Running** requirement, which takes the scheduler's ID.

# Tips

1. Use clear IDs like `hud_update`, `menu_animation`, `music_fade`.
2. Start with a higher tick delay (for example `200`-`1000` ms), then lower it only if needed, to save performance.
3. In the scheduler list, right-click a scheduler to quick-edit its actions.
4. In the scheduler list, double-click a scheduler ID to rename it.