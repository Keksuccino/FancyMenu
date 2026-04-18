package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaBinaryFrameIndexHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaChunkedPayloadHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaContainerV2;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameDescriptor;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameIndex;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaFrameOperationType;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaIoHelper;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaPayloadArchiveLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Standalone AFMA inspection helper for encoder debugging.
 *
 * <p>This is meant for quick command-line analysis of generated AFMA files without loading the full mod or
 * decoding textures onto the GPU. It summarizes frame-operation usage, payload bytes per operation family, and
 * the exact frame indexes for each operation so encoder regressions can be compared between revisions.</p>
 */
public final class AfmaInspectStandaloneTool {

    private AfmaInspectStandaloneTool() {
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
        if (!afmaFile.isFile()) {
            throw new IllegalArgumentException("The selected AFMA file does not exist: " + afmaFile.getPath());
        }

        System.out.println("AFMA standalone inspector");
        System.out.println("  File: " + afmaFile.getPath().replace('\\', '/'));
        System.out.println("  Size: " + formatBytes(Files.size(afmaFile.toPath())));

        try (ArchiveSummary archiveSummary = readArchiveSummary(afmaFile)) {
            SequenceSummary introSummary = summarizeSequence(archiveSummary.payloadLengthsByPath(), archiveSummary.frameIndex().getIntroFrames());
            SequenceSummary mainSummary = summarizeSequence(archiveSummary.payloadLengthsByPath(), archiveSummary.frameIndex().getFrames());
            printSequenceSummary("intro", introSummary, arguments.showIndexes());
            printSequenceSummary("main", mainSummary, arguments.showIndexes());
            printCombinedSummary(introSummary, mainSummary);
        }
    }

    protected static void printUsage() {
        System.out.println("AFMA standalone inspector");
        System.out.println("Usage:");
        System.out.println("  --file <archive.afma> [options]");
        System.out.println("Options:");
        System.out.println("  --show-indexes <true|false>");
        System.out.println("  --temp-dir <dir>");
        System.out.println("  --help");
    }

    @NotNull
    protected static SequenceSummary summarizeSequence(@NotNull Map<String, Integer> payloadLengthsByPath,
                                                       @NotNull List<AfmaFrameDescriptor> descriptors) throws Exception {
        EnumMap<AfmaFrameOperationType, Integer> counts = new EnumMap<>(AfmaFrameOperationType.class);
        EnumMap<AfmaFrameOperationType, Long> payloadBytes = new EnumMap<>(AfmaFrameOperationType.class);
        EnumMap<AfmaFrameOperationType, List<Integer>> indexes = new EnumMap<>(AfmaFrameOperationType.class);
        for (int i = 0; i < descriptors.size(); i++) {
            AfmaFrameDescriptor descriptor = Objects.requireNonNull(descriptors.get(i), "AFMA frame descriptor was NULL");
            AfmaFrameOperationType type = Objects.requireNonNull(descriptor.getType(), "AFMA frame type was NULL");
            counts.merge(type, 1, Integer::sum);
            indexes.computeIfAbsent(type, ignored -> new ArrayList<>()).add(i);
            long bytes = resolvePayloadBytes(payloadLengthsByPath, descriptor);
            if (bytes > 0L) {
                payloadBytes.merge(type, bytes, Long::sum);
            }
        }
        return new SequenceSummary(descriptors.size(), counts, payloadBytes, indexes);
    }

    protected static long resolvePayloadBytes(@NotNull Map<String, Integer> payloadLengthsByPath,
                                              @NotNull AfmaFrameDescriptor descriptor) throws Exception {
        long bytes = 0L;
        String primaryPath = descriptor.getPath();
        if ((primaryPath != null) && !primaryPath.isBlank()) {
            bytes += payloadLengthsByPath.getOrDefault(primaryPath.toLowerCase(Locale.ROOT), 0);
        }
        if (descriptor.getPatch() != null) {
            String patchPath = descriptor.getPatch().getPath();
            if ((patchPath != null) && !patchPath.isBlank()) {
                bytes += payloadLengthsByPath.getOrDefault(patchPath.toLowerCase(Locale.ROOT), 0);
            }
        }
        return bytes;
    }

