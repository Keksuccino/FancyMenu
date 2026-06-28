package de.keksuccino.fancymenu.util.watermedia;

import com.mojang.blaze3d.opengl.GlStateManager;
import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33C;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
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

        Object gfxEngine = video ? buildModernGfxEngine(renderThread, renderThreadExecutor) : null;
        Object sfxEngine = audio ? buildModernSfxEngine() : null;
        Supplier<Object> gfxSupplier = () -> gfxEngine;
        Supplier<Object> sfxSupplier = () -> sfxEngine;

        try {
            Method createPlayer = mediaApiClass.getMethod("createPlayer", mrlClass, int.class, Supplier.class, Supplier.class);
            Object player = createPlayer.invoke(null, mrl, 0, gfxSupplier, sfxSupplier);

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
        configureModernGfxEngineBuilder(builder);
        Method build = builderClass.getMethod("build");
        return build.invoke(builder);
    }

    private static void configureModernGfxEngineBuilder(@NotNull Object builder) throws Throwable {
        setBuilderCallbackIfPresent(builder, "setGenTexture", (IntSupplier) GlStateManager::_genTexture);
        setBuilderBindConsumerIfPresent(builder, "setBindTexture", WatermediaReflectionBridge::bindTexture);
        setBuilderTexParamConsumerIfPresent(builder, "setTexParameter", WatermediaReflectionBridge::texParameter);
        setBuilderBindConsumerIfPresent(builder, "setPixelStore", GlStateManager::_pixelStore);
        setBuilderCallbackIfPresent(builder, "setDelTexture", (IntConsumer) GlStateManager::_deleteTexture);
        setBuilderCallbackIfPresent(builder, "setGetTexture", (IntConsumer) WatermediaReflectionBridge::bindTexture);
        setBuilderCallbackIfPresent(builder, "setActiveTexture", (IntConsumer) WatermediaReflectionBridge::activeTexture);
        setBuilderCallbackIfPresent(builder, "setBindVertexArray", (IntConsumer) GlStateManager::_glBindVertexArray);
        setBuilderBindConsumerIfPresent(builder, "setBindFrameBuffer", WatermediaReflectionBridge::bindFrameBuffer);
        setBuilderBindConsumerIfPresent(builder, "setBindBuffer", GlStateManager::_glBindBuffer);
    }

    private static void bindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            bindTexture(texture);
            return;
        }
        GL11.glBindTexture(target, texture);
    }

    private static void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GlStateManager._bindTexture(texture);
    }

    private static void activeTexture(int texture) {
        unbindSampler(texture);
        GL13.glActiveTexture(texture);
        GlStateManager._activeTexture(texture);
    }

    private static void bindFrameBuffer(int target, int frameBuffer) {
        GL30.glBindFramebuffer(target, frameBuffer);
        GlStateManager._glBindFramebuffer(target, frameBuffer);
    }

    private static void texParameter(int target, int parameterName, int value) {
        GlStateManager._texParameter(target, parameterName, value);
        if (target == GL11.GL_TEXTURE_2D) {
            // WaterMedia video textures are single-level textures; keep external samplers from requiring mipmaps.
            GlStateManager._texParameter(target, GL12.GL_TEXTURE_BASE_LEVEL, 0);
            GlStateManager._texParameter(target, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        }
    }

    private static void unbindSampler(int texture) {
        int textureUnit = texture - GL13.GL_TEXTURE0;
        if (textureUnit >= 0) {
            // Minecraft's sampler objects override texture parameters. WaterMedia expects raw texture state here.
            GL33C.glBindSampler(textureUnit, 0);
        }
    }

    private static void setBuilderCallbackIfPresent(@NotNull Object builder, @NotNull String methodName, @NotNull Object callback) throws Throwable {
        Method method = findMethod(builder.getClass(), methodName, 1);
        if (method == null) return;
        if (!method.getParameterTypes()[0].isInstance(callback)) return;
        method.invoke(builder, callback);
    }

    private static void setBuilderBindConsumerIfPresent(@NotNull Object builder, @NotNull String methodName, @NotNull IntPairConsumer callback) throws Throwable {
        setBuilderProxyCallbackIfPresent(builder, methodName, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return handleProxyObjectMethod(proxy, method, args, methodName);
            callback.accept(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
            return null;
        });
    }

    private static void setBuilderTexParamConsumerIfPresent(@NotNull Object builder, @NotNull String methodName, @NotNull IntTripleConsumer callback) throws Throwable {
        setBuilderProxyCallbackIfPresent(builder, methodName, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return handleProxyObjectMethod(proxy, method, args, methodName);
            callback.accept(((Number) args[0]).intValue(), ((Number) args[1]).intValue(), ((Number) args[2]).intValue());
            return null;
        });
    }

    private static void setBuilderProxyCallbackIfPresent(@NotNull Object builder, @NotNull String methodName, @NotNull InvocationHandler invocationHandler) throws Throwable {
        Method method = findMethod(builder.getClass(), methodName, 1);
        if (method == null) return;
        Class<?> callbackType = method.getParameterTypes()[0];
        if (!callbackType.isInterface()) return;
        Object callback = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[]{callbackType}, invocationHandler);
        method.invoke(builder, callback);
    }

    @Nullable
    private static Object handleProxyObjectMethod(@NotNull Object proxy, @NotNull Method method, @Nullable Object[] args, @NotNull String name) {
        return switch (method.getName()) {
            case "toString" -> "FancyMenu WaterMedia GL callback proxy: " + name;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == ((args != null && args.length > 0) ? args[0] : null);
            default -> null;
        };
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

    @FunctionalInterface
    private interface IntPairConsumer {
        void accept(int first, int second);
    }

    @FunctionalInterface
    private interface IntTripleConsumer {
        void accept(int first, int second, int third);
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
