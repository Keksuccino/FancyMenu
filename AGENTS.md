# Repository Guidelines

## Project Structure & Module Organization
- This project is "FancyMenu", which is a Minecraft Java 26.2 mod (the version number is not a typo). It uses the MultiLoader layout with shared logic under `common` and loader-specific wrappers under `fabric` and `neoforge`.
- Place shared Java sources in `common/src/main/java` and assets such as menu JSON, translations, or textures in `common/src/main/resources` so they ship with every loader build.
- Loader-only hooks belong inside each module's `src/main/java` tree; keep local run directories like `run_client` and `run_server` for iterative testing but never depend on them for assets.

## Environment
- You are operating on macOS 27 Beta.

## Coding Style & Naming Conventions
- Target Java 25 with 4-space indentation and UTF-8 encoding (WITHOUT BOM), matching the Gradle toolchain configuration.
- Follow existing packages under `de.keksuccino.fancymenu`, mirroring existing sub-packages to keep cross-loader boundaries clear.
- Name resources with the `fancymenu` prefix (e.g., `fancymenu.mixins.json`, `fancymenu.accesswidener`) so Gradle and the loaders resolve them consistently.
- Prefer explicit nullability annotations from `jsr305`.
- Code should be made reusable/shareable whenever possible. Avoid copy-pasting nearly identical code to multiple places when you could make it a shared method/field/etc. instead.
- The whole project (code, classes, packages, etc.) should always be well-structured and organized, with great focus on easy maintainability. The project should be easy to understand and maintain for new devs later.
- Avoid god classes. Split large classes into organized and well-structured smaller classes.
- Avoid spanning method heads and method calls over multiple lines, no matter how long they are. One line per method head and method call.
- Always document fragile parts of the code that could break easily when handled wrong. Explain what they do and what is important for them.
- Always document code that could look a bit hacky, weird, or even useless at first look. Explain what the code does, why it is there, and what is important to note for it.
- Prefer giving every class that needs a logger its own static final LOGGER object, instead of using a global shared logger.

## Mixin Structurization
- Place shared mixins under `common/src/main/java/de/keksuccino/fancymenu/mixin/mixins/common/<side>` and mirror the existing folder depth when adding new targets.
- Declare `@Mixin` classes (and accessor interfaces) with imports grouped at the top, list `@Unique` members before any `@Shadow` declarations, and extend or implement the vanilla type when necessary; supply a suppressed dummy constructor when subclasses require it.
- Suffix every unique field or helper with `_FancyMenu`. Static finals use all caps with `_FANCYMENU`, and injected method names follow the `before/after/on/wrap/cancel_<VanillaMethod>_FancyMenu` pattern. Accessor/invoker methods also end in `_FancyMenu`.
- Cluster related injections together (for example, all `setScreen` hooks in `MixinGui`) and keep helper wrappers private unless a wider contract is required.
- Use short `//` comments for quick reminders and `/** @reason ... */` blocks ahead of injections that change vanilla behavior, matching the authoring tone in existing files.
- FancyMenu has access to Mixin Extras.
- Prefer using features from Mixin Extras instead of using normal Mixin redirects or overrides.
- When leveraging Mixin Extras (`WrapOperation`, `WrapWithCondition`, etc.), name helpers after the intent (`wrap_..._FancyMenu`, `cancel_..._FancyMenu`) and call the provided `Operation` when returning to vanilla flow.
- When crating normal mixin classes, call them `Mixin<OriginalClassName>`, so for the `Minecraft` class that would be `MixinMinecraft`.
- When creating Mixin accessor interfaces, name them `AccessorMixin<OriginalClassName>`, so for the `Minecraft` class that would be `AccessorMixinMinecraft`.
- Keep Mixin classes lightweight.
- Unique methods in Mixin classes go BELOW normal Mixin methods (like injections, wrap operations, etc.).
- Unique fields go BELOW shadow fields in Mixin classes.
- Both unique and shadow fields should always be at the top of the class, before any methods.
- Avoid spanning Mixin annotations over multiple lines, no matter how long they are. Each annotation should only consume one line. One line per annotation.
- A special case for Mixin-related annotations are @Shadow, @Unique, @Final, and @Mutable on fields in Mixin classes. For fields, these should always go on the same line as the field itself, as prefix. Both these annotations and the field itself on a single line.

