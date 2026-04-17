package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaIoHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Standalone command-line entrypoint for isolated AFMA encode testing.
 *
 * <p>This class exists so the AFMA encoder can be profiled, benchmarked, and regression-tested without opening
 * FancyMenu's creator UI or booting the full in-game export screen flow. It drives {@link AfmaEncodePlanner} and
 * {@link AfmaArchiveWriter} directly and mirrors the creator-side preset defaults in plain Java code.
 *
 * <p>Primary use cases:
 * <ul>
 *     <li>Compare output size across encoder revisions.</li>
 *     <li>Measure encode-only performance on a fixed frame set.</li>
 *     <li>Debug frame-family selection and archive-packing behavior outside the Minecraft UI.</li>
 *     <li>Compile only the AFMA encode side plus its direct dependencies in an isolated test setup.</li>
 * </ul>
 *
 * <p>Compilation notes:
 * <ul>
 *     <li>Target Java 21, matching the project toolchain.</li>
 *     <li>This tool intentionally avoids {@code AfmaCreatorScreen} and {@code AfmaCreatorState}.</li>
 *     <li>For an isolated compile, include the AFMA packages under
 *     {@code de.keksuccino.fancymenu.util.resource.resources.texture.afma} and
 *     {@code de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator}, plus shared utility classes
 *     they reference such as {@code CloseableUtils}.</li>
 *     <li>The minimal external libraries still need to be on the classpath. In practice that means the same libraries
 *     the encode path already uses, such as JetBrains annotations, Gson, Apache Commons IO/Compress, and LWJGL STB/System
 *     for PNG loading inside {@link AfmaFrameNormalizer}.</li>
 *     <li>If you compile from IntelliJ, the easiest path is a plain Application run configuration for this class while
 *     limiting the compile target to the AFMA encode-side source set you want to test.</li>
 * </ul>
 *
 * <p>Runtime notes:
 * <ul>
 *     <li>Use {@code --main <dir>} for the required PNG frame directory.</li>
 *     <li>Use {@code --intro <dir>} for optional intro frames.</li>
 *     <li>Use {@code --output <file>} for the produced AFMA path.</li>
 *     <li>Use {@code --preset smallest_file}, {@code balanced}, or {@code fastest_decode} to mirror the creator presets.</li>
 *     <li>Use {@code --temp-dir <dir>} or the {@code fancymenu.afma.temp_dir} system property to redirect AFMA temp files
 *     during isolated runs.</li>
 * </ul>
 *
 * <p>Example invocation after compiling:
 * <pre>{@code
 * java ... de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator.AfmaEncodeStandaloneTool \
 *   --main /path/to/frames \
 *   --output /path/to/output.afma \
 *   --preset smallest_file
 * }</pre>
 *
 * <p>The tool prints planning progress, a frame-mix summary, payload totals, and final output size so encoder changes
 * can be compared quickly between revisions.
 */
public final class AfmaEncodeStandaloneTool {

    private static final long DEFAULT_FRAME_TIME_MS = 41L;

    private AfmaEncodeStandaloneTool() {
    }

    /**
     * Runs the standalone AFMA encoder.
     *
     * <p>Pass {@code --help} to print the supported arguments. The method throws on invalid arguments or encode failures
     * so isolated test runners can fail fast and surface the underlying issue directly.
     *
     * @param args command-line arguments for source directories, output path, and encoder options
     * @throws Exception if argument parsing, planning, or archive writing fails
     */
    public static void main(@NotNull String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments.help()) {
            printUsage();
            return;
        }
        if (arguments.tempDirectory() != null) {
            System.setProperty(AfmaIoHelper.TEMP_DIR_PROPERTY, arguments.tempDirectory().getPath());
        }

        File outputFile = normalizeOutputFile(arguments.outputFile());
        AfmaSourceSequence mainSequence = resolveSequence(arguments.mainFramesDirectory(), false, "main");
        AfmaSourceSequence introSequence = (arguments.introFramesDirectory() != null)
                ? resolveSequence(arguments.introFramesDirectory(), true, "intro")
                : AfmaSourceSequence.empty();
        if (mainSequence.isEmpty() && introSequence.isEmpty()) {
            throw new IllegalArgumentException("The selected source directories do not contain any PNG frame files.");
        }

        AfmaEncodeOptions options = buildOptions(arguments);
        printConfiguration(arguments, outputFile, mainSequence, introSequence, options);

