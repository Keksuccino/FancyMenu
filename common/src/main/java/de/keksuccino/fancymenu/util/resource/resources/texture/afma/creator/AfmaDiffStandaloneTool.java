package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaAlphaResidualMode;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinIntraPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinaryFrameIndexHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaContainerV2;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaIoHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualPayload;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaResidualPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaSparsePayload;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaSparsePayloadHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Standalone AFMA frame diff helper for encoder and decoder debugging.
 *
 * <p>This tool compares decoded AFMA frames against the original PNG frame set and reports the
 * largest mismatches. It is intentionally lightweight so AFMA regressions can be analyzed
 * without loading the full FancyMenu runtime.</p>
 */
public final class AfmaDiffStandaloneTool {

    private static final Gson GSON = new GsonBuilder().create();

    private AfmaDiffStandaloneTool() {
    }

    public static void main(@NotNull String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments.help()) {
            printUsage();
            return;
        }
        if (arguments.tempDirectory() != null) {
            System.setProperty(AfmaIoHelper.TEMP_DIR_PROPERTY, arguments.tempDirectory().getPath());
        }

        File afmaFile = Objects.requireNonNull(arguments.afmaFile(), "AFMA file was NULL");
        File mainFramesDirectory = Objects.requireNonNull(arguments.mainFramesDirectory(), "Main frame directory was NULL");
        File introFramesDirectory = arguments.introFramesDirectory();
        AfmaSourceSequence mainSequence = AfmaEncodeStandaloneTool.resolveSequence(mainFramesDirectory, false, "main");
        AfmaSourceSequence introSequence = (introFramesDirectory != null)
                ? AfmaEncodeStandaloneTool.resolveSequence(introFramesDirectory, true, "intro")
                : AfmaSourceSequence.empty();

