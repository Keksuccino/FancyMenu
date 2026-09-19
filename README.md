
# About

This is the repository for FancyMenu v3.

# Copyright

FancyMenu © Copyright 2020-2025 Keksuccino.<br>
FancyMenu is licenced under DSMSLv3.<br>
For more information about the license, please check `LICENSE.md`.

Japng © Copyright A. Ellerton.<br>
Japng is licensed under Apache-2.0.

[Animated GIF library for Java](https://github.com/rtyley/animated-gif-lib-for-java) © Copyright Roberto Tyley, Kevin Weiner.<br>
Original code by Kevin Weiner on [fmsware.com](http://www.fmsware.com/stuff/gif.html).<br>
Re-packaged and improved by Roberto Tyley. Improvements licensed under the Apache-2.0 license.

<br>

-------------

<br>

# Development for Minecraft 26.3

This branch uses the MultiLoader layout: shared code and resources are in
`common`, with loader-specific integrations in `fabric` and `neoforge`.
See [PORTING.md](PORTING.md) for the prepared toolchain, dependency availability,
and work still required before running the game.

Import the root Gradle project in IntelliJ IDEA, select Java 25 for both the
project SDK and Gradle JVM, and use the checked-in Gradle wrapper. Refresh the
Gradle project after switching to this workspace.

Build and test through the loader modules:

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 25) sh gradlew :fabric:test :fabric:build :neoforge:build
```

After the code port is complete, launch development clients with
`:fabric:runClient` or `:neoforge:runClient`. Loader-specific code belongs in its
loader module; code shared by both loaders belongs in `common`.