        AfmaEncodePlanner planner = new AfmaEncodePlanner();
        AfmaArchiveWriter writer = new AfmaArchiveWriter();
        Instant startedAt = Instant.now();
        try (AfmaEncodePlan plan = planner.plan(
                mainSequence,
                introSequence,
                options,
                null,
                (task, progress) -> printProgress("plan", task, progress)
        )) {
            Instant plannedAt = Instant.now();
            printPlanSummary(plan, Duration.between(startedAt, plannedAt));
            writer.write(
                    plan,
                    outputFile,
                    null,
                    (path, progress) -> printProgress("write", path, progress)
            );
            Instant finishedAt = Instant.now();
            long outputBytes = Files.size(outputFile.toPath());
            System.out.println("Done.");
            System.out.println("  Output: " + outputFile.getPath().replace('\\', '/'));
            System.out.println("  Output size: " + formatBytes(outputBytes));
            System.out.println("  Write time: " + formatDuration(Duration.between(plannedAt, finishedAt)));
            System.out.println("  Total time: " + formatDuration(Duration.between(startedAt, finishedAt)));
        }
    }

    protected static void printUsage() {
        System.out.println("AFMA standalone encoder");
        System.out.println("Usage:");
        System.out.println("  --main <dir> --output <file> [options]");
        System.out.println("Options:");
        System.out.println("  --intro <dir>");
        System.out.println("  --preset <smallest_file|balanced|fastest_decode>");
        System.out.println("  --frame-time <ms>");
        System.out.println("  --intro-frame-time <ms>");
        System.out.println("  --loop-count <count>");
        System.out.println("  --keyframe-interval <frames>");
        System.out.println("  --rect-copy <true|false>");
        System.out.println("  --duplicate-elision <true|false>");
        System.out.println("  --near-lossless <true|false>");
        System.out.println("  --adaptive-keyframes <true|false>");
        System.out.println("  --adaptive-max-keyframe-interval <frames>");
        System.out.println("  --adaptive-continuation-min-savings-bytes <bytes>");
        System.out.println("  --adaptive-continuation-min-savings-ratio <ratio>");
        System.out.println("  --perceptual-visible-color-delta <value>");
        System.out.println("  --perceptual-alpha-delta <value>");
        System.out.println("  --perceptual-average-error <value>");
        System.out.println("  --max-copy-search-distance <pixels>");
        System.out.println("  --max-candidate-axis-offsets <count>");
        System.out.println("  --temp-dir <dir>");
        System.out.println("  --help");
    }

    @NotNull
    protected static AfmaEncodeOptions buildOptions(@NotNull Arguments arguments) {
        StandalonePreset preset = arguments.preset();
        AfmaEncodeOptions options = new AfmaEncodeOptions()
                .setFrameTimeMs(arguments.frameTimeMs())
                .setIntroFrameTimeMs(arguments.introFrameTimeMs())
                .setLoopCount(arguments.loopCount())
                .setKeyframeInterval(arguments.keyframeInterval())
                .setRectCopyEnabled(arguments.rectCopyEnabled())
                .setDuplicateFrameElision(arguments.duplicateFrameElision())
                .setNearLosslessMaxChannelDelta(arguments.nearLosslessEnabled() ? AfmaEncodeOptions.DEFAULT_NEAR_LOSSLESS_MAX_CHANNEL_DELTA : 0)
                .setMaxCopySearchDistance(arguments.maxCopySearchDistance())
                .setMaxCandidateAxisOffsets(arguments.maxCandidateAxisOffsets());
        applyPresetDefaults(options, arguments, preset);
        return options;
    }

    protected static void applyPresetDefaults(@NotNull AfmaEncodeOptions options,
                                              @NotNull Arguments arguments,
                                              @NotNull StandalonePreset preset) {
        int preferredKeyframeInterval = Math.max(1, options.getKeyframeInterval());
        switch (preset) {
            case SMALLEST_FILE -> options
                    .setPlannerSearchWindowFrames(24)
                    .setPlannerBeamWidth(14)
                    .setPlannerDecodeCostPenaltyBytes(80L)
                    .setPlannerComplexityPenaltyBytes(8L)
                    .setPlannerAverageDriftPenaltyBytes(24D)
                    .setPlannerVisibleColorDriftPenaltyBytes(8L)
                    .setPlannerAlphaDriftPenaltyBytes(8L)
                    .setPlannerLossyContinuationPenaltyBytes(40L)
                    .setPlannerKeyframeDistancePenaltyBytes(96L);
            case BALANCED -> options
                    .setPlannerSearchWindowFrames(16)
                    .setPlannerBeamWidth(10)
                    .setPlannerDecodeCostPenaltyBytes(144L)
                    .setPlannerComplexityPenaltyBytes(12L)
                    .setPlannerAverageDriftPenaltyBytes(40D)
                    .setPlannerVisibleColorDriftPenaltyBytes(10L)
                    .setPlannerAlphaDriftPenaltyBytes(10L)
                    .setPlannerLossyContinuationPenaltyBytes(80L)
                    .setPlannerKeyframeDistancePenaltyBytes(160L);
            case FASTEST_DECODE -> options
                    .setPlannerSearchWindowFrames(8)
                    .setPlannerBeamWidth(4)
                    .setPlannerDecodeCostPenaltyBytes(288L)
                    .setPlannerComplexityPenaltyBytes(32L)
                    .setPlannerAverageDriftPenaltyBytes(72D)
                    .setPlannerVisibleColorDriftPenaltyBytes(16L)
                    .setPlannerAlphaDriftPenaltyBytes(16L)
                    .setPlannerLossyContinuationPenaltyBytes(160L)
                    .setPlannerKeyframeDistancePenaltyBytes(256L);
        }

        options
                .setFullFrameReferencePlanEnabled(preset == StandalonePreset.SMALLEST_FILE)
                .setAdaptiveKeyframePlacement(arguments.adaptiveKeyframePlacement())
                .setAdaptiveMaxKeyframeInterval(Math.max(preferredKeyframeInterval, arguments.adaptiveMaxKeyframeInterval()))
                .setAdaptiveContinuationMinSavingsBytes(arguments.adaptiveContinuationMinSavingsBytes())
                .setAdaptiveContinuationMinSavingsRatio(arguments.adaptiveContinuationMinSavingsRatio());

        if (!arguments.nearLosslessEnabled()) {
            options
                    .setPerceptualBinIntraMaxVisibleColorDelta(0)
                    .setPerceptualBinIntraMaxAlphaDelta(0)
                    .setPerceptualBinIntraMaxAverageError(0D)
                    .setPlannerMaxCumulativeAverageError(0D)
                    .setPlannerMaxCumulativeVisibleColorDelta(0)
                    .setPlannerMaxCumulativeAlphaDelta(0)
                    .setPlannerMaxConsecutiveLossyFrames(0);
            return;
        }

        options
                .setPerceptualBinIntraMaxVisibleColorDelta(arguments.perceptualVisibleColorDelta())
                .setPerceptualBinIntraMaxAlphaDelta(arguments.perceptualAlphaDelta())
                .setPerceptualBinIntraMaxAverageError(arguments.perceptualAverageError());

        switch (preset) {
            case SMALLEST_FILE -> options
                    .setPlannerMaxCumulativeAverageError(Math.max(18D, options.getPerceptualBinIntraMaxAverageError() * 4.0D))
                    .setPlannerMaxCumulativeVisibleColorDelta(Math.max(48, options.getPerceptualBinIntraMaxVisibleColorDelta() * 4))
                    .setPlannerMaxCumulativeAlphaDelta(Math.max(80, options.getPerceptualBinIntraMaxAlphaDelta() * 3))
                    .setPlannerMaxConsecutiveLossyFrames(10);
            case BALANCED -> options
                    .setPlannerMaxCumulativeAverageError(Math.max(10D, options.getPerceptualBinIntraMaxAverageError() * 2.75D))
                    .setPlannerMaxCumulativeVisibleColorDelta(Math.max(24, options.getPerceptualBinIntraMaxVisibleColorDelta() * 3))
                    .setPlannerMaxCumulativeAlphaDelta(Math.max(48, options.getPerceptualBinIntraMaxAlphaDelta() * 2))
                    .setPlannerMaxConsecutiveLossyFrames(6);
            case FASTEST_DECODE -> options
                    .setPlannerMaxCumulativeAverageError(Math.max(6D, options.getPerceptualBinIntraMaxAverageError() * 2.0D))
                    .setPlannerMaxCumulativeVisibleColorDelta(Math.max(16, options.getPerceptualBinIntraMaxVisibleColorDelta() * 2))
                    .setPlannerMaxCumulativeAlphaDelta(Math.max(32, options.getPerceptualBinIntraMaxAlphaDelta() * 2))
                    .setPlannerMaxConsecutiveLossyFrames(3);
        }
    }

    protected static void printConfiguration(@NotNull Arguments arguments,
                                             @NotNull File outputFile,
                                             @NotNull AfmaSourceSequence mainSequence,
                                             @NotNull AfmaSourceSequence introSequence,
                                             @NotNull AfmaEncodeOptions options) {
        System.out.println("AFMA standalone encoder");
        System.out.println("  Main frames: " + describeSequence(arguments.mainFramesDirectory(), mainSequence));
        System.out.println("  Intro frames: " + ((arguments.introFramesDirectory() != null)
                ? describeSequence(arguments.introFramesDirectory(), introSequence)
                : "(disabled)"));
        System.out.println("  Output: " + outputFile.getPath().replace('\\', '/'));
        System.out.println("  Preset: " + arguments.preset().id);
        System.out.println("  Frame time: " + options.getFrameTimeMs() + " ms");
        System.out.println("  Intro frame time: " + options.getIntroFrameTimeMs() + " ms");
        System.out.println("  Loop count: " + options.getLoopCount());
        System.out.println("  Keyframe interval: " + options.getKeyframeInterval());
        System.out.println("  Rect copy: " + options.isRectCopyEnabled());
        System.out.println("  Duplicate elision: " + options.isDuplicateFrameElision());
        System.out.println("  Near-lossless: " + options.isNearLosslessEnabled());
        if (arguments.tempDirectory() != null) {
            System.out.println("  Temp dir: " + arguments.tempDirectory().getPath().replace('\\', '/'));
        }
    }

    protected static void printPlanSummary(@NotNull AfmaEncodePlan plan, @NotNull Duration planDuration) {
        System.out.println("Planning complete.");
        System.out.println("  Plan time: " + formatDuration(planDuration));
        System.out.println("  Total payload bytes: " + formatBytes(plan.getTotalPayloadBytes()));
        LinkedHashMap<String, Integer> frameCounts = new LinkedHashMap<>();
        for (AfmaFrameOperationType type : AfmaFrameOperationType.values()) {
            int count = plan.countFrames(type);
            if (count > 0) {
                frameCounts.put(type.name().toLowerCase(Locale.ROOT), count);
            }
        }
        if (!frameCounts.isEmpty()) {
            System.out.println("  Frame mix:");
            for (Map.Entry<String, Integer> entry : frameCounts.entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    protected static void printProgress(@NotNull String phase, @NotNull String task, double progress) {
        System.out.printf(Locale.ROOT, "[%s] %5.1f%% %s%n", phase, progress * 100.0D, task);
    }

    @NotNull
    protected static String describeSequence(@NotNull File directory, @NotNull AfmaSourceSequence sequence) {
        return directory.getPath().replace('\\', '/') + " (" + sequence.size() + " PNGs)";
    }

    @NotNull
    protected static AfmaSourceSequence resolveSequence(@NotNull File directory, boolean optional, @NotNull String sequenceName) {
        if (!directory.isDirectory()) {
            if (optional) {
                return AfmaSourceSequence.empty();
            }
            throw new IllegalArgumentException("The selected " + sequenceName + " directory does not exist: " + directory.getPath());
        }

        File[] files = directory.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".png"));
        if ((files == null) || (files.length == 0)) {
            if (optional) {
                return AfmaSourceSequence.empty();
            }
            throw new IllegalArgumentException("The selected " + sequenceName + " directory does not contain any PNG frame files.");
        }

        ArrayList<File> orderedFrames = new ArrayList<>(List.of(files));
        orderedFrames.sort(Comparator.comparing(File::getName, NaturalFilenameComparator.INSTANCE));
        return AfmaSourceSequence.ofFiles(orderedFrames);
    }

    @NotNull
    protected static File normalizeOutputFile(@NotNull File outputFile) {
        if (outputFile.isDirectory()) {
            throw new IllegalArgumentException("The AFMA output path must point to a file, not a directory.");
        }
        if (outputFile.getName().toLowerCase(Locale.ROOT).endsWith(".afma")) {
            return outputFile;
        }
        if (outputFile.getParentFile() != null) {
            return new File(outputFile.getParentFile(), outputFile.getName() + ".afma");
        }
        return new File(outputFile.getPath() + ".afma");
    }

    @NotNull
    protected static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kib = bytes / 1024.0D;
        if (kib < 1024.0D) {
            return String.format(Locale.ROOT, "%.2f KiB", kib);
        }
        double mib = kib / 1024.0D;
        if (mib < 1024.0D) {
            return String.format(Locale.ROOT, "%.2f MiB", mib);
        }
        return String.format(Locale.ROOT, "%.2f GiB", mib / 1024.0D);
    }

    @NotNull
    protected static String formatDuration(@NotNull Duration duration) {
        long millis = duration.toMillis();
        if (millis < 1000L) {
            return millis + " ms";
        }
        return String.format(Locale.ROOT, "%.3f s", millis / 1000.0D);
    }

    protected enum StandalonePreset {
        SMALLEST_FILE("smallest_file", 90, true, true, true, 2048, 12, true, 360, 128L, 0.0025D, 16, 32, 7.5D),
        BALANCED("balanced", 30, true, true, false, 512, 5, true, 66, 768L, 0.0075D, 10, 20, 4.0D),
        FASTEST_DECODE("fastest_decode", 18, false, true, false, 96, 2, false, 18, 0L, 0D, 8, 16, 3.0D);

        private final @NotNull String id;
        private final int keyframeInterval;
        private final boolean rectCopyEnabled;
        private final boolean duplicateFrameElision;
        private final boolean nearLosslessEnabledByDefault;
        private final int maxCopySearchDistance;
        private final int maxCandidateAxisOffsets;
        private final boolean adaptiveKeyframePlacement;
        private final int adaptiveMaxKeyframeInterval;
        private final long adaptiveContinuationMinSavingsBytes;
        private final double adaptiveContinuationMinSavingsRatio;
        private final int perceptualVisibleColorDelta;
        private final int perceptualAlphaDelta;
        private final double perceptualAverageError;

        StandalonePreset(@NotNull String id, int keyframeInterval, boolean rectCopyEnabled,
                         boolean duplicateFrameElision, boolean nearLosslessEnabledByDefault,
                         int maxCopySearchDistance, int maxCandidateAxisOffsets,
                         boolean adaptiveKeyframePlacement, int adaptiveMaxKeyframeInterval,
                         long adaptiveContinuationMinSavingsBytes, double adaptiveContinuationMinSavingsRatio,
                         int perceptualVisibleColorDelta, int perceptualAlphaDelta, double perceptualAverageError) {
            this.id = id;
            this.keyframeInterval = keyframeInterval;
            this.rectCopyEnabled = rectCopyEnabled;
            this.duplicateFrameElision = duplicateFrameElision;
            this.nearLosslessEnabledByDefault = nearLosslessEnabledByDefault;
            this.maxCopySearchDistance = maxCopySearchDistance;
            this.maxCandidateAxisOffsets = maxCandidateAxisOffsets;
            this.adaptiveKeyframePlacement = adaptiveKeyframePlacement;
            this.adaptiveMaxKeyframeInterval = adaptiveMaxKeyframeInterval;
            this.adaptiveContinuationMinSavingsBytes = adaptiveContinuationMinSavingsBytes;
            this.adaptiveContinuationMinSavingsRatio = adaptiveContinuationMinSavingsRatio;
            this.perceptualVisibleColorDelta = perceptualVisibleColorDelta;
            this.perceptualAlphaDelta = perceptualAlphaDelta;
            this.perceptualAverageError = perceptualAverageError;
        }

        @NotNull
        public static StandalonePreset parse(@Nullable String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return SMALLEST_FILE;
            }
            String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
            for (StandalonePreset preset : values()) {
                if (preset.id.equals(normalized)) {
                    return preset;
                }
            }
            throw new IllegalArgumentException("Unknown AFMA preset: " + rawValue);
        }
    }

    protected record Arguments(
            boolean help,
            @NotNull File mainFramesDirectory,
            @Nullable File introFramesDirectory,
            @NotNull File outputFile,
            @NotNull StandalonePreset preset,
            long frameTimeMs,
            long introFrameTimeMs,
            int loopCount,
            int keyframeInterval,
            boolean rectCopyEnabled,
            boolean duplicateFrameElision,
            boolean nearLosslessEnabled,
            boolean adaptiveKeyframePlacement,
            int adaptiveMaxKeyframeInterval,
            long adaptiveContinuationMinSavingsBytes,
            double adaptiveContinuationMinSavingsRatio,
            int perceptualVisibleColorDelta,
            int perceptualAlphaDelta,
            double perceptualAverageError,
            int maxCopySearchDistance,
            int maxCandidateAxisOffsets,
            @Nullable File tempDirectory
    ) {
        @NotNull
        public static Arguments parse(@NotNull String[] args) {
            Objects.requireNonNull(args);
            if ((args.length == 0) || containsHelpFlag(args)) {
                return new Arguments(
                        true,
                        new File("."),
                        null,
                        new File("animation.afma"),
                        StandalonePreset.SMALLEST_FILE,
                        DEFAULT_FRAME_TIME_MS,
                        DEFAULT_FRAME_TIME_MS,
                        0,
                        StandalonePreset.SMALLEST_FILE.keyframeInterval,
                        StandalonePreset.SMALLEST_FILE.rectCopyEnabled,
                        StandalonePreset.SMALLEST_FILE.duplicateFrameElision,
                        StandalonePreset.SMALLEST_FILE.nearLosslessEnabledByDefault,
                        StandalonePreset.SMALLEST_FILE.adaptiveKeyframePlacement,
                        StandalonePreset.SMALLEST_FILE.adaptiveMaxKeyframeInterval,
                        StandalonePreset.SMALLEST_FILE.adaptiveContinuationMinSavingsBytes,
                        StandalonePreset.SMALLEST_FILE.adaptiveContinuationMinSavingsRatio,
                        StandalonePreset.SMALLEST_FILE.perceptualVisibleColorDelta,
                        StandalonePreset.SMALLEST_FILE.perceptualAlphaDelta,
                        StandalonePreset.SMALLEST_FILE.perceptualAverageError,
                        StandalonePreset.SMALLEST_FILE.maxCopySearchDistance,
                        StandalonePreset.SMALLEST_FILE.maxCandidateAxisOffsets,
                        null
                );
            }

            Map<String, String> values = parseKeyValueArgs(args);
            StandalonePreset preset = StandalonePreset.parse(values.get("preset"));
            File mainFramesDirectory = requireFile(values, "main");
            File outputFile = requireFile(values, "output");
            return new Arguments(
                    false,
                    mainFramesDirectory,
                    optionalFile(values, "intro"),
                    outputFile,
                    preset,
                    parsePositiveLong(values.get("frame-time"), DEFAULT_FRAME_TIME_MS, "frame time"),
                    parsePositiveLong(values.get("intro-frame-time"), parsePositiveLong(values.get("frame-time"), DEFAULT_FRAME_TIME_MS, "frame time"), "intro frame time"),
                    parseNonNegativeInt(values.get("loop-count"), 0, "loop count"),
                    parsePositiveInt(values.get("keyframe-interval"), preset.keyframeInterval, "keyframe interval"),
                    parseBoolean(values.get("rect-copy"), preset.rectCopyEnabled),
                    parseBoolean(values.get("duplicate-elision"), preset.duplicateFrameElision),
                    parseBoolean(values.get("near-lossless"), preset.nearLosslessEnabledByDefault),
                    parseBoolean(values.get("adaptive-keyframes"), preset.adaptiveKeyframePlacement),
                    parsePositiveInt(values.get("adaptive-max-keyframe-interval"), preset.adaptiveMaxKeyframeInterval, "adaptive max keyframe interval"),
                    parseNonNegativeLong(values.get("adaptive-continuation-min-savings-bytes"), preset.adaptiveContinuationMinSavingsBytes, "adaptive continuation min savings bytes"),
                    parseNonNegativeDouble(values.get("adaptive-continuation-min-savings-ratio"), preset.adaptiveContinuationMinSavingsRatio, "adaptive continuation min savings ratio"),
                    parseNonNegativeInt(values.get("perceptual-visible-color-delta"), preset.perceptualVisibleColorDelta, "perceptual visible color delta"),
                    parseNonNegativeInt(values.get("perceptual-alpha-delta"), preset.perceptualAlphaDelta, "perceptual alpha delta"),
                    parseNonNegativeDouble(values.get("perceptual-average-error"), preset.perceptualAverageError, "perceptual average error"),
                    parsePositiveInt(values.get("max-copy-search-distance"), preset.maxCopySearchDistance, "max copy search distance"),
                    parsePositiveInt(values.get("max-candidate-axis-offsets"), preset.maxCandidateAxisOffsets, "max candidate axis offsets"),
                    optionalFile(values, "temp-dir")
            );
        }
    }

    protected static final class NaturalFilenameComparator implements Comparator<String> {

        protected static final @NotNull NaturalFilenameComparator INSTANCE = new NaturalFilenameComparator();
        private static final Pattern NUMBERS = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        @Override
        public int compare(String first, String second) {
            if (first == null || second == null) {
                return (first == null) ? ((second == null) ? 0 : -1) : 1;
            }

            String[] firstParts = NUMBERS.split(first);
            String[] secondParts = NUMBERS.split(second);
            int length = Math.min(firstParts.length, secondParts.length);
            for (int index = 0; index < length; index++) {
                char firstChar = firstParts[index].charAt(0);
                char secondChar = secondParts[index].charAt(0);
                int comparison = 0;
                if (Character.isDigit(firstChar) && Character.isDigit(secondChar)) {
                    comparison = new BigInteger(firstParts[index]).compareTo(new BigInteger(secondParts[index]));
                }
                if (comparison == 0) {
                    comparison = firstParts[index].compareTo(secondParts[index]);
                }
                if (comparison != 0) {
                    return comparison;
                }
            }
            return firstParts.length - secondParts.length;
        }
    }

    protected static boolean containsHelpFlag(@NotNull String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    protected static Map<String, String> parseKeyValueArgs(@NotNull String[] args) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            String arg = Objects.requireNonNull(args[index], "AFMA standalone argument was NULL");
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
            String key = arg.substring(2);
            if (key.isBlank()) {
                throw new IllegalArgumentException("Invalid empty argument key");
            }
            if ((index + 1) >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for argument --" + key);
            }
            values.put(key, args[++index]);
        }
        return values;
    }

    @NotNull
    protected static File requireFile(@NotNull Map<String, String> values, @NotNull String key) {
        File file = optionalFile(values, key);
        if (file == null) {
            throw new IllegalArgumentException("Missing required argument --" + key);
        }
        return file;
    }

    @Nullable
    protected static File optionalFile(@NotNull Map<String, String> values, @NotNull String key) {
        String rawValue = values.get(key);
        if ((rawValue == null) || rawValue.isBlank()) {
            return null;
        }
        return new File(rawValue.replace("\\", "/"));
    }

    protected static int parseInt(@Nullable String rawValue, int defaultValue, @NotNull String label) {
        if ((rawValue == null) || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + rawValue, ex);
        }
    }

    protected static int parsePositiveInt(@Nullable String rawValue, int defaultValue, @NotNull String label) {
        int value = parseInt(rawValue, defaultValue, label);
        if (value <= 0) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than 0.");
        }
        return value;
    }

    protected static int parseNonNegativeInt(@Nullable String rawValue, int defaultValue, @NotNull String label) {
        int value = parseInt(rawValue, defaultValue, label);
        if (value < 0) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than or equal to 0.");
        }
        return value;
    }

    protected static long parseLong(@Nullable String rawValue, long defaultValue, @NotNull String label) {
        if ((rawValue == null) || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + rawValue, ex);
        }
    }

    protected static long parsePositiveLong(@Nullable String rawValue, long defaultValue, @NotNull String label) {
        long value = parseLong(rawValue, defaultValue, label);
        if (value <= 0L) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than 0.");
        }
        return value;
    }

    protected static long parseNonNegativeLong(@Nullable String rawValue, long defaultValue, @NotNull String label) {
        long value = parseLong(rawValue, defaultValue, label);
        if (value < 0L) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a value greater than or equal to 0.");
        }
        return value;
    }

    protected static double parseDouble(@Nullable String rawValue, double defaultValue, @NotNull String label) {
        if ((rawValue == null) || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + rawValue, ex);
        }
    }

    protected static double parseNonNegativeDouble(@Nullable String rawValue, double defaultValue, @NotNull String label) {
        double value = parseDouble(rawValue, defaultValue, label);
        if (!Double.isFinite(value) || value < 0D) {
            throw new IllegalArgumentException("Invalid " + label + ": expected a finite value greater than or equal to 0.");
        }
        return value;
    }

    protected static boolean parseBoolean(@Nullable String rawValue, boolean defaultValue) {
        if ((rawValue == null) || rawValue.isBlank()) {
            return defaultValue;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean value: " + rawValue);
        };
    }

}
