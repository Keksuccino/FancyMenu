package de.keksuccino.fancymenu.customization.global;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
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
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

public final class SeamlessWorldLoadingHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final File ROOT_DIR = FileUtils.createDirectory(new File(FancyMenu.INSTANCE_DATA_DIR, "seamless_world_loading"));
    private static final int RECENT_PRELOAD_LIMIT = 5;

    @Nullable private static LoadTarget activeTarget;
    @Nullable private static File activeFile;
    @Nullable private static String activeSource;
    @Nullable private static ResourceSupplier<ITexture> activeSupplier;
    @Nullable private static File preWarmedWorldFile;
    @Nullable private static String preWarmedWorldSource;
    @Nullable private static ResourceSupplier<ITexture> preWarmedWorldSupplier;
    @Nullable private static LoadTarget captureTarget;
    @Nullable private static String captureIdentifier;
    @Nullable private static NativeImage lastCapturedImage;
    private static long lastCaptureTimeMs;
    private static final long CAPTURE_INTERVAL_MS = 1000L;

    private SeamlessWorldLoadingHandler() {
    }

    public static void beginWorldLoad(@Nullable String worldSavePath) {
        setActiveTarget(LoadTarget.WORLD, worldSavePath);
    }

    public static void beginServerLoad(@Nullable String serverIp) {
        setActiveTarget(LoadTarget.SERVER, serverIp);
    }

    public static void preWarmWorldLoad(@Nullable String worldSavePath) {
        if (!isEnabled()) {
            clearPreWarmedWorld();
            return;
        }
        String resolved = normalizeIdentifier(LoadTarget.WORLD, worldSavePath);
        if (resolved == null) {
            clearPreWarmedWorld();
            return;
        }
        File screenshotFile = resolveExistingScreenshotFile(LoadTarget.WORLD, resolved);
        if (screenshotFile == null) {
            clearPreWarmedWorld();
            return;
        }
        String source = toLocalSource(screenshotFile);
        if (!Objects.equals(source, preWarmedWorldSource)) {
            preWarmedWorldSource = source;
            preWarmedWorldSupplier = ResourceSupplier.image(source);
        }
        preWarmedWorldFile = screenshotFile;
        if (preWarmedWorldSupplier != null) {
            preWarmedWorldSupplier.get();
        }
    }

    public static void preLoadRecentWorldScreenshots() {
        preLoadRecentScreenshots(LoadTarget.WORLD, RECENT_PRELOAD_LIMIT);
    }

    public static void preLoadRecentServerScreenshots() {
        preLoadRecentScreenshots(LoadTarget.SERVER, RECENT_PRELOAD_LIMIT);
    }

    public static void finishWorldLoad() {
        clearActiveTarget(LoadTarget.WORLD);
    }

    public static void finishServerLoad() {
        clearActiveTarget(LoadTarget.SERVER);
    }

    public static void startWorldCapture(@Nullable String worldSavePath) {
        setCaptureTarget(LoadTarget.WORLD, worldSavePath);
    }

    public static void startServerCapture(@Nullable String serverIp) {
        setCaptureTarget(LoadTarget.SERVER, serverIp);
    }

    public static void saveAndClearWorldCapture(@Nullable String worldSavePath) {
        saveAndClearCapture(LoadTarget.WORLD, worldSavePath);
    }

    public static void saveAndClearServerCapture(@Nullable String serverIp) {
        saveAndClearCapture(LoadTarget.SERVER, serverIp);
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

        AspectRatio aspectRatio = background.getAspectRatio();
        int[] renderSize = aspectRatio.getAspectRatioSizeByMinimumSize(width, height);
        int renderWidth = renderSize[0];
        int renderHeight = renderSize[1];
        if (renderWidth <= 0 || renderHeight <= 0) {
            return false;
        }

        int renderX = x + (width - renderWidth) / 2;
        int renderY = y + (height - renderHeight) / 2;

        RenderSystem.enableBlend();
        RenderingUtils.resetShaderColor(graphics);
        graphics.blit(location, renderX, renderY, 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
        RenderingUtils.resetShaderColor(graphics);
        RenderSystem.disableBlend();
        return true;
    }

    @Nullable
    private static RenderableResource getActiveLoadingBackground(@Nullable Screen screen) {
        if (!isEnabled()) {
            return null;
        }
        if (!isLoadingScreen(screen)) {
            return null;
        }

        ITexture texture = getActiveTexture();
        if (texture != null) {
            return texture;
        }

        if (screen instanceof GenericMessageScreen genericMessageScreen) {
            return getWorldJoinGenericMessageTexture(genericMessageScreen);
        }
        return null;
    }

    @Nullable
    private static ITexture getActiveTexture() {
        if (activeSupplier == null || activeFile == null) {
            return null;
        }
        ITexture texture = activeSupplier.get();
        if (texture == null || texture.isClosed()) {
            return null;
        }
        return texture;
    }

    @Nullable
    private static ITexture getWorldJoinGenericMessageTexture(@NotNull GenericMessageScreen screen) {
        if (!isWorldJoinGenericMessageScreen(screen) || LastWorldHandler.isLastWorldServer()) {
            return null;
        }
        String lastWorldPath = normalizeIdentifier(LoadTarget.WORLD, LastWorldHandler.getLastWorld());
        if (lastWorldPath == null) {
            return null;
        }
        File screenshotFile = resolveExistingScreenshotFile(LoadTarget.WORLD, lastWorldPath);
        if (screenshotFile == null) {
            return null;
        }
        if (preWarmedWorldFile != null && preWarmedWorldSupplier != null && screenshotFile.equals(preWarmedWorldFile)) {
            ITexture texture = preWarmedWorldSupplier.get();
            if (texture != null && !texture.isClosed()) {
                return texture;
            }
        }
        ITexture texture = ResourceSupplier.image(toLocalSource(screenshotFile)).get();
        if (texture == null || texture.isClosed()) {
            return null;
        }
        return texture;
    }

    private static boolean isWorldJoinGenericMessageScreen(@NotNull GenericMessageScreen screen) {
        Component title = screen.getTitle();
        if (!(title.getContents() instanceof TranslatableContents translatableContents)) {
            return false;
        }
        String key = translatableContents.getKey();
        return "selectWorld.data_read".equals(key) || "selectWorld.resource_load".equals(key);
    }

    private static void setActiveTarget(@NotNull LoadTarget target, @Nullable String identifier) {
        if (!isEnabled()) {
            clearAll();
            return;
        }
        String resolved = normalizeIdentifier(target, identifier);
        clearAll();
        if (resolved == null) {
            return;
        }
        File screenshotFile = resolveExistingScreenshotFile(target, resolved);
        if (screenshotFile == null) {
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
        clearPreWarmedWorld();
    }

    private static void clearPreWarmedWorld() {
        preWarmedWorldFile = null;
        preWarmedWorldSource = null;
        preWarmedWorldSupplier = null;
    }

    private static void preLoadRecentScreenshots(@NotNull LoadTarget target, int limit) {
        if (!isEnabled() || limit <= 0) {
            return;
        }
        File[] files = ROOT_DIR.listFiles((dir, name) -> {
            if (name == null || !name.endsWith(".png")) {
                return false;
            }
            return target == LoadTarget.WORLD ? name.startsWith("world_") : name.startsWith("server_");
        });
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        int preLoaded = 0;
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            try {
                ResourceSupplier.image(toLocalSource(file)).get();
            } catch (Exception ex) {
                LOGGER.warn("[FANCYMENU] Failed to pre-load seamless world loading screenshot: " + file.getName(), ex);
            }
            preLoaded++;
            if (preLoaded >= limit) {
                break;
            }
        }
    }

    public static void clearCapture() {
        clearCaptureState();
    }

    public static void captureFrameIfNeeded(@NotNull RenderTarget renderTarget) {
        if (!isEnabled()) {
            clearCaptureState();
            return;
        }
        if (captureTarget == null || captureIdentifier == null) {
            return;
        }
        long now = Util.getMillis();
        if (lastCapturedImage != null && now - lastCaptureTimeMs < CAPTURE_INTERVAL_MS) {
            return;
        }
        NativeImage image;
        try {
            image = Screenshot.takeScreenshot(renderTarget);
        } catch (Exception ex) {
            LOGGER.warn("[FANCYMENU] Failed to capture seamless world loading screenshot.", ex);
            return;
        }
        replaceLastCapturedImage(image);
        lastCaptureTimeMs = now;
    }

    private static void setCaptureTarget(@NotNull LoadTarget target, @Nullable String identifier) {
        if (!isEnabled()) {
            clearCaptureState();
            return;
        }
        String resolved = normalizeIdentifier(target, identifier);
        if (resolved == null) {
            clearCaptureState();
            return;
        }
        if (target != captureTarget || !resolved.equals(captureIdentifier)) {
            clearLastCapturedImage();
            captureTarget = target;
            captureIdentifier = resolved;
            lastCaptureTimeMs = 0L;
        }
    }

    private static void saveAndClearCapture(@NotNull LoadTarget target, @Nullable String identifier) {
        if (!isEnabled()) {
            clearCaptureState();
            return;
        }
        String resolved = normalizeIdentifier(target, identifier);
        if (resolved == null) {
            clearCaptureState();
            return;
        }
        if (captureTarget != target || !resolved.equals(captureIdentifier)) {
            clearCaptureState();
            return;
        }
        NativeImage image = lastCapturedImage;
        lastCapturedImage = null;
        captureTarget = null;
        captureIdentifier = null;
        lastCaptureTimeMs = 0L;
        if (image == null) {
            return;
        }
        File outputFile = resolveScreenshotFile(target, resolved);
        saveNativeImage(image, outputFile);
    }

    private static void clearCaptureState() {
        captureTarget = null;
        captureIdentifier = null;
        lastCaptureTimeMs = 0L;
        clearLastCapturedImage();
    }

    private static void clearLastCapturedImage() {
        if (lastCapturedImage != null) {
            lastCapturedImage.close();
            lastCapturedImage = null;
        }
    }

    private static void replaceLastCapturedImage(@NotNull NativeImage image) {
        clearLastCapturedImage();
        lastCapturedImage = image;
    }

    private static void saveNativeImage(@NotNull NativeImage image, @NotNull File outputFile) {
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

    @NotNull
    private static String normalizeWorldPathIdentifier(@NotNull String pathString) {
        try {
            return Path.of(pathString).toAbsolutePath().normalize().toString().replace("\\", "/");
        } catch (Exception ex) {
            return pathString.replace("\\", "/");
        }
    }

    @Nullable
    private static String normalizeIdentifier(@NotNull LoadTarget target, @Nullable String identifier) {
        String sanitized = sanitizeIdentifier(identifier);
        if (sanitized == null) {
            return null;
        }
        if (target == LoadTarget.WORLD) {
            return normalizeWorldPathIdentifier(sanitized);
        }
        return sanitized;
    }

    @Nullable
    private static File resolveExistingScreenshotFile(@NotNull LoadTarget target, @NotNull String identifier) {
        if (target == LoadTarget.WORLD) {
            for (String candidate : getWorldIdentifierCandidates(identifier)) {
                File file = resolveScreenshotFile(LoadTarget.WORLD, candidate);
                if (file.isFile()) {
                    return file;
                }
            }
            return null;
        }
        File file = resolveScreenshotFile(target, identifier);
        return file.isFile() ? file : null;
    }

    @NotNull
    private static Set<String> getWorldIdentifierCandidates(@NotNull String identifier) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(identifier);
        candidates.add(identifier.replace("\\", "/"));
        candidates.add(identifier.replace("/", "\\"));
        try {
            Path absolute = Path.of(identifier).toAbsolutePath();
            String absoluteRaw = absolute.toString();
            String absoluteSlash = absoluteRaw.replace("\\", "/");
            String absoluteBackslash = absoluteSlash.replace("/", "\\");
            String absoluteNormalized = absolute.normalize().toString();
            String absoluteNormalizedSlash = absoluteNormalized.replace("\\", "/");
            String absoluteNormalizedBackslash = absoluteNormalizedSlash.replace("/", "\\");
            String absoluteDotRaw = absolute.resolve(".").toString();
            String absoluteDotSlash = absoluteDotRaw.replace("\\", "/");
            String absoluteDotBackslash = absoluteDotSlash.replace("/", "\\");
            candidates.add(absoluteRaw);
            candidates.add(absoluteSlash);
            candidates.add(absoluteBackslash);
            candidates.add(absoluteNormalized);
            candidates.add(absoluteNormalizedSlash);
            candidates.add(absoluteNormalizedBackslash);
            candidates.add(absoluteDotRaw);
            candidates.add(absoluteDotSlash);
            candidates.add(absoluteDotBackslash);
        } catch (Exception ignored) {
        }
        return candidates;
    }

    private static boolean isEnabled() {
        return FancyMenu.getOptions().seamlessWorldLoading.getValue();
    }

    private static boolean isLoadingScreen(@Nullable Screen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof GenericMessageScreen genericMessageScreen) {
            return activeTarget != null || isWorldJoinGenericMessageScreen(genericMessageScreen);
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
