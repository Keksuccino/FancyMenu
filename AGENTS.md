# Repository Guidelines

## Project Structure & Module Organization
- This workspace is giving you access to multiple "FancyMenu" branches/workspaces, which is a Minecraft Java mod. It uses the MultiLoader layout with shared logic under `common` and loader-specific wrappers under `fabric`, and `forge` or `neoforge` depending on the Minecraft version.
- Place shared Java sources in `common/src/main/java` and assets such as menu JSON, translations, or textures in `common/src/main/resources` so they ship with every loader build.
- Loader-only hooks belong inside each module's `src/main/java` tree; keep local run directories like `run_client` and `run_server` for iterative testing but never depend on them for assets.
- FancyMenu's Gradle projects have an unconventional handling of the `version` variable. In FancyMenu's Gradle projects, this variable is always `1.0.0`, which is intentional and should NEVER be changed.
  - The actual mod version is instead defined in `mod_version`.
  - It is also intentional that some stuff in the project uses the `version` variable.

## Environment
- You are operating on macOS 27 Beta.

## Sub-Workspaces
- This primary workspace here is the so-called "agent root", but it only exists as an access point to all sub-workspaces/secondary workspaces you also have access to.
- This workspace gives you access to the actual FancyMenu source workspaces for multiple branches/Minecraft versions.
- The folder name of these should always be something like `fancymenu-0.0.0`, with the `0.0.0` replaced with the Minecraft version the workspace is targeting.
- You should avoid writing permanent data to this "root" workspace here. Temporary working data is okay, but clear it up at the end, since your actual work targets will always be the sub-workspaces, where you should actually make changes to/write to.

## Coding Style & Naming Conventions
- Target the correct Java version for each sub-workspace with 4-space indentation and UTF-8 encoding (WITHOUT BOM), matching the Gradle toolchain configuration.
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
- The code base between all sub-workspaces should always look as similar/identical as possible, to easily find a specific piece of logic in multiple sub-workspaces.

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
- You can't nest classes or interfaces in Mixin classes. You need to place them outside Mixin classes.
- You can't place non-Mixin classes/interfaces in packages declared as "Mixin packages". You need to place them outside these packages.

## Workflow Guidelines
- When the user gives you a log snippet, always search for the full log file containing that snippet, and scan the whole log, so you have a complete picture of what was happening.
- Do not simply implement things without a second thought. Simulate in your reasoning STEP-BY-STEP what each step of the execution chain of the code you implemented does, where it does something, and what could be side effects of it. Chase the whole code execution chain step-by-step, to notice edge cases, incomplete implementations, bugs, etc.
- Always implement everything in the best way possible. Implement everything in the most optimized, performance-friendly, and professional way, following best practices for everything.
- Never rush tasks. It doesn't matter how long a task will take, you always take the best possible route instead of the fastest.
- Everything always needs to be compatible with Sodium and Iris. To check compatibility for these, scan the Sodium and Iris sources in the places you touch, to check if Sodium/Iris touch it too, and then see if both works well together.
- Always clean up after yourself! When finishing a task, remove leftover code from testing, code from earlier unsuccessful implementation attempts, and dead code.
- When you work with Vanilla Minecraft code, or Iris/Sodium, always deeply analyze the source code for these, so you really understand what you are working with and how the related code works.

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
- You have access to full sources of Minecraft, and even some libraries used by Minecraft and FancyMenu, in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES`.
- There are always sources for each relevant loader for a Minecraft version, like this: `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/<minecraft_version>/minecraft/<loader_name>/`, with `<minecraft_version>` replaced with the target MC version (e.g. `26.2`, `1.21.11`, and so on), and `<loader_name>` replaced with either `fabric`, `forge`, or `neoforge`.
- Sources for some libraries used by Minecraft and FancyMenu are in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/<minecraft_version>/libraries/`.
- Sources for Sodium, Sodium Extra, and Iris are in `/Volumes/STUFF/CODING/WORKSPACES/Java/Minecraft Mods/.MINECRAFT_SOURCES/<minecraft_version>/libraries/`.
- Use the Minecraft sources for research when working with Minecraft-related code.
- Always prefer the sources provided in the `/<minecraft_version>/libraries/` folder instead of trying to unpack source JARs yourself. Only do that when the provided sources don't contain what you need.

## New Minecraft Versioning Scheme
- Minecraft Java recently changed their versioning scheme from `1.X.X` to `RELEASE_YEAR.X.X`, so starting with 2026 releases, Minecraft Java versions will be named `26.X.X`, then in 2027 it will be `27.X.X`, and so on.

## Autonomous Testing
- After making changes, always compile/build the project to identify and fix compile errors.
- Only use the `fabric` and `neoforge` modules for compile checks. Never use the `common` module.
- Make sure to use the correct Java version for the specific sub-workspace for compile/run stuff, like this for example: `JAVA_HOME=$(/usr/libexec/java_home -v <correct_java_version_for_sub_workspace>) sh gradlew :fabric:compileJava :neoforge:compileJava --stacktrace`
- Add focused JUnit 5 regression tests for every bug fix or behavior change that can be tested automatically. The tests should fail for the broken behavior and cover the main path plus relevant boundary, failure, and lifecycle cases.
- Place shared and Fabric test classes under `fabric/src/test/java`, mirroring the production package and naming each class `<Subject>Test`. Treat each test class as a focused suite for one coherent subject; split unrelated behavior into separate classes.
- Do not duplicate shared tests in the `neoforge`/`forge` module. (Neo)Forge's test task is disabled, so run tests through `fabric` and verify production compatibility by compiling both loaders.
- Keep tests deterministic, isolated, and Minecraft-light. Prefer small reusable or package-private helpers/controllers for logic that cannot safely instantiate Minecraft runtime objects, without weakening or distorting the production design just for testing.
- Inject clocks, executors, suppliers, and other changing inputs when needed. Use temporary directories, loopback servers, and fakes instead of real user files, external services, arbitrary sleeps, or test-order dependencies.
- Run the focused suite first, for example: `JAVA_HOME=$(/usr/libexec/java_home -v <correct_java_version_for_sub_workspace>) sh gradlew :fabric:test --tests 'fully.qualified.SubjectTest' --stacktrace`
- Before finishing, run the complete Fabric suite with the correct Java version using `JAVA_HOME=$(/usr/libexec/java_home -v <correct_java_version_for_sub_workspace>) sh gradlew :fabric:test --stacktrace`, then compile Fabric and (Neo)Forge. Use `--rerun-tasks` for the final test run when cached results could hide whether the current tree was executed.
- Report the number of discovered suites/tests and the passed, failed, errored, and skipped totals.
- A successful compile or an up-to-date Gradle task does not replace an executed regression test.
- After compiling and normal tests succeeded, run the client of both `fabric` and `neoforge`/`forge` modules via their client launch tasks, and check the log output, to see if stuff like Mixin injects succeeded, but make sure to CLOSE THE CLIENT WINDOWS after. Only run clients when it makes sense to run them, which means when compiling is not enough to catch all potential issues.
- Never try to control the client, for example with "Computer Use".
- You can add temporary testing code to the mod that executes on client launch or when it hits the Title screen or something, for getting feedback from the game process directly, for things like shader testing and other stuff you need the actual Minecraft process for. Make sure to remove that testing code after.
- There are tools available on the system to validate GLSL shaders. Use these when working with shaders.
- You always TRIPLE-CHECK EVERYTHING! When you are finishing a task, you triple-check everything for completeness, possible bad implementations, rushed implementations, performance, optimization, structurization, and so on.

