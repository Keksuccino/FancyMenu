# Repository Guidelines

## Project Structure & Module Organization
FancyMenu is a Minecraft 1.21.1 mod that uses the MultiLoader layout with shared logic under `common` and loader-specific wrappers under `fabric`, `forge`, and `neoforge`. Place shared Java sources in `common/src/main/java` and assets such as menu JSON, translations, or textures in `common/src/main/resources` so they ship with every loader build. Loader-only hooks belong inside each module's `src/main/java` tree; keep local run directories like `run_client` and `run_server` for iterative testing but never depend on them for assets.

## Coding Style & Naming Conventions
Target Java 21 with 4-space indentation and UTF-8 encoding (WITHOUT BOM), matching the Gradle toolchain configuration. Follow existing packages under `de.keksuccino.fancymenu`, mirroring existing sub-packages like `customization`, `events`, and `platform` to keep cross-loader boundaries clear. Name resources with the `fancymenu` prefix (e.g., `fancymenu.mixins.json`, `fancymenu.accesswidener`) so Gradle and the loaders resolve them consistently. Prefer explicit nullability annotations from `jsr305`, keep Mixin classes lightweight, and document multi-step flows with concise comments.

## Mixin Structurization
- Place shared mixins under `common/src/main/java/de/keksuccino/fancymenu/mixin/mixins/common/<side>` and mirror the existing folder depth when adding new targets.
- Declare `@Mixin` classes (and accessor interfaces) with imports grouped at the top, list `@Unique` members before any `@Shadow` declarations, and extend or implement the vanilla type when necessary; supply a suppressed dummy constructor when subclasses require it.
- Suffix every unique field or helper with `_FancyMenu`. Static finals use all caps with `_FANCYMENU`, and injected method names follow the `before/after/on/wrap/cancel_<VanillaMethod>_FancyMenu` pattern. Accessor/invoker methods also end in `_FancyMenu`.
- Cluster related injections together (for example, all `setScreen` hooks in `MixinMinecraft`) and keep helper wrappers private unless a wider contract is required.
- Use short `//` comments for quick reminders and `/** @reason ... */` blocks ahead of injections that change vanilla behaviour, matching the authoring tone in existing files.
- FancyMenu has access to Mixin Extras.
- Prefer using features from Mixin Extras instead of using normal Mixin redirects or overrides.
- When leveraging Mixin Extras (`WrapOperation`, `WrapWithCondition`, etc.), name helpers after the intent (`wrap_..._FancyMenu`, `cancel_..._FancyMenu`) and call the provided `Operation` when returning to vanilla flow.

## Localization
- Always add en_us localizations for the features you add. Only en_us.
- The en_us.json file is pretty large, too large for you to read the full file, so if you need something from it, search for specific lines.
- ALWAYS add new locals to the END OF THE FILE (without breaking the JSON syntax).
- When you add something to a system that already has localizations available for other parts of the system, first read the existing localizations to understand how the new localizations should get formatted.
- Always read and write en_us.json with an explicit UTF-8-without-BOM encoding
  - Never use plain Get-Content/Set-Content (or Add-Content) on this file; Windows PowerShell 5.x will mis-handle BOM-less UTF-8.
  - After editing, quickly sanity-check that sequences like `§z` still look correct (no stray `Â` characters) before proceeding.

## Networking & Packets
FancyMenu uses its own custom packet system. If you need to add packets for a feature, make sure to analyze the `de.keksuccino.fancymenu.networking` package in the `common` module first, to understand how packets get implemented and registered.

## Minecraft Sources
You have access to the full Minecraft 1.21.1 sources in the `minecraft_cached_sources` folder. The folder contains source sets for Fabric (`fabric`), Forge (`forge`) and NeoForge (`neoforge`). Before starting a task, make sure to read sources you could need for the task, so you know how the current Minecraft code actually looks. Always do that, knowing how the actual Minecraft code looks is very important, especially when you work with mixins.
Make sure to always compare Vanilla classes from all 3 modloaders (Fabric, Forge, NeoForge), since Forge and NeoForge often alter Vanilla classes, so mixins can't always get applied in `common` and instead need to get implemented for every launcher if the point to place the mixin differs between modloaders.

## Writing to Files
When writing files (edit or create), ALWAYS use UTF-8 (WITHOUT BOM) encoding! NEVER use any other encodings, such as UTF-8-BOM, etc.
When you use PowerShell commands, generate PowerShell 5 compatible code to save files in UTF-8 without BOM, using System.Text.UTF8Encoding(false).

## Git & Run/Compile
NEVER try to run git commands or try to run/compile the project!