    protected static void printSequenceSummary(@NotNull String label,
                                               @NotNull SequenceSummary summary,
                                               boolean showIndexes) {
        System.out.println("Sequence: " + label);
        System.out.println("  Frames: " + summary.frameCount());
        if (summary.counts().isEmpty()) {
            System.out.println("  Frame mix: (empty)");
            return;
        }
        System.out.println("  Frame mix:");
        for (AfmaFrameOperationType type : AfmaFrameOperationType.values()) {
            Integer count = summary.counts().get(type);
            if (count == null || count <= 0) {
                continue;
            }
            long payloadBytes = summary.payloadBytes().getOrDefault(type, 0L);
            System.out.println("    " + type.name().toLowerCase(Locale.ROOT)
                    + ": " + count
                    + " frames, payload " + formatBytes(payloadBytes));
            if (showIndexes) {
                System.out.println("      indexes: " + formatIndexes(summary.indexes().get(type)));
            }
        }
    }

    protected static void printCombinedSummary(@NotNull SequenceSummary introSummary,
                                               @NotNull SequenceSummary mainSummary) {
        EnumMap<AfmaFrameOperationType, Integer> counts = new EnumMap<>(AfmaFrameOperationType.class);
        EnumMap<AfmaFrameOperationType, Long> payloadBytes = new EnumMap<>(AfmaFrameOperationType.class);
        mergeSummary(counts, payloadBytes, introSummary);
        mergeSummary(counts, payloadBytes, mainSummary);

        System.out.println("Combined:");
        System.out.println("  Total frames: " + (introSummary.frameCount() + mainSummary.frameCount()));
        for (AfmaFrameOperationType type : AfmaFrameOperationType.values()) {
            Integer count = counts.get(type);
            if (count == null || count <= 0) {
                continue;
            }
            System.out.println("    " + type.name().toLowerCase(Locale.ROOT)
                    + ": " + count
                    + " frames, payload " + formatBytes(payloadBytes.getOrDefault(type, 0L)));
        }
    }