        Instant startedAt = Instant.now();
        try (ArchiveReader archiveReader = ArchiveReader.open(afmaFile)) {
            AfmaMetadata metadata = archiveReader.metadata();
            AfmaFrameIndex frameIndex = archiveReader.frameIndex();
            int canvasWidth = metadata.getCanvasWidth();
            int canvasHeight = metadata.getCanvasHeight();

            System.out.println("AFMA standalone diff");
            System.out.println("  File: " + afmaFile.getPath().replace('\\', '/'));
            System.out.println("  Size: " + AfmaEncodeStandaloneTool.formatBytes(Files.size(afmaFile.toPath())));
            System.out.println("  Canvas: " + canvasWidth + "x" + canvasHeight);

            SequenceDiffSummary introSummary = diffSequence(
                    "intro",
                    archiveReader,
                    frameIndex.getIntroFrames(),
                    introSequence,
                    canvasWidth,
                    canvasHeight,
                    arguments.topFrames()
            );
            SequenceDiffSummary mainSummary = diffSequence(
                    "main",
                    archiveReader,
                    frameIndex.getFrames(),
                    mainSequence,
                    canvasWidth,
                    canvasHeight,
                    arguments.topFrames()
            );

            printSummary("intro", introSummary);
            printSummary("main", mainSummary);
            System.out.println("  Total time: " + AfmaEncodeStandaloneTool.formatDuration(Duration.between(startedAt, Instant.now())));
        }
    }

    protected static void printUsage() {
        System.out.println("AFMA standalone diff");
        System.out.println("Usage:");
        System.out.println("  --file <archive.afma> --main <dir> [options]");
        System.out.println("Options:");
        System.out.println("  --intro <dir>");
        System.out.println("  --top <count>");
        System.out.println("  --temp-dir <dir>");
        System.out.println("  --help");
    }

    @NotNull
    protected static SequenceDiffSummary diffSequence(@NotNull String label,
                                                      @NotNull ArchiveReader archiveReader,
                                                      @NotNull List<AfmaFrameDescriptor> descriptors,
                                                      @NotNull AfmaSourceSequence sourceSequence,
                                                      int canvasWidth,
                                                      int canvasHeight,
                                                      int topFrames) throws Exception {
        ArrayList<FrameDiffResult> worstFrames = new ArrayList<>();
        AfmaFrameNormalizer frameNormalizer = new AfmaFrameNormalizer();
        int[] canvasPixels = new int[canvasWidth * canvasHeight];
        long totalDifferingPixels = 0L;
        double totalAverageError = 0D;
        int worstVisibleDelta = 0;
        int worstAlphaDelta = 0;
        FrameDiffResult firstDifferingFrame = null;
        EnumMap<AfmaFrameOperationType, FrameDiffResult> firstDifferingFrameByType = new EnumMap<>(AfmaFrameOperationType.class);

        int frameCount = Math.min(descriptors.size(), sourceSequence.size());
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            AfmaFrameDescriptor descriptor = Objects.requireNonNull(descriptors.get(frameIndex), label + " frame descriptor was NULL");
            thisDecodeFrame(archiveReader, descriptor, canvasPixels, canvasWidth, canvasHeight);

            AfmaPixelFrame sourceFrame = frameNormalizer.loadFrame(Objects.requireNonNull(sourceSequence.getFrame(frameIndex)));
            try {
                int[] decodedPixels = Arrays.copyOf(canvasPixels, canvasPixels.length);
                AfmaPixelFrame decodedFrame = new AfmaPixelFrame(canvasWidth, canvasHeight, decodedPixels);
                try {
                    FrameDiffResult result = measureFrameDiff(frameIndex, descriptor.getType(), sourceFrame, decodedFrame);
                    totalDifferingPixels += result.differingPixels();
                    totalAverageError += result.averageError();
                    worstVisibleDelta = Math.max(worstVisibleDelta, result.maxVisibleColorDelta());
                    worstAlphaDelta = Math.max(worstAlphaDelta, result.maxAlphaDelta());
                    if (result.differingPixels() > 0L) {
                        if (firstDifferingFrame == null) {
                            firstDifferingFrame = result;
                        }
                        AfmaFrameOperationType frameType = result.type();
                        if ((frameType != null) && !firstDifferingFrameByType.containsKey(frameType)) {
                            firstDifferingFrameByType.put(frameType, result);
                        }
                    }
                    insertWorstFrame(worstFrames, result, topFrames);
                } finally {
                    decodedFrame.close();
                }
            } finally {
                sourceFrame.close();
            }
        }

        return new SequenceDiffSummary(
                frameCount,
                totalDifferingPixels,
                (frameCount > 0) ? (totalAverageError / (double) frameCount) : 0D,
                worstVisibleDelta,
                worstAlphaDelta,
                firstDifferingFrame,
                Map.copyOf(firstDifferingFrameByType),
                List.copyOf(worstFrames)
        );
    }

    protected static void thisDecodeFrame(@NotNull ArchiveReader archiveReader,
                                          @NotNull AfmaFrameDescriptor descriptor,
                                          @NotNull int[] canvasPixels,
                                          int canvasWidth,
                                          int canvasHeight) throws Exception {
        AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame type was NULL");
        switch (type) {
            case FULL -> {
                byte[] payloadBytes = archiveReader.readPayloadBytes(Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                AfmaBinIntraPayloadHelper.decodePayloadIntoArgbBuffer(
                        payloadBytes,
                        0,
                        payloadBytes.length,
                        canvasPixels,
                        0,
                        canvasWidth,
                        null
                );
            }
            case SAME -> {
            }
            case DELTA_RECT -> {
                byte[] payloadBytes = archiveReader.readPayloadBytes(Objects.requireNonNull(descriptor.getPrimaryPayloadPath()));
                AfmaBinIntraPayloadHelper.decodePayloadIntoArgbBuffer(
                        payloadBytes,
                        0,
                        payloadBytes.length,
                        canvasPixels,
                        frameOffset(descriptor.getX(), descriptor.getY(), canvasWidth),
                        canvasWidth,
                        null
                );
            }
            case RESIDUAL_DELTA_RECT -> applyResidualPayload(
                    canvasPixels,
                    canvasWidth,
                    descriptor.getX(),
                    descriptor.getY(),
                    descriptor.getWidth(),
                    descriptor.getHeight(),
                    archiveReader.readPayloadBytes(Objects.requireNonNull(descriptor.getPrimaryPayloadPath())),
                    Objects.requireNonNull(descriptor.getResidual(), "AFMA residual metadata was NULL")
            );
            case SPARSE_DELTA_RECT -> applySparsePayload(
                    canvasPixels,
                    canvasWidth,
                    descriptor.getX(),
                    descriptor.getY(),
                    descriptor.getWidth(),
                    descriptor.getHeight(),
                    archiveReader.readPayloadBytes(Objects.requireNonNull(descriptor.getPrimaryPayloadPath())),
                    archiveReader.readPayloadBytes(Objects.requireNonNull(Objects.requireNonNull(descriptor.getSparse(), "AFMA sparse metadata was NULL").getPixelsPath())),
                    Objects.requireNonNull(descriptor.getSparse(), "AFMA sparse metadata was NULL")
            );
            default -> throw new UnsupportedOperationException("Standalone diff does not support frame type yet: " + type);
        }

        if (canvasPixels.length != (canvasWidth * canvasHeight)) {
            throw new IllegalStateException("AFMA canvas size changed unexpectedly");
        }
    }

    protected static void applyResidualPayload(@NotNull int[] canvasPixels,
                                               int canvasWidth,
                                               int dstX,
                                               int dstY,
                                               int width,
                                               int height,
                                               @NotNull byte[] payloadBytes,
                                               @NotNull AfmaResidualPayload residualPayload) {
        int sampleCount = width * height;
        int primaryChannels = (residualPayload.getAlphaMode() == AfmaAlphaResidualMode.FULL)
                ? residualPayload.getChannels()
                : AfmaResidualPayloadHelper.RGB_CHANNELS;
        int primaryLength = AfmaResidualPayloadHelper.resolvePrimaryStreamLength(sampleCount, residualPayload.getChannels(), residualPayload.getAlphaMode());
        AfmaResidualPayloadHelper.ResidualSampleReader primaryReader = AfmaResidualPayloadHelper.openReader(
                payloadBytes,
                0,
                primaryLength,
                sampleCount,
                primaryChannels,
                residualPayload.getCodec()
        );

        int alphaMaskOffset = AfmaResidualPayloadHelper.resolveAlphaMaskOffset(0, sampleCount, residualPayload.getChannels(), residualPayload.getAlphaMode());
        int alphaStreamOffset = AfmaResidualPayloadHelper.resolveAlphaStreamOffset(0, sampleCount, residualPayload.getChannels(), residualPayload.getAlphaMode());
        AfmaResidualPayloadHelper.ResidualSampleReader alphaReader = (residualPayload.getAlphaMode() == AfmaAlphaResidualMode.SPARSE)
                ? AfmaResidualPayloadHelper.openReader(
                payloadBytes,
                alphaStreamOffset,
                residualPayload.getAlphaChangedPixelCount(),
                residualPayload.getAlphaChangedPixelCount(),
                AfmaResidualPayloadHelper.ALPHA_ONLY_CHANNELS,
                residualPayload.getCodec()
        )
                : null;

        int sequenceIndex = 0;
        for (int localY = 0; localY < height; localY++) {
            int rowOffset = frameOffset(dstX, dstY + localY, canvasWidth);
            for (int localX = 0; localX < width; localX++, sequenceIndex++) {
                int pixelIndex = rowOffset + localX;
                int predictedColor = canvasPixels[pixelIndex];
                int updatedColor = (residualPayload.getAlphaMode() == AfmaAlphaResidualMode.FULL)
                        ? primaryReader.readNextRgbaIntoAbgr(toAbgr(predictedColor))
                        : primaryReader.readNextRgbIntoAbgr(toAbgr(predictedColor));
                if ((alphaReader != null)
                        && ((payloadBytes[alphaMaskOffset + (sequenceIndex >>> 3)] & (1 << (7 - (sequenceIndex & 7)))) != 0)) {
                    updatedColor = alphaReader.readNextAlphaIntoAbgr(updatedColor);
                }
                canvasPixels[pixelIndex] = toArgb(updatedColor);
            }
        }
    }

    protected static void applySparsePayload(@NotNull int[] canvasPixels,
                                             int canvasWidth,
                                             int dstX,
                                             int dstY,
                                             int width,
                                             int height,
                                             @NotNull byte[] layoutBytes,
                                             @NotNull byte[] residualBytes,
                                             @NotNull AfmaSparsePayload sparsePayload) throws Exception {
        int sampleCount = sparsePayload.getChangedPixelCount();
        int primaryChannels = (sparsePayload.getAlphaMode() == AfmaAlphaResidualMode.FULL)
                ? sparsePayload.getChannels()
                : AfmaResidualPayloadHelper.RGB_CHANNELS;
        int primaryLength = AfmaResidualPayloadHelper.resolvePrimaryStreamLength(sampleCount, sparsePayload.getChannels(), sparsePayload.getAlphaMode());
        AfmaResidualPayloadHelper.ResidualSampleReader primaryReader = AfmaResidualPayloadHelper.openReader(
                residualBytes,
                0,
                primaryLength,
                sampleCount,
                primaryChannels,
                sparsePayload.getResidualCodec()
        );

        int alphaMaskOffset = AfmaResidualPayloadHelper.resolveAlphaMaskOffset(0, sampleCount, sparsePayload.getChannels(), sparsePayload.getAlphaMode());
        int alphaStreamOffset = AfmaResidualPayloadHelper.resolveAlphaStreamOffset(0, sampleCount, sparsePayload.getChannels(), sparsePayload.getAlphaMode());
        AfmaResidualPayloadHelper.ResidualSampleReader alphaReader = (sparsePayload.getAlphaMode() == AfmaAlphaResidualMode.SPARSE)
                ? AfmaResidualPayloadHelper.openReader(
                residualBytes,
                alphaStreamOffset,
                sparsePayload.getAlphaChangedPixelCount(),
                sparsePayload.getAlphaChangedPixelCount(),
                AfmaResidualPayloadHelper.ALPHA_ONLY_CHANNELS,
                sparsePayload.getResidualCodec()
        )
                : null;

        AfmaSparsePayloadHelper.walkChangedPixels(
                layoutBytes,
                0,
                layoutBytes.length,
                width,
                height,
                sparsePayload.getLayoutCodec(),
                sparsePayload.getChangedPixelCount(),
                (localIndex, sequenceIndex) -> {
                    int localX = localIndex % width;
                    int localY = localIndex / width;
                    int pixelIndex = frameOffset(dstX + localX, dstY + localY, canvasWidth);
                    int predictedColor = canvasPixels[pixelIndex];
                    int updatedColor = (sparsePayload.getAlphaMode() == AfmaAlphaResidualMode.FULL)
                            ? primaryReader.readNextRgbaIntoAbgr(toAbgr(predictedColor))
                            : primaryReader.readNextRgbIntoAbgr(toAbgr(predictedColor));
                    if ((alphaReader != null)
                            && ((residualBytes[alphaMaskOffset + (sequenceIndex >>> 3)] & (1 << (7 - (sequenceIndex & 7)))) != 0)) {
                        updatedColor = alphaReader.readNextAlphaIntoAbgr(updatedColor);
                    }
                    canvasPixels[pixelIndex] = toArgb(updatedColor);
                }
        );
    }

    protected static int frameOffset(int x, int y, int width) {
        return (y * width) + x;
    }

    protected static int toAbgr(int argbColor) {
        return ((argbColor >>> 24) << 24)
                | ((argbColor & 0x00FF0000) >>> 16)
                | (argbColor & 0x0000FF00)
                | ((argbColor & 0x000000FF) << 16);
    }

    protected static int toArgb(int abgrColor) {
        return ((abgrColor >>> 24) << 24)
                | ((abgrColor & 0x00FF0000) >>> 16)
                | (abgrColor & 0x0000FF00)
                | ((abgrColor & 0x000000FF) << 16);
    }

    @NotNull
    protected static FrameDiffResult measureFrameDiff(int frameIndex,
                                                      @Nullable AfmaFrameOperationType type,
                                                      @NotNull AfmaPixelFrame sourceFrame,
                                                      @NotNull AfmaPixelFrame decodedFrame) {
        long differingPixels = 0L;
        int[] sourcePixels = sourceFrame.getPixelsUnsafe();
        int[] decodedPixels = decodedFrame.getPixelsUnsafe();
        for (int pixelIndex = 0; pixelIndex < sourcePixels.length; pixelIndex++) {
            if (sourcePixels[pixelIndex] != decodedPixels[pixelIndex]) {
                differingPixels++;
            }
        }

        AfmaFramePairAnalysis driftAnalysis = new AfmaFramePairAnalysis(sourceFrame, decodedFrame);
        AfmaFramePairAnalysis.PerceptualDriftMetrics drift = driftAnalysis.perceptualDriftMetrics();
        return new FrameDiffResult(
                frameIndex,
                type,
                differingPixels,
                drift.averageError(),
                drift.maxVisibleColorDelta(),
                drift.maxAlphaDelta()
        );
    }

    protected static void insertWorstFrame(@NotNull List<FrameDiffResult> worstFrames,
                                           @NotNull FrameDiffResult result,
                                           int maxFrames) {
        worstFrames.add(result);
        worstFrames.sort(Comparator
                .comparingLong(FrameDiffResult::differingPixels).reversed()
                .thenComparingInt(FrameDiffResult::maxVisibleColorDelta).reversed()
                .thenComparingDouble(FrameDiffResult::averageError).reversed()
                .thenComparingInt(FrameDiffResult::frameIndex));
        if (worstFrames.size() > maxFrames) {
            worstFrames.removeLast();
        }
    }

    protected static void printSummary(@NotNull String label, @NotNull SequenceDiffSummary summary) {
        System.out.println("Sequence: " + label);
        System.out.println("  Frames compared: " + summary.frameCount());
        System.out.println("  Total differing pixels: " + summary.totalDifferingPixels());
        System.out.println("  Average frame error: " + String.format(Locale.ROOT, "%.4f", summary.averageFrameError()));
        System.out.println("  Worst visible color delta: " + summary.worstVisibleColorDelta());
        System.out.println("  Worst alpha delta: " + summary.worstAlphaDelta());
        if (summary.firstDifferingFrame() != null) {
            FrameDiffResult first = summary.firstDifferingFrame();
            System.out.println("  First differing frame: #"
                    + first.frameIndex()
                    + " "
                    + String.valueOf(first.type()).toLowerCase(Locale.ROOT)
                    + " diff_pixels=" + first.differingPixels()
                    + " avg_error=" + String.format(Locale.ROOT, "%.4f", first.averageError()));
        }
        if (!summary.firstDifferingFrameByType().isEmpty()) {
            System.out.println("  First differing frame by type:");
            for (AfmaFrameOperationType frameType : AfmaFrameOperationType.values()) {
                FrameDiffResult first = summary.firstDifferingFrameByType().get(frameType);
                if (first == null) {
                    continue;
                }
                System.out.println("    " + frameType.name().toLowerCase(Locale.ROOT)
                        + " -> #"
                        + first.frameIndex()
                        + " diff_pixels=" + first.differingPixels()
                        + " avg_error=" + String.format(Locale.ROOT, "%.4f", first.averageError()));
            }
        }
        if (!summary.worstFrames().isEmpty()) {
            System.out.println("  Worst frames:");
            for (FrameDiffResult result : summary.worstFrames()) {
                System.out.println("    #" + result.frameIndex()
                        + " " + String.valueOf(result.type()).toLowerCase(Locale.ROOT)
                        + " diff_pixels=" + result.differingPixels()
                        + " avg_error=" + String.format(Locale.ROOT, "%.4f", result.averageError())
                        + " max_visible=" + result.maxVisibleColorDelta()
                        + " max_alpha=" + result.maxAlphaDelta());
            }
        }
    }

    protected record FrameDiffResult(int frameIndex,
                                     @Nullable AfmaFrameOperationType type,
                                     long differingPixels,
                                     double averageError,
                                     int maxVisibleColorDelta,
                                     int maxAlphaDelta) {
    }

    protected record SequenceDiffSummary(int frameCount,
                                         long totalDifferingPixels,
                                         double averageFrameError,
                                         int worstVisibleColorDelta,
                                         int worstAlphaDelta,
                                         @Nullable FrameDiffResult firstDifferingFrame,
                                         @NotNull Map<AfmaFrameOperationType, FrameDiffResult> firstDifferingFrameByType,
                                         @NotNull List<FrameDiffResult> worstFrames) {
    }

    protected record Arguments(@Nullable File afmaFile,
                               @Nullable File mainFramesDirectory,
                               @Nullable File introFramesDirectory,
                               @Nullable File tempDirectory,
                               int topFrames,
                               boolean help) {

        @NotNull
        protected static Arguments parse(@NotNull String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
                if (!argument.startsWith("--")) {
                    throw new IllegalArgumentException("Unexpected argument: " + argument);
                }
                String key = argument.substring(2);
                if ("help".equals(key)) {
                    values.put(key, "true");
                    continue;
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for argument --" + key);
                }
                values.put(key, args[++i]);
            }

            boolean help = Boolean.parseBoolean(values.getOrDefault("help", "false"));
            if (help) {
                return new Arguments(null, null, null, null, 10, true);
            }

            return new Arguments(
                    requiredFile(values, "file"),
                    requiredFile(values, "main"),
                    optionalFile(values, "intro"),
                    optionalFile(values, "temp-dir"),
                    parsePositiveInt(values.get("top"), 10, "top"),
                    false
            );
        }

        @NotNull
        protected static File requiredFile(@NotNull Map<String, String> values, @NotNull String key) {
            String value = values.get(key);
            if ((value == null) || value.isBlank()) {
                throw new IllegalArgumentException("Missing required argument --" + key);
            }
            return new File(value);
        }

        @Nullable
        protected static File optionalFile(@NotNull Map<String, String> values, @NotNull String key) {
            String value = values.get(key);
            return ((value == null) || value.isBlank()) ? null : new File(value);
        }

        protected static int parsePositiveInt(@Nullable String rawValue, int defaultValue, @NotNull String name) {
            if ((rawValue == null) || rawValue.isBlank()) {
                return defaultValue;
            }
            int value = Integer.parseInt(rawValue);
            if (value <= 0) {
                throw new IllegalArgumentException("AFMA " + name + " must be greater than 0");
            }
            return value;
        }
    }

    protected interface ArchiveReader extends AutoCloseable {

        @NotNull
        static ArchiveReader open(@NotNull File afmaFile) throws Exception {
            try (RandomAccessFile file = new RandomAccessFile(afmaFile, "r")) {
                int magic = (file.length() >= Integer.BYTES) ? file.readInt() : 0;
                if (AfmaContainerV2.isMagic(magic)) {
                    return new ContainerArchiveReader(afmaFile);
                }
            }
            return new ZipArchiveReader(afmaFile);
        }

        @NotNull
        AfmaMetadata metadata();

        @NotNull
        AfmaFrameIndex frameIndex();

        @NotNull
        byte[] readPayloadBytes(@NotNull String path) throws Exception;
    }

    protected static final class ContainerArchiveReader implements ArchiveReader {

        @NotNull
        private final RandomAccessFile file;
        @NotNull
        private final AfmaMetadata metadata;
        @NotNull
        private final AfmaFrameIndex frameIndex;
        @NotNull
        private final Map<String, AfmaChunkedPayloadHelper.PayloadLocator> payloadLocatorsByPath;
        @NotNull
        private final List<AfmaContainerV2.ChunkDescriptor> chunkDescriptors;
        @NotNull
        private final Map<Integer, byte[]> loadedChunks = new LinkedHashMap<>();

        protected ContainerArchiveReader(@NotNull File afmaFile) throws Exception {
            this.file = new RandomAccessFile(afmaFile, "r");
            AfmaContainerV2.Header header = AfmaContainerV2.readHeader(this.file);
            byte[] metadataBytes = readFully(this.file, header.metadataLength());
            byte[] frameIndexBytes = readFully(this.file, header.frameIndexLength());
            byte[] payloadTableBytes = readFully(this.file, header.payloadTableLength());
            this.chunkDescriptors = List.copyOf(AfmaContainerV2.readChunkDescriptors(this.file, header.chunkCount()));
            this.metadata = parseMetadata(metadataBytes);
            this.frameIndex = AfmaBinaryFrameIndexHelper.decodeFrameIndex(frameIndexBytes);
            this.payloadLocatorsByPath = AfmaPayloadArchiveLayout.decodePayloadTable(payloadTableBytes).payloadLocatorsByPath();
        }

        @Override
        public @NotNull AfmaMetadata metadata() {
            return this.metadata;
        }

        @Override
        public @NotNull AfmaFrameIndex frameIndex() {
            return this.frameIndex;
        }

        @Override
        public @NotNull byte[] readPayloadBytes(@NotNull String path) throws Exception {
            AfmaChunkedPayloadHelper.PayloadLocator locator = this.payloadLocatorsByPath.get(normalizePayloadPath(path));
            if (locator == null) {
                throw new IllegalArgumentException("Missing AFMA payload: " + path);
            }
            byte[] chunkBytes = this.loadChunk(locator.chunkId());
            return Arrays.copyOfRange(chunkBytes, locator.offset(), locator.offset() + locator.length());
        }

        @NotNull
        protected byte[] loadChunk(int chunkId) throws Exception {
            byte[] cached = this.loadedChunks.get(chunkId);
            if (cached != null) {
                return cached;
            }

            AfmaContainerV2.ChunkDescriptor descriptor = this.chunkDescriptors.get(chunkId);
            byte[] compressedBytes = new byte[descriptor.compressedLength()];
            this.file.seek(descriptor.fileOffset());
            this.file.readFully(compressedBytes);

            byte[] chunkBytes = switch (descriptor.compressionMode()) {
                case AfmaContainerV2.COMPRESSION_STORED -> {
                    if (compressedBytes.length != descriptor.uncompressedLength()) {
                        throw new IllegalStateException("Stored AFMA chunk length does not match its descriptor");
                    }
                    yield compressedBytes;
                }
                case AfmaContainerV2.COMPRESSION_RAW_DEFLATE -> inflateRawChunk(compressedBytes, descriptor.uncompressedLength());
                default -> throw new IllegalStateException("Unsupported AFMA v2 chunk compression mode: " + descriptor.compressionMode());
            };
            this.loadedChunks.put(chunkId, chunkBytes);
            return chunkBytes;
        }

        @Override
        public void close() throws Exception {
            this.file.close();
        }
    }

    protected static final class ZipArchiveReader implements ArchiveReader {

        @NotNull
        private final ZipFile zipFile;
        @NotNull
        private final AfmaMetadata metadata;
        @NotNull
        private final AfmaFrameIndex frameIndex;

        protected ZipArchiveReader(@NotNull File afmaFile) throws Exception {
            this.zipFile = new ZipFile(afmaFile);
            this.metadata = parseMetadata(readZipEntry(this.zipFile, "metadata.json"));
            this.frameIndex = AfmaBinaryFrameIndexHelper.decodeFrameIndex(readZipEntry(this.zipFile, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH));
        }

        @Override
        public @NotNull AfmaMetadata metadata() {
            return this.metadata;
        }

        @Override
        public @NotNull AfmaFrameIndex frameIndex() {
            return this.frameIndex;
        }

        @Override
        public @NotNull byte[] readPayloadBytes(@NotNull String path) throws Exception {
            return readZipEntry(this.zipFile, path);
        }

        @Override
        public void close() throws Exception {
            this.zipFile.close();
        }
    }

    @NotNull
    protected static String normalizePayloadPath(@NotNull String path) {
        return AfmaIoHelper.normalizeEntryPath(path).toLowerCase(Locale.ROOT);
    }

    @NotNull
    protected static byte[] readZipEntry(@NotNull ZipFile zipFile, @NotNull String path) throws Exception {
        ZipEntry entry = zipFile.getEntry(AfmaIoHelper.normalizeEntryPath(path));
        if (entry == null) {
            throw new IllegalArgumentException("Missing AFMA ZIP entry: " + path);
        }
        try (InputStream in = zipFile.getInputStream(entry);
             ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, (int) Math.max(0L, entry.getSize())))) {
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    @NotNull
    protected static byte[] readFully(@NotNull RandomAccessFile file, int length) throws Exception {
        byte[] bytes = new byte[length];
        file.readFully(bytes);
        return bytes;
    }

    @NotNull
    protected static byte[] inflateRawChunk(@NotNull byte[] compressedBytes, int expectedLength) throws Exception {
        Inflater inflater = new Inflater(true);
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(compressedBytes);
             InflaterInputStream inflaterIn = new InflaterInputStream(byteIn, inflater);
             ByteArrayOutputStream out = new ByteArrayOutputStream(expectedLength)) {
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int read;
            while ((read = inflaterIn.read(buffer)) >= 0) {
                totalBytes += read;
                if (totalBytes > expectedLength) {
                    throw new IllegalStateException("AFMA raw-deflate chunk exceeded its declared uncompressed length");
                }
                out.write(buffer, 0, read);
            }
            byte[] inflatedBytes = out.toByteArray();
            if (inflatedBytes.length != expectedLength) {
                throw new IllegalStateException("AFMA raw-deflate chunk length does not match its descriptor");
            }
            return inflatedBytes;
        } finally {
            inflater.end();
        }
    }

    @NotNull
    protected static AfmaMetadata parseMetadata(@NotNull byte[] metadataBytes) throws Exception {
        String metadataString = new String(Objects.requireNonNull(metadataBytes), StandardCharsets.UTF_8);
        if (metadataString.trim().isEmpty()) {
            throw new IllegalArgumentException("metadata.json of AFMA file is empty");
        }
        AfmaMetadata parsedMetadata = GSON.fromJson(metadataString, AfmaMetadata.class);
        if (parsedMetadata == null) {
            throw new IllegalArgumentException("Unable to parse metadata.json of AFMA file");
        }
        parsedMetadata.validate();
        return parsedMetadata;
    }
}
