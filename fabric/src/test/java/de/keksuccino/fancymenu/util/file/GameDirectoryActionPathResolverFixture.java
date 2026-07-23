package de.keksuccino.fancymenu.util.file;

import java.io.IOException;
import java.nio.file.Path;

public final class GameDirectoryActionPathResolverFixture {

    private GameDirectoryActionPathResolverFixture() {
    }

    public static GameDirectoryActionPathResolver create(Path gameDirectoryRoot, Path minecraftDirectoryRoot) throws IOException {
        return GameDirectoryActionPathResolver.create(gameDirectoryRoot, minecraftDirectoryRoot);
    }
}
