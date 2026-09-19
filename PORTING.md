# Minecraft 26.3 preparation

The game identifier is `26.3`; the workspace and branch are named `26.3.0`.
The build uses Java 25, Gradle 9.7.1, Loom 1.18.2, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3, and NeoForge 26.3.0.6-beta with NeoGradle 7.1.39.
Java sources, access wideners, access transformers, and Mixin targets still need
the code port. Updating the build does not establish runtime compatibility.

Konkrete 1.11.1, Melody 1.0.17, and Rinku 3.0.4 use their published 26.3 builds.
Rinku is resolved through Modrinth because the custom Maven repository does not
currently serve the 26.3 coordinates.

Pending optional dependencies, checked on 2026-09-19:

- Fancy Entity Renderer: the existing local 0.5.4 JARs target 26.1.1 and remain
  compile-only. Replace them with a 26.3 release before testing this integration.
- Watermedia and Watermedia Binaries: no published version lists 26.3 support.
  Existing versions remain compile-only, including on Fabric. Once supported
  builds are available, update their properties and restore runtime dependencies
  for integration testing. They are optional for FancyMenu itself.

Refresh the Gradle project in IntelliJ to regenerate module and compiler settings.
Obsolete checked-in mapping paths from older Minecraft workspaces were removed.

Run validation with Java 25:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) sh gradlew :fabric:test :fabric:compileJava :neoforge:compileJava --continue --stacktrace
```

Preparation logs are under the ignored `build/reports/port-preparation/` directory.

Validation on 2026-09-19:

- Gradle configuration, wrapper regeneration, resource processing, and artifact
  resolution passed for both loaders and all declared classpaths.
- Both loaders fail on old rendering packages/classes and GLFW references. The compiler reaches its first 100 diagnostics; this is not a complete code-port inventory.
- The normal `:fabric:test :fabric:compileJava :neoforge:compileJava --continue`
  invocation stops downstream of shared production compilation. A separate
  diagnostic invocation excluded `:common:compileJava` so each loader could
  compile the shared sources directly and expose its own errors. This exclusion
  was only a command-line diagnostic; the build configuration was not weakened.
- Source inventory: 147 test classes containing 913 annotated test methods.
  JUnit did not start: 0 executed, 0 passed, 0 failed, 0 errored, 0 skipped.
  These totals do not indicate passing tests. No game client was launched.

The selected versions were checked against the
[Fabric repositories](https://maven.fabricmc.net/),
[NeoForge repositories](https://maven.neoforged.net/releases/net/neoforged/neoforge/26.3.0.6-beta/),
and [Gradle release metadata](https://services.gradle.org/versions/current).
Minecraft's [26.3 release notes](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3)
specify resource pack version 97.1 and the switch from GLFW to SDL3.
