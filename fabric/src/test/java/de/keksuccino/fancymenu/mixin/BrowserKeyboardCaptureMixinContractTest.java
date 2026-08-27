package de.keksuccino.fancymenu.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserKeyboardCaptureMixinContractTest {

    @Test
    void handledBrowserKeysCancelTheWholeKeyboardCallback() throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        List<Path> mixinSources = new ArrayList<>();
        addIfPresent(mixinSources, repositoryRoot.resolve("fabric/src/main/java/de/keksuccino/fancymenu/mixin/mixins/fabric/client/MixinFabricKeyboardHandler.java"));
        addIfPresent(mixinSources, repositoryRoot.resolve("forge/src/main/java/de/keksuccino/fancymenu/mixin/mixins/forge/client/MixinForgeKeyboardHandler.java"));
        addIfPresent(mixinSources, repositoryRoot.resolve("neoforge/src/main/java/de/keksuccino/fancymenu/mixin/mixins/neoforge/client/MixinNeoForgeKeyboardHandler.java"));
        assertFalse(mixinSources.isEmpty(), "No loader keyboard mixins were found");

        for (Path mixinSource : mixinSources) {
            String source = Files.readString(mixinSource, StandardCharsets.UTF_8);
            String sourceName = repositoryRoot.relativize(mixinSource).toString();
            assertTrue(source.contains("@Inject(method = \"keyPress\""), sourceName + " must intercept the keyboard callback");
            assertTrue(source.contains("cancellable = true"), sourceName + " must be able to stop loader-level raw key hooks");
            assertTrue(source.contains("info.cancel();"), sourceName + " must cancel after a focused browser consumes the key");
            assertFalse(source.contains("@WrapWithCondition(method = \"keyPress\""), sourceName + " must not merely skip the screen call and let raw key hooks run");
        }
    }

    private static void addIfPresent(List<Path> paths, Path candidate) {
        if (Files.isRegularFile(candidate)) paths.add(candidate);
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle")) || Files.isRegularFile(current.resolve("settings.gradle.kts"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate the repository root");
    }

}
