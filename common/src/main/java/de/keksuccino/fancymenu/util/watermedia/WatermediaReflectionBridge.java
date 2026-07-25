package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.threading.FancyMenuThreads;
import de.keksuccino.fancymenu.util.watermedia.vulkan.WatermediaVulkanInterop;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class WatermediaReflectionBridge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long SHUTDOWN_PLAYER_RELEASE_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(2L);
    private static final AtomicLong SHUTDOWN_PLAYER_RELEASE_DEADLINE_NANOS = new AtomicLong();
    private static final AtomicBoolean SHUTDOWN_PLAYER_RELEASE_TIMEOUT_LOGGED = new AtomicBoolean();
    private static volatile boolean WATERMEDIA_unsupported_gl_texture_handle_logged = false;

    @Nullable
    public static Object createMrl(@NotNull String source) {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();

        try {
            Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI", false, classLoader);
            Method createMrl = mediaApiClass.getMethod("mrl", String.class);
            return createMrl.invoke(null, source);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia MRL via MediaAPI#mrl for source: {}", source, ex);
        }

        return null;
    }

    @Nullable
    public static Object decodeImage(@NotNull byte[] data) {
        try {
            Class<?> codecsApiClass = Class.forName("org.watermedia.api.codecs.CodecsAPI", false, FancyMenu.class.getClassLoader());
            Method decodeImage = codecsApiClass.getMethod("decodeImage", byte[].class);
            return decodeImage.invoke(null, (Object) data);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to decode image with Watermedia CodecsAPI", ex);
        }
        return null;
    }

    public static int imageWidth(@Nullable Object image) {
        return invokeInt(image, "width", 0);
    }

    public static int imageHeight(@Nullable Object image) {
        return invokeInt(image, "height", 0);
    }

    public static int imageRepeat(@Nullable Object image) {
        return invokeInt(image, "repeat", 0);
    }

    @Nullable
    public static long[] imageDelay(@Nullable Object image) {
        Object result = invoke(image, "delay", 0);
        if (result instanceof long[] delays) return delays;
        return null;
    }

    @Nullable
    public static ByteBuffer[] imageFrames(@Nullable Object image) {
        Object result = invoke(image, "frames", 0);
        if (result instanceof ByteBuffer[] frameBuffers) return frameBuffers;
        return null;
    }

    public static boolean isMrlResolving(@Nullable Object mrl) {
        return mrlStatusName(mrl).equals("FETCHING");
    }

    public static boolean isMrlLoaded(@Nullable Object mrl) {
        return mrlStatusName(mrl).equals("LOADED");
    }

    public static boolean isMrlFailed(@Nullable Object mrl) {
        String statusName = mrlStatusName(mrl);
        return statusName.equals("ERROR")
                || statusName.equals("BLOCKED")
                || statusName.equals("EXPIRED")
                || statusName.equals("FORGOTTEN");
    }

    @NotNull
    public static String mrlStatusName(@Nullable Object mrl) {
        Object status = invoke(mrl, "status", 0);
        return (status != null) ? status.toString() : "UNKNOWN";
    }

    @Nullable
    public static Object createPlayer(@Nullable Object mrl, @NotNull Thread renderThread, @NotNull Executor renderThreadExecutor, boolean video, boolean audio) {
        if (mrl == null) return null;
        if (video && !WatermediaUtil.isWatermediaRenderingAvailable()) return null;

        WatermediaUtil.trySuppressDevelopmentFfmpegDebugLogs();

        try {
            Object player = createModernPlayer(mrl, renderThread, renderThreadExecutor, video, audio);
            if (player != null) {
                return player;
            }
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia player via media API", ex);
        }
        return null;
    }

    public static void playerStart(@Nullable Object player) {
        invoke(player, "start", 0);
    }

    public static void playerStartPaused(@Nullable Object player) {
        invoke(player, "startPaused", 0);
    }

    public static void playerPause(@Nullable Object player, boolean paused) {
        invoke(player, "pause", 1, paused);
    }

    public static void playerStop(@Nullable Object player) {
        invoke(player, "stop", 0);
    }

    public static void playerRelease(@Nullable Object player) {
        if (player instanceof ManagedModernPlayer managedModernPlayer) {
            managedModernPlayer.release();
            return;
        }
        invoke(player, "release", 0);
    }

    /**
     * Releases a player without allowing Watermedia's unbounded native thread joins to consume Minecraft's shutdown-watchdog window.
     * Modern players share one short wait budget so their GL engine can still be released on the render thread when native cleanup finishes promptly.
     */
    public static void playerReleaseForShutdown(@Nullable Object player) {
        if (player == null) return;
        if (player instanceof ManagedModernPlayer managedModernPlayer) {
            managedModernPlayer.releaseForShutdown();
            return;
        }
        FancyMenuThreads.startDaemonThread(() -> invoke(player, "release", 0), "Watermedia-PlayerRelease");
    }

    public static boolean playerIsPlaying(@Nullable Object player) {
        return invokeBoolean(player, "playing", false);
    }

    public static boolean playerIsPaused(@Nullable Object player) {
        return invokeBoolean(player, "paused", false);
    }

    @NotNull
    public static String playerStatusName(@Nullable Object player) {
        Object status = invoke(player, "status", 0);
        return (status != null) ? status.toString() : "UNKNOWN";
    }

    public static long playerTextureHandle(@Nullable Object player) {
        Number textureHandle = invokeNumber(player, "texture", 0);
        return textureHandle != null ? textureHandle.longValue() : 0L;
    }

    public static int playerWidth(@Nullable Object player) {
        return invokeInt(player, "width", 0);
    }

    public static int playerHeight(@Nullable Object player) {
        return invokeInt(player, "height", 0);
    }

    public static long playerDuration(@Nullable Object player) {
        return invokeLong(player, "duration", 0L);
    }

    public static long playerTime(@Nullable Object player) {
        return invokeLong(player, "time", 0L);
    }

    public static boolean playerSeek(@Nullable Object player, long timeMs) {
        Object result = invoke(player, "seek", 1, timeMs);
        if (result instanceof Boolean b) return b;
        return false;
    }

    public static boolean playerRepeat(@Nullable Object player) {
        return invokeBoolean(player, "repeat", false);
    }

    public static void setPlayerRepeat(@Nullable Object player, boolean repeat) {
        invoke(player, "repeat", 1, repeat);
    }

    public static void setPlayerVolume(@Nullable Object player, int volumePercent) {
        invoke(player, "volume", 1, volumePercent);
    }

    @Nullable
    private static Object createModernPlayer(@NotNull Object mrl, @NotNull Thread renderThread, @NotNull Executor renderThreadExecutor, boolean video, boolean audio) throws Throwable {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();
        Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI", false, classLoader);
        Class<?> mrlClass = Class.forName("org.watermedia.api.media.MRL", false, classLoader);

        Object gfxEngine = null;
        Object sfxEngine = null;
        AtomicBoolean gfxSupplied = new AtomicBoolean();
        AtomicBoolean sfxSupplied = new AtomicBoolean();

        try {
            gfxEngine = video ? buildModernGfxEngine(renderThread, renderThreadExecutor) : null;
            sfxEngine = audio ? buildModernSfxEngine() : null;
            Object preparedGfxEngine = gfxEngine;
            Object preparedSfxEngine = sfxEngine;
            Supplier<Object> gfxSupplier = () -> {
                gfxSupplied.set(true);
                return preparedGfxEngine;
            };
            Supplier<Object> sfxSupplier = () -> {
                sfxSupplied.set(true);
                return preparedSfxEngine;
            };
            Method createPlayer = mediaApiClass.getMethod("createPlayer", mrlClass, int.class, Supplier.class, Supplier.class);
            Object player = createPlayer.invoke(null, mrl, 0, gfxSupplier, sfxSupplier);

            // WaterMedia 3.0.0.22 owns an engine as soon as it invokes its supplier. Its player owns supplied engines on success, and MediaAPI releases them after a caught construction failure. Engines whose suppliers were never invoked remain FancyMenu's responsibility.
            if (!gfxSupplied.get()) releaseModernResource(gfxEngine);
            if (!sfxSupplied.get()) releaseModernResource(sfxEngine);
            if (player == null) return null;

            return new ManagedModernPlayer(player);
        } catch (Throwable ex) {
            // Errors deliberately propagate out of MediaAPI without its construction-failure cleanup, while reflection can fail before ownership transfer. Best-effort cleanup is therefore required for every pre-created engine on this path.
            releaseModernResource(gfxEngine);
            releaseModernResource(sfxEngine);
            throw ex;
        }
    }

    @NotNull
    private static Object buildModernGfxEngine(@NotNull Thread renderThread, @NotNull Executor renderThreadExecutor) throws Throwable {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();
        Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI", false, classLoader);
        if (graphicsBackend(RenderingUtils.isVulkanActive()) == GraphicsBackend.VULKAN) {
            Class<?> vkContextClass = Class.forName("org.watermedia.api.media.engines.vk.VKContext", false, classLoader);
            Object vkContext = WatermediaVulkanInterop.context();
            if (vkContext == null || !vkContextClass.isInstance(vkContext)) {
                throw new IllegalStateException("Minecraft's Vulkan device is not available through the Watermedia VKContext bridge");
            }
            Method createVkEngine = mediaApiClass.getMethod("vkEngine", vkContextClass);
            return createVkEngine.invoke(null, vkContext);
        }
        // WaterMedia 3.0.0.22 removed the callback-based builder; its factory engine now preserves the host's exact GL state itself, including state cached by Minecraft, Sodium, and Iris.
        Method createGlEngine = mediaApiClass.getMethod("glEngine", Thread.class, Executor.class);
        return createGlEngine.invoke(null, renderThread, renderThreadExecutor);
    }

    @NotNull
    static GraphicsBackend graphicsBackend(boolean vulkanActive) {
        return vulkanActive ? GraphicsBackend.VULKAN : GraphicsBackend.OPENGL;
    }

    static int openGlTextureId(long textureHandle) {
        if (textureHandle > 0L && textureHandle <= Integer.MAX_VALUE) return (int) textureHandle;
        if (textureHandle != 0L && !WATERMEDIA_unsupported_gl_texture_handle_logged) {
            WATERMEDIA_unsupported_gl_texture_handle_logged = true;
            LOGGER.warn("[FANCYMENU] Watermedia returned an unsupported OpenGL texture handle: {}", textureHandle);
        }
        return 0;
    }

    @NotNull
    private static Object buildModernSfxEngine() throws Throwable {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();
        Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI", false, classLoader);
        Method createAlEngine = mediaApiClass.getMethod("alEngine");
        return createAlEngine.invoke(null);
    }

    @Nullable
    private static Object invoke(@Nullable Object target, @NotNull String methodName, int parameterCount, Object... args) {
        Object invocationTarget = unwrapInvocationTarget(target);
        if (invocationTarget == null) return null;
        try {
            Method method = findMethod(invocationTarget.getClass(), methodName, parameterCount);
            if (method != null) {
                return method.invoke(invocationTarget, args);
            }
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to invoke Watermedia method '{}'", methodName, ex);
        }
        return null;
    }

    private static boolean invokeBoolean(@Nullable Object target, @NotNull String methodName, boolean fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Boolean b) return b;
        return fallback;
    }

    @Nullable
    private static Number invokeNumber(@Nullable Object target, @NotNull String methodName, int parameterCount, Object... args) {
        Object result = invoke(target, methodName, parameterCount, args);
        if (result instanceof Number number) return number;
        return null;
    }

    private static int invokeInt(@Nullable Object target, @NotNull String methodName, int fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Number n) return n.intValue();
        return fallback;
    }

    private static long invokeLong(@Nullable Object target, @NotNull String methodName, long fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Number n) return n.longValue();
        return fallback;
    }

    @Nullable
    private static Object unwrapInvocationTarget(@Nullable Object target) {
        if (target instanceof ManagedModernPlayer managedModernPlayer) {
            return managedModernPlayer.player;
        }
        return target;
    }

    private static void releaseModernResource(@Nullable Object resource) {
        if (resource == null) return;
        try {
            Method release = findMethod(resource.getClass(), "release", 0);
            if (release != null) {
                release.invoke(resource);
            }
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to release Watermedia engine resource", ex);
        }
    }

    private static boolean awaitShutdownPlayerRelease(@NotNull Thread releaseThread) {
        long now = System.nanoTime();
        long deadline = SHUTDOWN_PLAYER_RELEASE_DEADLINE_NANOS.updateAndGet(current -> current == 0L ? now + SHUTDOWN_PLAYER_RELEASE_BUDGET_NANOS : current);
        long remainingNanos = Math.max(0L, deadline - now);
        if (remainingNanos > 0L) {
            long remainingMillis = remainingNanos / 1_000_000L;
            int additionalNanos = (int) (remainingNanos % 1_000_000L);
            try {
                releaseThread.join(remainingMillis, additionalNanos);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        boolean completed = !releaseThread.isAlive();
        if (!completed && SHUTDOWN_PLAYER_RELEASE_TIMEOUT_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("[FANCYMENU] Watermedia player release exceeded the shared client-shutdown time budget; remaining native cleanup will finish on a daemon thread.");
        }
        return completed;
    }

    @Nullable
    private static Method findMethod(@NotNull Class<?> type, @NotNull String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private static final class ManagedModernPlayer {
        // WaterMedia 3.0.0.22 players own and release both supplied engines. Retaining either engine here would make every successful player release it twice.
        private final Object player;
        private final AtomicBoolean released = new AtomicBoolean();

        private ManagedModernPlayer(@NotNull Object player) {
            this.player = player;
        }

        private void release() {
            if (!this.released.compareAndSet(false, true)) return;

            this.releasePlayer();
        }

        private void releaseForShutdown() {
            if (!this.released.compareAndSet(false, true)) return;

            Thread releaseThread = FancyMenuThreads.startDaemonThread(this::releasePlayer, "Watermedia-NativePlayerRelease");
            awaitShutdownPlayerRelease(releaseThread);
        }

        private void releasePlayer() {
            try {
                Method release = findMethod(this.player.getClass(), "release", 0);
                if (release != null) {
                    release.invoke(this.player);
                }
            } catch (Throwable ex) {
                LOGGER.error("[FANCYMENU] Failed to release Watermedia player", ex);
            }
        }
    }

    enum GraphicsBackend {
        OPENGL,
        VULKAN
    }

}