    protected static void mergeSummary(@NotNull EnumMap<AfmaFrameOperationType, Integer> counts,
                                       @NotNull EnumMap<AfmaFrameOperationType, Long> payloadBytes,
                                       @NotNull SequenceSummary summary) {
        for (Map.Entry<AfmaFrameOperationType, Integer> entry : summary.counts().entrySet()) {
            counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<AfmaFrameOperationType, Long> entry : summary.payloadBytes().entrySet()) {
            payloadBytes.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
    }

    @NotNull
    protected static ArchiveSummary readArchiveSummary(@NotNull File afmaFile) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(afmaFile, "r")) {
            int magic = (file.length() >= Integer.BYTES) ? file.readInt() : 0;
            if (AfmaContainerV2.isMagic(magic)) {
                file.seek(0L);
                AfmaContainerV2.Header header = AfmaContainerV2.readHeader(file);
                readFully(file, header.metadataLength());
                byte[] frameIndexBytes = readFully(file, header.frameIndexLength());
                byte[] payloadTableBytes = readFully(file, header.payloadTableLength());
                AfmaContainerV2.readChunkDescriptors(file, header.chunkCount());
                AfmaFrameIndex frameIndex = AfmaBinaryFrameIndexHelper.decodeFrameIndex(frameIndexBytes);
                AfmaPayloadArchiveLayout.DecodedPayloadTable payloadTable = AfmaPayloadArchiveLayout.decodePayloadTable(payloadTableBytes);
                Map<String, Integer> payloadLengthsByPath = new java.util.LinkedHashMap<>(payloadTable.payloadLocatorsByPath().size());
                for (Map.Entry<String, AfmaChunkedPayloadHelper.PayloadLocator> entry : payloadTable.payloadLocatorsByPath().entrySet()) {
                    payloadLengthsByPath.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().length());
                }
                return new ArchiveSummary(frameIndex, payloadLengthsByPath);
            }
        }

        try (ZipFile zipFile = new ZipFile(afmaFile)) {
            if (zipFile.getEntry(AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH) == null) {
                throw new IllegalArgumentException("Legacy AFMA ZIP inspection is not supported by this tool yet.");
            }
            byte[] frameIndexBytes = readZipEntry(zipFile, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH);
            byte[] payloadTableBytes = readZipEntry(zipFile, AfmaChunkedPayloadHelper.PAYLOAD_INDEX_ENTRY_PATH);
            AfmaFrameIndex frameIndex = AfmaBinaryFrameIndexHelper.decodeFrameIndex(frameIndexBytes);
            AfmaChunkedPayloadHelper.DecodedPayloadIndex payloadIndex = AfmaChunkedPayloadHelper.decodePayloadIndex(payloadTableBytes);
            Map<String, Integer> payloadLengthsByPath = new java.util.LinkedHashMap<>(payloadIndex.payloadLocatorsByPath().size());
            for (Map.Entry<String, AfmaChunkedPayloadHelper.PayloadLocator> entry : payloadIndex.payloadLocatorsByPath().entrySet()) {
                payloadLengthsByPath.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().length());
            }
            return new ArchiveSummary(frameIndex, payloadLengthsByPath);
        }
    }

    @NotNull
    protected static byte[] readZipEntry(@NotNull ZipFile zipFile, @NotNull String path) throws Exception {
        ZipEntry entry = zipFile.getEntry(path);
        if (entry == null) {
            throw new IllegalArgumentException("Missing AFMA ZIP entry: " + path);
        }
        try (InputStream in = zipFile.getInputStream(entry);
             ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, (int) entry.getSize()))) {
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
    protected static String formatIndexes(@Nullable List<Integer> indexes) {
        if (indexes == null || indexes.isEmpty()) {
            return "(none)";
        }
        StringBuilder builder = new StringBuilder();
        int rangeStart = indexes.getFirst();
        int previous = rangeStart;
        for (int i = 1; i < indexes.size(); i++) {
            int current = indexes.get(i);
            if (current == previous + 1) {
                previous = current;
                continue;
            }
            appendRange(builder, rangeStart, previous);
            rangeStart = current;
            previous = current;
        }
        appendRange(builder, rangeStart, previous);
        return builder.toString();
    }

    protected static void appendRange(@NotNull StringBuilder builder, int start, int end) {
        if (!builder.isEmpty()) {
            builder.append(", ");
        }
        if (start == end) {
            builder.append(start);
        } else {
            builder.append(start).append('-').append(end);
        }
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

    protected record SequenceSummary(int frameCount,
                                     @NotNull EnumMap<AfmaFrameOperationType, Integer> counts,
                                     @NotNull EnumMap<AfmaFrameOperationType, Long> payloadBytes,
                                     @NotNull EnumMap<AfmaFrameOperationType, List<Integer>> indexes) {
    }

    protected record ArchiveSummary(@NotNull AfmaFrameIndex frameIndex,
                                    @NotNull Map<String, Integer> payloadLengthsByPath) implements AutoCloseable {

        @Override
        public void close() {
        }
    }

    protected record Arguments(@Nullable File afmaFile, boolean showIndexes, @Nullable File tempDirectory, boolean help) {

        @NotNull
        protected static Arguments parse(@NotNull String[] args) {
            File afmaFile = null;
            boolean showIndexes = false;
            File tempDirectory = null;
            for (int i = 0; i < args.length; i++) {
                String arg = Objects.requireNonNull(args[i], "Command-line argument was NULL");
                switch (arg) {
                    case "--file" -> afmaFile = new File(requireValue(args, ++i, arg));
                    case "--show-indexes" -> showIndexes = Boolean.parseBoolean(requireValue(args, ++i, arg));
                    case "--temp-dir" -> tempDirectory = new File(requireValue(args, ++i, arg));
                    case "--help", "-h" -> {
                        return new Arguments(null, showIndexes, tempDirectory, true);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            if (afmaFile == null) {
                throw new IllegalArgumentException("Missing required argument: --file <archive.afma>");
            }
            return new Arguments(afmaFile, showIndexes, tempDirectory, false);
        }

        @NotNull
        protected static String requireValue(@NotNull String[] args, int index, @NotNull String flag) {
            if (index < 0 || index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            return Objects.requireNonNull(args[index], "Command-line argument value was NULL");
        }
    }
}
