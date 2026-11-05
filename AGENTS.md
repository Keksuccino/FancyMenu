# Repository Guidelines

- This workspace is giving you access to multiple "FancyMenu" branches/workspaces, which is a Minecraft Java mod.

## Sub-Workspaces

- This primary workspace here is the so-called "agent root", but it only exists as an access point to all sub-workspaces/secondary workspaces you also have access to.
- This workspace gives you access to the actual FancyMenu source workspaces for multiple branches/Minecraft versions.
- The folder name of these should always be something like `modname-0.0.0`, with the `0.0.0` replaced with the Minecraft version the workspace is targeting, and `modname` replaced with the actual mod name.
- You should avoid writing permanent data to this "root" workspace here. Temporary working data is okay, but clear it up at the end, since your actual work targets will always be the sub-workspaces, where you should actually make changes to/write to.

## Coding Guidelines

- Prefer giving every class that needs a logger its own static final LOGGER object, instead of using a global shared logger.
- The code base between all sub-workspaces should always look as similar/identical as possible, to easily find a specific piece of logic in multiple sub-workspaces.

## Workflow Guidelines

- When you are told to implement or fix something, without clarifying in which sub-workspace, it means you should implement/fix it in all sub-workspaces. For fixes, apply the fix to all sub-workspaces that need the fix.

## Networking & Packets

- FancyMenu uses its own custom packet system.
- If you need to add packets for a feature, make sure to analyze the `de.keksuccino.fancymenu.networking` package in the `common` module first, to understand how packets get implemented and registered.
