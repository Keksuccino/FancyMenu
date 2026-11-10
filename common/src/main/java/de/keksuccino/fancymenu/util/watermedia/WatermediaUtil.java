package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WatermediaUtil {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String WATERMEDIA_MRL_CLASS = "org.watermedia.api.player.MRL";
    // WaterMedia ships the FFmpeg Java bindings in its own jar, so only the binaries mod's own module class proves that the natives are installed
    private static final String WATERMEDIA_BINARIES_CLASS = "org.watermedia.binaries.WaterMediaBinaries";
    private static final String WATERMEDIA_CODECS_API_CLASS = "org.watermedia.api.codecs.CodecsAPI";
    private static final String FFMPEG_AVUTIL_CLASS = "org.bytedeco.ffmpeg.global.avutil";

    public static volatile boolean WATERMEDIA_critical_failure = false;
    public static volatile boolean WATERMEDIA_initialized = false;
    protected static volatile boolean WATERMEDIA_dev_ffmpeg_log_level_suppressed = false;

    public static boolean isWatermediaLoaded() {
        return isWatermediaClassAvailable(WATERMEDIA_MRL_CLASS);
    }

    public static boolean isWatermediaBinariesLoaded() {
        return isWatermediaClassAvailable(WATERMEDIA_BINARIES_CLASS);
    }

    private static boolean isWatermediaClassAvailable(@NotNull String className) {
        if (WATERMEDIA_critical_failure) return false;
        if (FancyMenu.getOptions().devForceWatermediaMissing.getValue()) return false;
        try {
            Class.forName(className, false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isWatermediaVideoPlaybackAvailable() {
        boolean watermediaLoaded = isWatermediaLoaded();
        return isWatermediaVideoPlaybackAvailable(watermediaLoaded, watermediaLoaded && isWatermediaBinariesLoaded());
    }

    public static boolean isWatermediaRenderingAvailable() {
        return isWatermediaRenderingAvailable(isWatermediaLoaded());
    }

    static boolean isWatermediaVideoPlaybackAvailable(boolean watermediaLoaded, boolean watermediaBinariesLoaded) {
        return isWatermediaRenderingAvailable(watermediaLoaded) && watermediaBinariesLoaded;
    }

    static boolean isWatermediaRenderingAvailable(boolean watermediaLoaded) {
        return watermediaLoaded;
    }

    /**
     * Suppresses FFmpeg debug spam from Watermedia while developing FancyMenu.
     * Production behavior is intentionally left untouched.<br>
     * Only runs after Watermedia loaded FFmpeg, because the first access to the FFmpeg bindings loads their natives,
     * and loading them before Watermedia pointed JavaCPP to the extracted binaries would break FFmpeg for the whole session.
     */
    public static void trySuppressDevelopmentFfmpegDebugLogs() {
        if (WATERMEDIA_dev_ffmpeg_log_level_suppressed) return;
        if (!Services.PLATFORM.isDevelopmentEnvironment()) return;
        if (!isWatermediaLoaded()) return;

        try {
            Class<?> codecsApiClass = Class.forName(WATERMEDIA_CODECS_API_CLASS, false, FancyMenu.class.getClassLoader());
            if (!Boolean.TRUE.equals(codecsApiClass.getMethod("ffmpegLoaded").invoke(null))) return;

            Class<?> avutilClass = Class.forName(FFMPEG_AVUTIL_CLASS, false, FancyMenu.class.getClassLoader());

            Field avLogPrintLevelField = avutilClass.getField("AV_LOG_PRINT_LEVEL");
            Field avLogSkipRepeatedField = avutilClass.getField("AV_LOG_SKIP_REPEATED");
            Field avLogInfoField = avutilClass.getField("AV_LOG_INFO");

            int avLogPrintLevel = avLogPrintLevelField.getInt(null);
            int avLogSkipRepeated = avLogSkipRepeatedField.getInt(null);
            int avLogInfo = avLogInfoField.getInt(null);

            Method avLogSetFlags = avutilClass.getMethod("av_log_set_flags", int.class);
            Method avLogSetLevel = avutilClass.getMethod("av_log_set_level", int.class);

            avLogSetFlags.invoke(null, (avLogPrintLevel | avLogSkipRepeated));
            avLogSetLevel.invoke(null, avLogInfo);

            WATERMEDIA_dev_ffmpeg_log_level_suppressed = true;

            LOGGER.info("[FANCYMENU] Suppressed Watermedia FFmpeg debug logs for development environment.");

        } catch (Throwable ignored) {}
    }

}
