package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class WatermediaReflectionBridge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile boolean WATERMEDIA_unsupported_texture_handle_logged = false;

    @Nullable
    public static Object createMrl(@NotNull String source) {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();

        try {
            Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI", false, classLoader);
            Method getMrl = mediaApiClass.getMethod("getMRL", String.class);
            return getMrl.invoke(null, source);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia MRL via MediaAPI#getMRL for source: {}", source, ex);
        }

        return null;
    }

    @Nullable
    public static Object decodeImage(@NotNull byte[] data) {
        try {
            Class<?> decoderApiClass = Class.forName("org.watermedia.api.decode.DecoderAPI", false, FancyMenu.class.getClassLoader());
            Method decodeImage = decoderApiClass.getMethod("decodeImage", byte[].class);
            return decodeImage.invoke(null, (Object) data);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to decode image with Watermedia DecoderAPI", ex);
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

    public static int playerTextureId(@Nullable Object player) {
        Number textureHandle = invokeNumber(player, "texture", 0);
        if (textureHandle == null) return 0;

        long textureId = textureHandle.longValue();
        if (textureId <= 0L) return 0;
        if (textureId > Integer.MAX_VALUE) {
            if (!WATERMEDIA_unsupported_texture_handle_logged) {
                WATERMEDIA_unsupported_texture_handle_logged = true;
                LOGGER.warn("[FANCYMENU] Watermedia returned an unsupported texture handle for Minecraft rendering: {}", textureId);
            }
            return 0;
        }

        return (int) textureId;
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

        WatermediaRenderThreadExecutor gfxExecutor = video ? new WatermediaRenderThreadExecutor(renderThread, renderThreadExecutor) : null;
        Object gfxEngine = null;
        Object sfxEngine = null;

        try {
            gfxEngine = video ? buildModernGfxEngine(renderThread, gfxExecutor) : null;
            sfxEngine = audio ? buildModernSfxEngine() : null;
            Object createdGfxEngine = gfxEngine;
            Object createdSfxEngine = sfxEngine;
            Supplier<Object> gfxSupplier = () -> createdGfxEngine;
            Supplier<Object> sfxSupplier = () -> createdSfxEngine;
            Method createPlayer = mediaApiClass.getMethod("createPlayer", mrlClass, int.class, Supplier.class, Supplier.class);
            Object player = createPlayer.invoke(null, mrl, 0, gfxSupplier, sfxSupplier);

            if (player == null) {
                releaseModernGfxResource(gfxEngine, gfxExecutor);
                releaseModernResource(sfxEngine);
                return null;
            }

            return new ManagedModernPlayer(player, gfxEngine, gfxExecutor);
        } catch (Throwable ex) {
            releaseModernGfxResource(gfxEngine, gfxExecutor);
            releaseModernResource(sfxEngine);
            throw ex;
        }
    }

    @NotNull
    private static Object buildModernGfxEngine(@NotNull Thread renderThread, @NotNull Executor renderThreadExecutor) throws Throwable {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();
        Class<?> builderClass = Class.forName("org.watermedia.api.media.engines.GLEngine$Builder", false, classLoader);
        Object builder = builderClass.getConstructor(Thread.class, Executor.class).newInstance(renderThread, renderThreadExecutor);
        Method build = builderClass.getMethod("build");
        return build.invoke(builder);
    }

    @NotNull
    private static Object buildModernSfxEngine() throws Throwable {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();
        Class<?> alEngineClass = Class.forName("org.watermedia.api.media.engines.ALEngine", false, classLoader);

        try {
            Method buildDefault = alEngineClass.getMethod("buildDefault");
            return buildDefault.invoke(null);
        } catch (NoSuchMethodException ignored) {
            Class<?> builderClass = Class.forName("org.watermedia.api.media.engines.ALEngine$Builder", false, classLoader);
            Object builder = builderClass.getConstructor().newInstance();
            Method build = builderClass.getMethod("build");
            return build.invoke(builder);
        }
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

    private static void releaseModernGfxResource(@Nullable Object gfxEngine, @Nullable WatermediaRenderThreadExecutor gfxExecutor) {
        Runnable releaseTask = gfxEngine != null ? () -> releaseModernResource(gfxEngine) : () -> {};
        if (gfxExecutor != null) {
            try {
                gfxExecutor.close(releaseTask);
            } catch (Throwable ex) {
                LOGGER.error("[FANCYMENU] Failed to schedule Watermedia graphics-engine release on the render thread", ex);
            }
            return;
        }
        releaseTask.run();
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

    static final class ManagedModernPlayer {
        private final Object player;
        @Nullable
        private final Object gfxEngine;
        @Nullable
        private final WatermediaRenderThreadExecutor gfxExecutor;
        private final AtomicBoolean released = new AtomicBoolean(false);

        ManagedModernPlayer(@NotNull Object player, @Nullable Object gfxEngine, @Nullable WatermediaRenderThreadExecutor gfxExecutor) {
            this.player = player;
            this.gfxEngine = gfxEngine;
            this.gfxExecutor = gfxExecutor;
        }

        void release() {
            if (!this.released.compareAndSet(false, true)) return;

            try {
                Method release = findMethod(this.player.getClass(), "release", 0);
                if (release != null) {
                    release.invoke(this.player);
                }
            } catch (Throwable ex) {
                LOGGER.error("[FANCYMENU] Failed to release Watermedia player", ex);
            } finally {
                // FFMediaPlayer.release() stops and joins its producer threads before releasing SFX. Only then can the
                // executor reject new GL submissions and wait for its already-accepted render work to drain.
                releaseModernGfxResource(this.gfxEngine, this.gfxExecutor);
            }
        }
    }

}
