# Repository Guidelines

## Project Description and Structure
- This project is "FancyMenu", which is a Minecraft Java 1.20.1 mod that uses the MultiLoader layout with shared logic under `common` and loader-specific wrappers under `fabric` and `neoforge`.
- Place shared Java sources in `common/src/main/java`, and assets such as menu JSON, translations, or textures in `common/src/main/resources`, so they ship with every loader build.
- Loader-only hooks belong inside each module's `/src/main/java` tree.

## Environment
- You are operating on macOS 27 Beta.

## General Guidelines and Reminders
- Always keep in mind that you are an AI. Time does not work the same for you as it does for humans.
- What would take humans weeks or more to implement/write will take you only some minutes, so you should never take the "faster" route just to get the job done quicker. Take the BEST route, even if it takes longer.

## Missing Promised Information
- If you are unable to locate promised files, or you miss promised information in general, end/stop your turn and tell the user about the missing information. Do not try to work around the missing information/files.

## Coding Style & Naming Conventions
- Target Java 21 with 4-space indentation and UTF-8 encoding (WITHOUT BOM), matching the Gradle toolchain configuration.
- Follow existing packages under `de.keksuccino.fancymenu`, mirroring existing sub-packages to keep cross-loader boundaries clear.
- Name resources with the `fancymenu` prefix (e.g., `fancymenu.mixins.json`, `fancymenu.accesswidener`) so Gradle and the loaders resolve them consistently.
- Prefer explicit nullability annotations from `jsr305`.
- Keep Mixin classes lightweight.
- Always use proper class imports. Don't call classes by their full classpath in code, if not needed.

## Mixin Structurization
- Place shared mixins under `common/src/main/java/de/keksuccino/fancymenu/mixin/mixins/common/<side>` and mirror the existing folder depth when adding new targets.
- Declare `@Mixin` classes (and accessor interfaces) with imports grouped at the top, list `@Unique` members before any `@Shadow` declarations, and extend or implement the vanilla type when necessary; supply a suppressed dummy constructor when subclasses require it.
- Suffix every unique field or helper with `_FancyMenu`. Static finals use all caps with `_FANCYMENU`, and injected method names follow the `before/after/on/wrap/cancel_<VanillaMethod>_FancyMenu` pattern. Accessor/invoker methods also end in `_FancyMenu`.
- Cluster related injections together (for example, all `setScreen` hooks in `MixinMinecraft`) and keep helper wrappers private unless a wider contract is required.
- Use short `//` comments for quick reminders and `/** @reason ... */` blocks ahead of injections that change vanilla behavior, matching the authoring tone in existing files.
- FancyMenu has access to Mixin Extras.
- Prefer using features from Mixin Extras instead of using normal Mixin redirects or overrides.
- When leveraging Mixin Extras (`WrapOperation`, `WrapWithCondition`, etc.), name helpers after the intent (`wrap_..._FancyMenu`, `cancel_..._FancyMenu`) and call the provided `Operation` when returning to vanilla flow.

## Localization
- Always add en_us localizations for the features you add. Only en_us.
- The en_us.json file is pretty large, too large for you to read the full file, so if you need something from it, search for specific lines.
- ALWAYS add new locals to the END OF THE FILE (without breaking the JSON syntax).
- When you add something to a system that already has localizations available for other parts of the system, first read the existing localizations to understand how the new localizations should get formatted.
- Always read and write en_us.json with an explicit UTF-8-without-BOM encoding.

## Networking & Packets
- FancyMenu uses its own custom packet system.
- If you need to add packets for a feature, make sure to analyze the `de.keksuccino.fancymenu.networking` package in the `common` module first, to understand how packets get implemented and registered.

## Minecraft Sources
- You have access to the full Minecraft 1.20.1 sources in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/1.20.1/minecraft/fabric/` and `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/1.20.1/minecraft/neoforge/`.
- Sources for some libraries used by Minecraft 1.20.1 are in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/1.20.1/libraries/`.
- You have access to the full Minecraft 1.19.2 sources in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/1.19.2/minecraft/fabric/` and `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/1.19.2/minecraft/neoforge/`.
- Sources for some libraries used by Minecraft 1.19.2 are in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/1.19.2/libraries/`.
- Use the Minecraft sources for research when working with Minecraft-related code.
- Always prefer the sources provided in the `/<mc_version>/libraries/` folder instead of trying to unpack source JARs yourself. Only do that when the provided sources don't contain what you need.
- Minecraft 1.19.2 is the version before Minecraft 1.20.1.

## Autonomous Testing
- After making changes, always compile/build the project to identify and fix compile errors.
- Only use the `fabric` and `forge`/`neoforge` modules for compile checks. Never use the `common` module.
- Make sure to use Java 21 for compile/run stuff, like this for example: `JAVA_HOME=$(/usr/libexec/java_home -v 21) sh gradlew :fabric:compileJava :neoforge:compileJava --stacktrace`

## Visual Testing
- When the user tells you to also do visual testing, run the `fabric` and `neoforge` modules via IntelliJ IDE.
- Only use "Computer Use" for running the modules! You will click the "Run" button in the top-right of IntelliJ to run the modules (and also select the correct run config before, obviously).
- After the Minecraft client started, use "Computer Use" to navigate in the game and visually check your changes. Check if everything looks good and works as intended.
- IntelliJ IDE is already open with the project active.
