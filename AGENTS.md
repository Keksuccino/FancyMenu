# Repository Guidelines

## Project Structure & Module Organization
- FancyMenu is a Minecraft Java 1.20.1 mod that uses the MultiLoader layout with shared logic under `common` and loader-specific wrappers under `fabric` and `forge`.
- Place shared Java sources in `common/src/main/java` and assets such as menu JSON, translations, or textures in `common/src/main/resources` so they ship with every loader build.
- Loader-only hooks belong inside each module's `src/main/java` tree; keep local run directories like `run_client` and `run_server` for iterative testing but never depend on them for assets.

## Environment
- You are running inside WSL on a Windows machine.

## General Guidelines and Reminders
- Always keep in mind that you are an AI. Time does not work the same for you as it does for humans.
- What would take humans weeks or more to implement/write will take you only some minutes, so you should never take the "faster" route just to get the job done quicker. Take the BEST route, even if it takes a bit longer.

## Coding Style
- Target Java 17 with 4-space indentation and UTF-8 encoding (WITHOUT BOM), matching the Gradle toolchain configuration.
- Follow existing packages under `de.keksuccino.fancymenu`, mirroring existing sub-packages like `customization`, `events`, and `platform` to keep cross-loader boundaries clear.
- Name resources with the `fancymenu` prefix (e.g., `fancymenu.mixins.json`, `fancymenu.accesswidener`) so Gradle and the loaders resolve them consistently.
- Prefer explicit nullability annotations from `jsr305`.
- Always use proper class imports. Don't call classes by their full classpath in code, if not needed.

## Mixin Structurization
- Place shared mixins under `common/src/main/java/de/keksuccino/fancymenu/mixin/mixins/common/<side>` and mirror the existing folder depth when adding new targets.
- List `@Unique` members before any `@Shadow` declarations.
- Extend or implement the vanilla type when necessary; supply a suppressed dummy constructor when subclasses require it.
- Suffix every unique field or helper with `_FancyMenu`. Static finals use all caps with `_FANCYMENU`, and injected method names follow the `before/after/on/wrap/cancel_<VanillaMethod>_FancyMenu` pattern. Accessor/invoker methods also end in `_FancyMenu`.
- Cluster related injections together (for example, all `setScreen` hooks in `MixinMinecraft`) and keep helper wrappers private unless a wider contract is required.
- FancyMenu has access to Mixin Extras.
- Prefer using features from Mixin Extras instead of using normal Mixin redirects or overrides.
- When leveraging Mixin Extras (`WrapOperation`, `WrapWithCondition`, etc.), name helpers after the intent (`wrap_..._FancyMenu`, `cancel_..._FancyMenu`) and call the provided `Operation` when returning to vanilla flow.
- Keep Mixin classes lightweight.
- Try to always describe what a Mixin does in its javadoc via `@reason ...`, but keep in mind `@reason` only works on methods, not class heads.
- Always leave `require` at default for mixins! Never do `require = 0` or similar, unless the user tells you to do it.

## Localization
- Always add en_us localizations for the features you add. Only en_us.
- The en_us.json file is pretty large, too large for you to read the full file, so if you need something from it, search for specific lines.
- ALWAYS add new locals to the END OF THE FILE (without breaking the JSON syntax).
- When you add something to a system that already has localizations available for other parts of the system, first read the existing localizations to understand how the new localizations should get formatted.
- Always read and write en_us.json with an explicit UTF-8-without-BOM encoding.

## Networking & Packets
- FancyMenu uses its own custom packet system.
- If you need to add packets for a feature, make sure to analyze the `de.keksuccino.fancymenu.networking` package in the `common` module first, to understand how packets get implemented and registered.

## Minecraft & Library Sources
- There is a directory with full Minecraft sources of all common versions for you to look up things when working with Minecraft code.
- The sources directory is located at `E:\CODING\WORKSPACES\IntelliJ\Minecraft Mods\.MINECRAFT_SOURCES`.
- The top level of the sources directory has subdirectories for each Minecraft version, such as `1.19.2`, `1.20.1`, `1.21.1`, `1.21.11`, `26.1.1`, etc.
- In each Minecraft version subdirectory is a `minecraft` directory and a `libraries` directory.
- The `minecraft` directory has subdirectories for the different launchers, so you can compare launcher-specific changes to Minecraft code, such as `fabric`, `forge`, `neoforge`, etc., and inside each is the Minecraft source code.
- The `libraries` directory contains the source code of some important libraries used by Minecraft, such as the source code for LWJGL.
- Make sure to check the Minecraft source code when working with Minecraft code, instead of guessing.

## Porting
- When porting code from other Minecraft versions to this project, make sure to preserve 100% of the code's original function.
- Do not dumb down or straight up remove parts of the code just because it's difficult to port. Take all the time that's needed to properly port the code and make it work in this project.
- Preserve optimizations of the original code, to not make the port less optimized, less performance-friendly, etc.
- When porting code from a different Minecraft version, you should compare both Minecraft version's code to see what changed, how it works here and how to best port the code.

## Gradle
- Do NOT directly run or compile the project via Gradle, except the user explicitly tells you to do so. Using IntelliJ run configurations does not count here and is allowed.

## Testing
- To check if the project compiles and to look for errors/issues in general, use the `get_run_configurations` and `execute_run_configuration` tools to get and execute the Fabric and Forge client run configurations inside the open IntelliJ IDE.
- Never try to run the `common` module directly, because it is only the base for the `fabric` and `forge` ones.
- When running a "run configuration", always set a timeout of 80 seconds by default, and only if that's not enough after trying the first time, add 30 more seconds and so on.
- Do not treat "the game is not crashing" as "everything works". When the client successfully launched in testing, check its log files for remaining errors and other problems/issues.
- You are allowed to add temporary testing code and classes to the project, to be able to better analyze and test specific parts. This can be simple debug logging (always use the INFO level), but it can also be code for you to automatically open menus, launch things, or whatever you need for testing. Just make sure to remove the testing code after.
- You can take screenshots by using OS-level tools/utils, if needed.
- Manually kill/close lingering game processes from running "run configurations". They do not automatically close, not even on timeout. They only close automatically when crashing (obviously).
