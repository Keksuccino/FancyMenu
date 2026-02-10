package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public class WatermediaReflectionBridge {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static Object createMrl(@NotNull String source) {
        try {
            Class<?> mrlClass = Class.forName("org.watermedia.api.media.MRL", false, FancyMenu.class.getClassLoader());
            Method get = mrlClass.getMethod("get", String.class);
            return get.invoke(null, source);
        } catch (Throwable ex) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia MRL for source: {}", source, ex);
        }
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
        Method createPlayer = findMethod(mrl.getClass(), "createPlayer", 7);
        if (createPlayer == null) {
            LOGGER.error("[FANCYMENU] Failed to create Watermedia player, unable to find MRL#createPlayer(..) method");
            return null;
        }
        try {
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
        return invokeInt(player, "texture", 0);
    }

    public static int playerWidth(@Nullable Object player) {
        return invokeInt(player, "width", 0);
    }

    public static int playerHeight(@Nullable Object player) {
        return invokeInt(player, "height", 0);
    }

    public static void setPlayerVolume(@Nullable Object player, int volumePercent) {
        invoke(player, "volume", 1, volumePercent);
    }

    @Nullable
    private static Object invoke(@Nullable Object target, @NotNull String methodName, int parameterCount, Object... args) {
        if (target == null) return null;
        try {
            Method method = findMethod(target.getClass(), methodName, parameterCount);
            if (method != null) {
                return method.invoke(target, args);
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

    private static int invokeInt(@Nullable Object target, @NotNull String methodName, int fallback) {
        Object result = invoke(target, methodName, 0);
        if (result instanceof Number n) return n.intValue();
        return fallback;
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

}
