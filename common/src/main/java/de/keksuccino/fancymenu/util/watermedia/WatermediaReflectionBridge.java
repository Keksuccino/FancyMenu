package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

public class WatermediaReflectionBridge {

    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile boolean WATERMEDIA_unsupported_texture_handle_logged = false;

    @Nullable
    public static Object createMrl(@NotNull String source) {
        ClassLoader classLoader = FancyMenu.class.getClassLoader();

        // Watermedia 3.x+ API
        try {
            Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI", false, classLoader);
            Method getMrl = mediaApiClass.getMethod("getMRL", String.class);
            return getMrl.invoke(null, source);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Fall back to older APIs below
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia MRL via MediaAPI#getMRL for source: {}", source, ex);
        }

        // Legacy Watermedia API
        try {
            Class<?> mrlClass = Class.forName("org.watermedia.api.media.MRL", false, classLoader);
            Method get = mrlClass.getMethod("get", String.class);
            return get.invoke(null, source);
        } catch (NoSuchMethodException ignored) {
            // Some versions only expose get(URI)
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia MRL via MRL#get(String) for source: {}", source, ex);
        }

        // Legacy/alpha variants with URI signature.
        try {
            Class<?> mrlClass = Class.forName("org.watermedia.api.media.MRL", false, classLoader);
            Method get = mrlClass.getDeclaredMethod("get", URI.class);
            get.setAccessible(true);
            File sourceFile = new File(source);
            URI sourceUri;
            if (sourceFile.exists()) {
                sourceUri = sourceFile.getAbsoluteFile().toURI();
            } else {
                try {
                    sourceUri = URI.create(source);
                } catch (Throwable ignored) {
                    sourceUri = sourceFile.getAbsoluteFile().toURI();
                }
            }
            return get.invoke(null, sourceUri);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia MRL for source: {}", source, ex);
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

    public static boolean isMrlBusy(@Nullable Object mrl) {
        return invokeBoolean(mrl, "busy", false);
    }

    public static boolean isMrlError(@Nullable Object mrl) {
        return invokeBoolean(mrl, "error", true);
    }

    @Nullable
    public static Object createPlayer(@Nullable Object mrl, @NotNull Thread renderThread, @NotNull Executor renderThreadExecutor, boolean video, boolean audio) {
        if (mrl == null) return null;

        WatermediaUtil.trySuppressDevelopmentFfmpegDebugLogs();

        // Watermedia 3.0.0-beta.15+ API
        try {
            Object player = createModernPlayer(mrl, renderThread, renderThreadExecutor, video, audio);
            if (player != null) {
                return player;
            }
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Fall back to the legacy API below.
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia player via beta15+ engine API", ex);
        }

        // Legacy Watermedia 3.x API
        try {
            Method createPlayer = findMethod(mrl.getClass(), "createPlayer", 7);
            if (createPlayer == null) {
                LOGGER.error("[FANCYMENU] Failed to create Watermedia player, unable to find MRL#createPlayer(..) method");
                return null;
            }
            return createPlayer.invoke(mrl, 0, renderThread, renderThreadExecutor, null, null, video, audio);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia player!", ex);
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
        Class<?> gfxEngineClass = Class.forName("org.watermedia.api.media.engines.GFXEngine", false, classLoader);
        Class<?> sfxEngineClass = Class.forName("org.watermedia.api.media.engines.SFXEngine", false, classLoader);

        Object gfxEngine = video ? buildModernGfxEngine(renderThread, renderThreadExecutor) : null;
        Object sfxEngine = audio ? buildModernSfxEngine() : null;

        try {
            Object player;
            try {
                Method createPlayer = mrl.getClass().getMethod("createPlayer", int.class, gfxEngineClass, sfxEngineClass);
                player = createPlayer.invoke(mrl, 0, gfxEngine, sfxEngine);
            } catch (NoSuchMethodException ignored) {
                Method createPlayer = mrl.getClass().getMethod("createPlayer", gfxEngineClass, sfxEngineClass);
                player = createPlayer.invoke(mrl, gfxEngine, sfxEngine);
            }

            if (player == null) {
                releaseModernResource(gfxEngine);
                releaseModernResource(sfxEngine);
                return null;
            }

            return new ManagedModernPlayer(player, gfxEngine);
        } catch (Throwable ex) {
            releaseModernResource(gfxEngine);
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
        private final Object player;
        @Nullable
        private final Object gfxEngine;
        private volatile boolean released = false;

        private ManagedModernPlayer(@NotNull Object player, @Nullable Object gfxEngine) {
            this.player = player;
            this.gfxEngine = gfxEngine;
        }

        private void release() {
            if (this.released) return;
            this.released = true;

            try {
                Method release = findMethod(this.player.getClass(), "release", 0);
                if (release != null) {
                    release.invoke(this.player);
                }
            } catch (Throwable ex) {
                LOGGER.error("[FANCYMENU] Failed to release Watermedia player", ex);
            }

            releaseModernResource(this.gfxEngine);
        }
    }

}
