package de.keksuccino.fancymenu.customization.global;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.Util;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

public final class SeamlessWorldLoadingHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File ROOT_DIR = FileUtils.createDirectory(new File(FancyMenu.INSTANCE_DATA_DIR, "seamless_world_loading"));

    @Nullable private static LoadTarget activeTarget;
    @Nullable private static File activeFile;
    @Nullable private static String activeSource;
    @Nullable private static ResourceSupplier<ITexture> activeSupplier;
    @Nullable private static LoadTarget pendingTarget;
    @Nullable private static String pendingIdentifier;

    private SeamlessWorldLoadingHandler() {
    }

    public static void beginWorldLoad(@Nullable String worldSavePath) {
        setActiveTarget(LoadTarget.WORLD, worldSavePath);
    }

    public static void beginServerLoad(@Nullable String serverIp) {
        setActiveTarget(LoadTarget.SERVER, serverIp);
    }

    public static void finishWorldLoad() {
        clearActiveTarget(LoadTarget.WORLD);
    }

    public static void finishServerLoad() {
        clearActiveTarget(LoadTarget.SERVER);
    }

    public static void requestWorldScreenshot(@Nullable String worldSavePath) {
        requestScreenshot(LoadTarget.WORLD, worldSavePath);
    }

    public static void requestServerScreenshot(@Nullable String serverIp) {
        requestScreenshot(LoadTarget.SERVER, serverIp);
    }

    public static boolean renderLoadingBackgroundIfActive(@NotNull GuiGraphics graphics, int x, int y, int width, int height, @Nullable Screen screen) {
        RenderableResource background = getActiveLoadingBackground(screen);
        if (background == null) {
            return false;
        }
        ResourceLocation location = background.getResourceLocation();
        if (location == null) {
            return false;
        }
        int textureWidth = background.getWidth();
        int textureHeight = background.getHeight();
        if (textureWidth <= 0 || textureHeight <= 0 || width <= 0 || height <= 0) {
            return false;
        }

        float scale = Math.max((float) width / (float) textureWidth, (float) height / (float) textureHeight);
        int renderWidth = (int) Math.ceil(textureWidth * scale);
        int renderHeight = (int) Math.ceil(textureHeight * scale);
        if (renderWidth <= 0 || renderHeight <= 0) {
            return false;
        }

        int renderX = x + (width - renderWidth) / 2;
        int renderY = y + (height - renderHeight) / 2;

        RenderSystem.enableBlend();
        RenderingUtils.resetShaderColor(graphics);
        graphics.blit(location, renderX, renderY, 0.0F, 0.0F, renderWidth, renderHeight, textureWidth, textureHeight);
        RenderingUtils.resetShaderColor(graphics);
        RenderSystem.disableBlend();
        return true;
    }

    @Nullable
    private static RenderableResource getActiveLoadingBackground(@Nullable Screen screen) {
        if (!isEnabled()) {
            return null;
        }
        if (activeSupplier == null || activeFile == null) {
            return null;
        }
        if (!isLoadingScreen(screen)) {
            return null;
        }
        ITexture texture = activeSupplier.get();
        if (texture == null || texture.isClosed()) {
            return null;
        }
        return texture;
    }

    private static void setActiveTarget(@NotNull LoadTarget target, @Nullable String identifier) {
        if (!isEnabled()) {
            clearAll();
            return;
        }
        String resolved = sanitizeIdentifier(identifier);
        clearAll();
        if (resolved == null) {
            return;
        }
        File screenshotFile = resolveScreenshotFile(target, resolved);
        if (!screenshotFile.isFile()) {
            return;
        }
        String source = toLocalSource(screenshotFile);
        if (!Objects.equals(source, activeSource)) {
            activeSource = source;
            activeSupplier = ResourceSupplier.image(source);
        }
        activeTarget = target;
        activeFile = screenshotFile;
    }

    private static void clearActiveTarget(@NotNull LoadTarget target) {
        if (activeTarget != target) {
            return;
        }
        clearAll();
    }

    private static void clearAll() {
        activeTarget = null;
        activeFile = null;
        activeSource = null;
        activeSupplier = null;
    }

    public static void cancelPending() {
        pendingTarget = null;
        pendingIdentifier = null;
    }

    private static void requestScreenshot(@NotNull LoadTarget target, @Nullable String identifier) {
        if (!isEnabled()) {
            return;
        }
        String resolved = sanitizeIdentifier(identifier);
        if (resolved == null) {
            return;
        }
        pendingTarget = target;
        pendingIdentifier = resolved;
    }

    public static void capturePendingIfPossible(@NotNull RenderTarget renderTarget) {
        if (!isEnabled()) {
            pendingTarget = null;
            pendingIdentifier = null;
            return;
        }
        if (pendingTarget == null || pendingIdentifier == null) {
            return;
        }
        File outputFile = resolveScreenshotFile(pendingTarget, pendingIdentifier);
        pendingTarget = null;
        pendingIdentifier = null;
        saveScreenshot(renderTarget, outputFile);
    }

    private static void saveScreenshot(@NotNull RenderTarget renderTarget, @NotNull File outputFile) {
        NativeImage image;
        try {
            image = Screenshot.takeScreenshot(renderTarget);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to capture seamless world loading screenshot.", ex);
            return;
        }
        FileUtils.createDirectory(outputFile.getParentFile());
        Util.ioPool().execute(() -> {
            try {
                image.writeToFile(outputFile);
                releaseCachedTexture(outputFile);
            } catch (Exception ex) {
                LOGGER.warn("[FANCYMENU] Failed to save seamless world loading screenshot.", ex);
            } finally {
                image.close();
            }
        });
    }

    private static void releaseCachedTexture(@NotNull File outputFile) {
        try {
            ResourceHandlers.getImageHandler().release(toLocalSource(outputFile), true);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to release cached seamless world loading texture.", ex);
        }
    }

    @Nullable
    private static String sanitizeIdentifier(@Nullable String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isEnabled() {
        return FancyMenu.getOptions().seamlessWorldLoading.getValue();
    }

    private static boolean isLoadingScreen(@Nullable Screen screen) {
        if (screen == null) {
            return false;
        }
        return screen instanceof ConnectScreen
                || screen instanceof LevelLoadingScreen
                || screen instanceof ReceivingLevelScreen
                || screen instanceof GenericWaitingScreen
                || screen instanceof ProgressScreen;
    }

    @NotNull
    private static File resolveScreenshotFile(@NotNull LoadTarget target, @NotNull String identifier) {
        String hash = hashIdentifier(identifier);
        String fileName = (target == LoadTarget.WORLD ? "world_" : "server_") + hash + ".png";
        return new File(ROOT_DIR, fileName);
    }

    @NotNull
    private static String toLocalSource(@NotNull File file) {
        String path = file.getAbsolutePath().replace("\\", "/");
        return ResourceSourceType.LOCAL.getSourcePrefix() + path;
    }

    @NotNull
    private static String hashIdentifier(@NotNull String identifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(identifier.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to hash seamless world loading identifier, falling back to hashCode.", ex);
            return Integer.toHexString(identifier.hashCode());
        }
    }

    private enum LoadTarget {
        WORLD,
        SERVER
    }

}
