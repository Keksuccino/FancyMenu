package de.keksuccino.fancymenu.util.watermedia;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WatermediaUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    public static volatile boolean WATERMEDIA_critical_failure = false;
    public static volatile boolean WATERMEDIA_initialized = false;
    protected static volatile boolean WATERMEDIA_dev_ffmpeg_log_level_suppressed = false;

    public static boolean isWatermediaLoaded() {
        if (WATERMEDIA_critical_failure) return false;
        if (FancyMenu.getOptions().devForceWatermediaMissing.getValue()) return false;
        try {
            Class.forName("org.watermedia.api.media.MRL", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isWatermediaBinariesLoaded() {
        if (WATERMEDIA_critical_failure) return false;
        if (FancyMenu.getOptions().devForceWatermediaMissing.getValue()) return false;
        try {
            Class.forName("org.bytedeco.ffmpeg.global.avutil", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isWatermediaVideoPlaybackAvailable() {
        return isWatermediaLoaded() && isWatermediaBinariesLoaded();
    }

    /**
     * Suppresses FFmpeg debug spam from Watermedia while developing FancyMenu.
     * Production behavior is intentionally left untouched.
     */
    public static void trySuppressDevelopmentFfmpegDebugLogs() {
        if (WATERMEDIA_dev_ffmpeg_log_level_suppressed) return;
        if (!Services.PLATFORM.isDevelopmentEnvironment()) return;
        if (!isWatermediaLoaded()) return;

        try {
            Class<?> avutilClass = Class.forName("org.bytedeco.ffmpeg.global.avutil", false, FancyMenu.class.getClassLoader());

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
