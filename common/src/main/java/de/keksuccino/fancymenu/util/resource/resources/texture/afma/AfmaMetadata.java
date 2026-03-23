package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class AfmaMetadata {

    public static final String FORMAT_NAME = "AFMA";
    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final int DEFAULT_KEYFRAME_INTERVAL = 30;

    @Nullable
    protected String format;
    protected int format_version;
    protected int canvas_width;
    protected int canvas_height;
    protected int loop_count;
    protected long frame_time;
    protected long frame_time_intro;
    @Nullable
    protected Map<Integer, Long> custom_frame_times;
    @Nullable
    protected Map<Integer, Long> custom_frame_times_intro;
    protected int keyframe_interval;
    @Nullable
    protected Encoding encoding;
    @Nullable
    protected Creator creator;

    public AfmaMetadata() {
    }

    @NotNull
    public static AfmaMetadata create(int canvasWidth, int canvasHeight, int loopCount, long frameTime, long frameTimeIntro,
                                      @Nullable Map<Integer, Long> customFrameTimes, @Nullable Map<Integer, Long> customFrameTimesIntro,
                                      int keyframeInterval, boolean rectCopyEnabled, boolean duplicateFrameElision) {
        AfmaMetadata metadata = new AfmaMetadata();
        metadata.format = FORMAT_NAME;
        metadata.format_version = CURRENT_FORMAT_VERSION;
        metadata.canvas_width = canvasWidth;
        metadata.canvas_height = canvasHeight;
        metadata.loop_count = loopCount;
        metadata.frame_time = frameTime;
        metadata.frame_time_intro = frameTimeIntro;
        metadata.custom_frame_times = copyIfNotEmpty(customFrameTimes);
        metadata.custom_frame_times_intro = copyIfNotEmpty(customFrameTimesIntro);
        metadata.keyframe_interval = keyframeInterval;
        metadata.encoding = new Encoding(8, null, rectCopyEnabled, duplicateFrameElision);
        metadata.creator = null;
        return metadata;
    }

    protected static @Nullable LinkedHashMap<Integer, Long> copyIfNotEmpty(@Nullable Map<Integer, Long> values) {
        if ((values == null) || values.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(values);
    }

    @Nullable
    public String getFormat() {
        return this.format;
    }

    public int getFormatVersion() {
        return this.format_version;
    }

    public int getCanvasWidth() {
        return this.canvas_width;
    }

    public int getCanvasHeight() {
        return this.canvas_height;
    }

    public int getLoopCount() {
        return this.loop_count;
    }

    public long getFrameTime() {
        return this.frame_time;
    }

    public long getFrameTimeIntro() {
        return (this.frame_time_intro > 0L) ? this.frame_time_intro : this.frame_time;
    }

    @NotNull
    public Map<Integer, Long> getCustomFrameTimes() {
        return (this.custom_frame_times != null) ? this.custom_frame_times : Map.of();
    }

    @NotNull
    public Map<Integer, Long> getCustomFrameTimesIntro() {
        return (this.custom_frame_times_intro != null) ? this.custom_frame_times_intro : Map.of();
    }

    public int getKeyframeInterval() {
        return (this.keyframe_interval > 0) ? this.keyframe_interval : DEFAULT_KEYFRAME_INTERVAL;
    }

    @Nullable
    public Encoding getEncoding() {
        return this.encoding;
    }

    @Nullable
    public Creator getCreator() {
        return this.creator;
    }

    public long getFrameTimeForFrame(int frameIndex, boolean introFrame) {
        Long customDelay = introFrame ? this.getCustomFrameTimesIntro().get(frameIndex) : this.getCustomFrameTimes().get(frameIndex);
        if ((customDelay != null) && (customDelay > 0L)) {
            return customDelay;
        }
        return introFrame ? this.getFrameTimeIntro() : this.getFrameTime();
    }

    public void validate() {
        if ((this.format == null) || !FORMAT_NAME.equalsIgnoreCase(this.format)) {
            throw new IllegalArgumentException("metadata.json is missing the AFMA format marker");
        }
        if (this.format_version != CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported AFMA format version: " + this.format_version);
        }
        if (this.canvas_width <= 0 || this.canvas_height <= 0) {
            throw new IllegalArgumentException("AFMA canvas size is invalid");
        }
        if (this.frame_time <= 0L) {
            throw new IllegalArgumentException("AFMA frame_time must be greater than 0");
        }
        if ((this.frame_time_intro <= 0L) && (this.frame_time <= 0L)) {
            throw new IllegalArgumentException("AFMA frame_time_intro must be greater than 0");
        }
    }

    public static class Encoding {

        protected int png_bit_depth;
        @Nullable
        protected String color_model;
        protected boolean rect_copy_enabled;
        protected boolean duplicate_frame_elision;

        public Encoding() {
        }

        public Encoding(int pngBitDepth, @Nullable String colorModel, boolean rectCopyEnabled, boolean duplicateFrameElision) {
            this.png_bit_depth = pngBitDepth;
            this.color_model = colorModel;
            this.rect_copy_enabled = rectCopyEnabled;
            this.duplicate_frame_elision = duplicateFrameElision;
        }

        public int getPngBitDepth() {
            return this.png_bit_depth;
        }

        @Nullable
        public String getColorModel() {
            return this.color_model;
        }

        public boolean isRectCopyEnabled() {
            return this.rect_copy_enabled;
        }

        public boolean isDuplicateFrameElision() {
            return this.duplicate_frame_elision;
        }

    }

    public static class Creator {

        @Nullable
        protected String tool;
        @Nullable
        protected String tool_version;
        @Nullable
        protected String created_at_utc;

        public Creator() {
        }

        public Creator(@Nullable String tool, @Nullable String toolVersion, @Nullable String createdAtUtc) {
            this.tool = tool;
            this.tool_version = toolVersion;
            this.created_at_utc = createdAtUtc;
        }

        @Nullable
        public String getTool() {
            return this.tool;
        }

        @Nullable
        public String getToolVersion() {
            return this.tool_version;
        }

        @Nullable
        public String getCreatedAtUtc() {
            return this.created_at_utc;
        }

    }

}