## Workflow Guidelines
- When the user gives you a log snippet, always search for the full log file containing that snippet, and scan the whole log, so you have a complete picture of what was happening.
- Do not simply implement things without a second thought. Simulate in your reasoning STEP-BY-STEP what each step of the execution chain of the code you implemented does, where it does something, and what could be side effects of it. Chase the whole code execution chain step-by-step, to notice edge cases, incomplete implementations, bugs, etc.
- Always implement everything in the best way possible. Implement everything in the most optimized, performance-friendly, and professional way, following best practices for everything.
- Never rush tasks. It doesn't matter how long a task will take, you always take the best possible route instead of the fastest.
- Everything always needs to be fully compatible with Sodium and Iris. Both mods are set as dependency for the `fabric` module, for testing.
- Everything always needs to work with and without Sodium and Iris.
- Always clean up after yourself! When finishing a task, remove leftover code from testing, code from earlier unsuccessful implementation attempts, and dead code.
- When you work with Vanilla Minecraft code, or Iris/Sodium, always deeply analyze the source code for these, so you really understand what you are working with and how the related code works.
- ALWAYS move most of the actual work to subagents. You just orchestrate your subagents as main agent. You keep and eye on them in case they do something stupid, so you can steer them, or correct their mistakes, if needed. Make sure to move as much work as possible to subagents.

## Mod Conflicts
- When fixing mod conflicts, avoid injections into the other mod. Always try first to fix the issue purely on the project's side, without altering/patching the other mod's code.
- When you discover that the issue actually comes from a bug or bad behavior in the other mod, do not try to patch that bug on the project's side, and instead tell the user that this is a bug/issue in the other mod, and that the other mod should better fix that on their side.
- If possible, always analyze/inspect the actual code of the other mod, to understand the origin of the issue, instead of guessing.
- When inspecting the source of other mods, always prefer the latest available build of the mod for this project's Minecraft version, to be able to understand how the other mod's code works, and to see if the conflict maybe already got fixed on the other mod's side.
- When you need to add compat code for a mod, place these classes in dedicated and well-organized "compat" packages.

## Networking & Packets
- FancyMenu uses its own custom packet system.
- If you need to add packets for a feature, make sure to analyze the `de.keksuccino.fancymenu.networking` package in the `common` module first, to understand how packets get implemented and registered.

## Localization
- Always add en_us localizations for the features you add. Only en_us.
- The en_us.json file is pretty large, too large for you to read the full file, so if you need something from it, search for specific lines.
- ALWAYS add new locals to the END OF THE FILE (without breaking the JSON syntax).
- When you add something to a system that already has localizations available for other parts of the system, first read the existing localizations to understand how the new localizations should get formatted.
- Always read and write en_us.json with an explicit UTF-8-without-BOM encoding.

## Minecraft Sources
- You have access to the full Minecraft 26.2 sources in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/26.2/minecraft/fabric/` and `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/26.2/minecraft/neoforge/`.
- Sources for some libraries used by Minecraft 26.2 are in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/26.2/libraries/`.
- Sources for Sodium, Sodium Extra, and Iris are in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/26.2/libraries/`.
- The following folder also contains sources for various other Minecraft versions, in case it is needed to compare Vanilla Minecraft code for something: `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES`.
- Use the Minecraft sources for research when working with Minecraft-related code.
- Always prefer the sources provided in the `/<mc_version>/libraries/` folder instead of trying to unpack source JARs yourself. Only do that when the provided sources don't contain what you need.

## Autonomous Testing
- After making changes, always compile/build the project to identify and fix compile errors.
- Only use the `fabric` and `neoforge` modules for compile checks. Never use the `common` module.
- Make sure to use Java 25 for compile/run stuff, like this for example: `JAVA_HOME=$(/usr/libexec/java_home -v 25) sh gradlew :fabric:compileJava :neoforge:compileJava --stacktrace`
- There are tools available on the system to validate GLSL shaders. Use these when working with shaders.
- You always TRIPLE-CHECK EVERYTHING! When you are finishing a task, you triple-check everything for completeness, possible bad implementations, rushed implementations, performance, optimization, structurization, and so on.

## Visual Testing
- When the user tells you to also do visual testing, run the `fabric` and `neoforge` modules via IntelliJ IDE.
- Only use "Computer Use" for running the modules! You will click the "Run" button in the top-right of IntelliJ to run the modules (and also select the correct run config before, obviously).
- After the Minecraft client started, use "Computer Use" to navigate in the game and visually check your changes. Check if everything looks good and works as intended.
- IntelliJ IDE is already open with the project active.
- NEVER DO VISUAL TESTING WITHOUT THE USER TELLING YOU TO DO SO! Do not run the game without the user telling you to do so.

## Subagents
- Always spawn ALL your subagents with the gpt-5.6 model on xhigh.
- Always spawn ALL your subagents with a CLEAN context (do not give them your context), so they have a clean context for doing their task in the best possible way.
