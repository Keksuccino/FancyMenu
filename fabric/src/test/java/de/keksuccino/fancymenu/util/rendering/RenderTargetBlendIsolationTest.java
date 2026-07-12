package de.keksuccino.fancymenu.util.rendering;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protects the source-level boundary because reproducing the blend-cache desynchronization itself requires a live Minecraft OpenGL render context.
 */
class RenderTargetBlendIsolationTest {

    @Test
    void renderTargetBlitsAreNotGloballyIntercepted() throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        Path mixinConfigPath = repositoryRoot.resolve("common/src/main/resources/fancymenu.mixins.json");
        JsonObject mixinConfig;
        try (Reader reader = Files.newBufferedReader(mixinConfigPath, StandardCharsets.UTF_8)) {
            mixinConfig = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonArray clientMixins = mixinConfig.getAsJsonArray("client");
        assertFalse(clientMixins.asList().stream().anyMatch(element -> element.getAsString().equals("client.MixinRenderTarget")));
    }

    @Test
    void offThreadBlitsReenterTheIsolatedFancyMenuPath() throws IOException {
        Path renderingUtilsPath = findRepositoryRoot().resolve("common/src/main/java/de/keksuccino/fancymenu/util/rendering/RenderingUtils.java");
        String renderingUtilsSource = Files.readString(renderingUtilsPath, StandardCharsets.UTF_8);

        assertTrue(renderingUtilsSource.contains("RenderSystem.recordRenderCall(() -> blitRenderTargetToScreenImmediate(renderTarget));"));
        assertFalse(renderingUtilsSource.contains("renderTarget.blitToScreen(screenWidth, screenHeight, false)"));
        assertFalse(renderingUtilsSource.contains("assumeOpaqueShaderBlendMode"));
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("common/src/main/resources/fancymenu.mixins.json"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate the FancyMenu repository root from the test working directory");
    }

}
