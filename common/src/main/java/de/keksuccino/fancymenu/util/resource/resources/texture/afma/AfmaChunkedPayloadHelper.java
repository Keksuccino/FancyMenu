package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.Deflater;

public final class AfmaChunkedPayloadHelper {

    public static final String PAYLOAD_INDEX_ENTRY_PATH = "payload_index.bin";
    public static final String PAYLOAD_CHUNK_DIRECTORY = "payload_chunks/";
    public static final String PAYLOAD_CHUNK_EXTENSION = ".bin";
    public static final int PAYLOAD_INDEX_MAGIC = 0x41465049; // AFPI
    public static final int PAYLOAD_INDEX_VERSION = 1;
    public static final int TARGET_CHUNK_BYTES = 256 * 1024;
    public static final int[] TARGET_CHUNK_BYTE_CANDIDATES = {
            128 * 1024,
            160 * 1024,
            192 * 1024,
            224 * 1024,
            TARGET_CHUNK_BYTES
    };
    public static final int SEQUENCE_KIND_INTRO = 0;
    public static final int SEQUENCE_KIND_MAIN = 1;
    public static final int PLANNER_DEFLATE_TAIL_BYTES = 32 * 1024;
    protected static final int ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES = 96;
    protected static final int SAME_FRAME_EDGE_WEIGHT = 10;
    protected static final int CONSECUTIVE_FRAME_EDGE_WEIGHT = 5;
    protected static final int CHUNK_CACHE_MISS_PENALTY_BYTES = 1024;
    protected static final int MULTI_CHUNK_FRAME_PENALTY_BYTES = 768;
    protected static final byte[] EMPTY_BYTES = new byte[0];

    private AfmaChunkedPayloadHelper() {
    }

    @NotNull
    public static PackedPayloadArchive buildArchiveLayout(@NotNull Map<String, byte[]> payloads) {
        return buildArchiveLayout(payloads, ArchivePackingHints.empty());
    }

    @NotNull
    public static PackedPayloadArchive buildArchiveLayout(@NotNull Map<String, byte[]> payloads, @Nullable ArchivePackingHints packingHints) {
        return buildArchiveLayoutInternal(payloads, packingHints, false);
    }

    @NotNull
    public static PackedPayloadArchive simulateArchiveLayout(@NotNull Map<String, byte[]> payloads, @Nullable ArchivePackingHints packingHints) {
        return buildArchiveLayoutInternal(payloads, packingHints, true);
    }

    @NotNull
    public static ArchivePackingHints buildPackingHints(@NotNull AfmaFrameIndex frameIndex, int loopCount) {
        Objects.requireNonNull(frameIndex);
        ArrayList<PayloadAccessFrame> accessFrames = new ArrayList<>();
        int nextRegionId = 0;
        nextRegionId = appendSequenceAccessFrames(accessFrames, frameIndex.getIntroFrames(), SEQUENCE_KIND_INTRO, nextRegionId);
        nextRegionId = appendSequenceAccessFrames(accessFrames, frameIndex.getFrames(), SEQUENCE_KIND_MAIN, nextRegionId);
        if (shouldRepeatMainAccessTrace(frameIndex.getFrames(), loopCount)) {
            appendSequenceAccessFrames(accessFrames, frameIndex.getFrames(), SEQUENCE_KIND_MAIN, nextRegionId);
        }
        return new ArchivePackingHints(accessFrames);
    }

    @NotNull
    public static PayloadAccessFrame createAccessFrame(@NotNull AfmaFrameDescriptor descriptor, int keyframeRegionId, int sequenceKind) {
        Objects.requireNonNull(descriptor);
        ArrayList<String> payloadPaths = new ArrayList<>(2);
        addAccessPayloadPath(payloadPaths, descriptor.getPrimaryPayloadPath());
        addAccessPayloadPath(payloadPaths, descriptor.getSecondaryPayloadPath());
        return new PayloadAccessFrame(payloadPaths, keyframeRegionId, sequenceKind);
    }

    public static int estimateVarIntBytes(int value) {
        if ((value & ~0x7F) == 0) {
            return 1;
        }
        if ((value & ~0x3FFF) == 0) {
            return 2;
        }
        if ((value & ~0x1FFFFF) == 0) {
            return 3;
        }
        if ((value & ~0xFFFFFFF) == 0) {
            return 4;
        }
        return 5;
    }

    public static long estimateChunkCompressionDelta(@NotNull byte[] previousTail, @NotNull byte[] payloadBytes) {
        Objects.requireNonNull(previousTail);
        Objects.requireNonNull(payloadBytes);
        if (payloadBytes.length == 0) {
            return 0L;
        }
        if (previousTail.length == 0) {
            return AfmaPayloadMetricsHelper.estimateArchiveBytes(payloadBytes);
        }

        byte[] combinedBytes = new byte[previousTail.length + payloadBytes.length];
        System.arraycopy(previousTail, 0, combinedBytes, 0, previousTail.length);
        System.arraycopy(payloadBytes, 0, combinedBytes, previousTail.length, payloadBytes.length);
        long combinedEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(combinedBytes);
        long tailEstimate = AfmaPayloadMetricsHelper.estimateArchiveBytes(previousTail);
        return Math.max(0L, combinedEstimate - tailEstimate);
    }

    @NotNull
    public static byte[] appendDeflateTail(@NotNull byte[] currentTail, @NotNull byte[] payloadBytes) {
        Objects.requireNonNull(currentTail);
        Objects.requireNonNull(payloadBytes);
        if (payloadBytes.length == 0) {
            return currentTail;
        }
        int resultLength = Math.min(PLANNER_DEFLATE_TAIL_BYTES, currentTail.length + payloadBytes.length);
        byte[] resultTail = new byte[resultLength];
        int payloadBytesInTail = Math.min(payloadBytes.length, resultLength);
        System.arraycopy(payloadBytes, payloadBytes.length - payloadBytesInTail, resultTail, resultLength - payloadBytesInTail, payloadBytesInTail);
        int carriedPrefixBytes = resultLength - payloadBytesInTail;
        if (carriedPrefixBytes > 0) {
            System.arraycopy(currentTail, currentTail.length - carriedPrefixBytes, resultTail, 0, carriedPrefixBytes);
        }
        return resultTail;
    }

    @NotNull
    protected static PackedPayloadArchive buildArchiveLayoutInternal(@NotNull Map<String, byte[]> payloads,
                                                                    @Nullable ArchivePackingHints packingHints,
                                                                    boolean approximateCompression) {
        Objects.requireNonNull(payloads);
        LinkedHashMap<String, PayloadData> payloadDataByPath = new LinkedHashMap<>();
        int insertionOrder = 0;
        for (Map.Entry<String, byte[]> entry : payloads.entrySet()) {
            String payloadPath = Objects.requireNonNull(entry.getKey());
            byte[] payloadBytes = Objects.requireNonNull(entry.getValue(), "AFMA payload bytes were NULL for " + payloadPath);
            payloadDataByPath.put(payloadPath, new PayloadData(payloadPath, payloadBytes, insertionOrder++));
        }

        if (payloadDataByPath.isEmpty()) {
            return new PackedPayloadArchive(
                    Collections.unmodifiableMap(new LinkedHashMap<String, Integer>()),
                    List.of(),
                    List.of(),
                    PackingMetrics.empty(TARGET_CHUNK_BYTES)
            );
        }

        ArchivePackingHints effectiveHints = resolveArchivePackingHints(payloadDataByPath, packingHints);
        PackingModel packingModel = buildPackingModel(payloadDataByPath, effectiveHints);

        PackedPayloadArchive bestArchive = null;
        long bestScore = Long.MAX_VALUE;
        for (int targetChunkBytes : TARGET_CHUNK_BYTE_CANDIDATES) {
            PackedPayloadArchive candidate = packPayloads(payloadDataByPath, packingModel, targetChunkBytes, approximateCompression);
            long candidateScore = candidate.packingMetrics().scoredArchiveBytes();
            if (bestArchive == null || candidateScore < bestScore
                    || ((candidateScore == bestScore) && candidate.packingMetrics().predictedArchiveBytes() < bestArchive.packingMetrics().predictedArchiveBytes())) {
                bestArchive = candidate;
                bestScore = candidateScore;
            }
        }

        return Objects.requireNonNull(bestArchive);
    }

    @NotNull
    public static byte[] encodePayloadIndex(@NotNull PackedPayloadArchive archiveLayout) throws IOException {
        Objects.requireNonNull(archiveLayout);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeInt(PAYLOAD_INDEX_MAGIC);
            out.writeByte(PAYLOAD_INDEX_VERSION);
            writeVarInt(out, archiveLayout.chunkPlans().size());
            writeVarInt(out, archiveLayout.payloadLocators().size());
            for (ChunkPlan chunkPlan : archiveLayout.chunkPlans()) {
                writeVarInt(out, chunkPlan.uncompressedLength());
            }
            for (PayloadLocator payloadLocator : archiveLayout.payloadLocators()) {
                writeVarInt(out, payloadLocator.chunkId());
                writeVarInt(out, payloadLocator.offset());
                writeVarInt(out, payloadLocator.length());
            }
            out.flush();
            return byteStream.toByteArray();
        }
    }

    @NotNull
    public static DecodedPayloadIndex decodePayloadIndex(@NotNull byte[] payloadIndexBytes) throws IOException {
        Objects.requireNonNull(payloadIndexBytes);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payloadIndexBytes))) {
            int magic = in.readInt();
            if (magic != PAYLOAD_INDEX_MAGIC) {
                throw new IOException("AFMA payload index is missing its magic header");
            }

            int version = in.readUnsignedByte();
            if (version != PAYLOAD_INDEX_VERSION) {
                throw new IOException("Unsupported AFMA payload index version: " + version);
            }

            int chunkCount = readVarInt(in);
            int payloadCount = readVarInt(in);
            if (chunkCount < 0 || payloadCount < 0) {
                throw new IOException("AFMA payload index has invalid counts");
            }

            int[] chunkLengths = new int[chunkCount];
            for (int i = 0; i < chunkCount; i++) {
                chunkLengths[i] = readVarInt(in);
                if (chunkLengths[i] < 0) {
                    throw new IOException("AFMA payload chunk " + i + " has an invalid uncompressed length");
                }
            }

            LinkedHashMap<String, PayloadLocator> payloadLocatorsByPath = new LinkedHashMap<>();
            for (int payloadId = 0; payloadId < payloadCount; payloadId++) {
                int chunkId = readVarInt(in);
                int offset = readVarInt(in);
                int length = readVarInt(in);
                if (chunkId < 0 || chunkId >= chunkCount) {
                    throw new IOException("AFMA payload " + payloadId + " references an invalid payload chunk");
                }
                if (offset < 0 || length < 0 || ((long) offset + (long) length) > chunkLengths[chunkId]) {
                    throw new IOException("AFMA payload " + payloadId + " has an invalid chunk range");
                }
                payloadLocatorsByPath.put(syntheticPayloadPath(payloadId).toLowerCase(Locale.ROOT), new PayloadLocator(chunkId, offset, length));
            }

            if (in.available() > 0) {
                throw new IOException("AFMA payload index contains trailing data");
            }

            return new DecodedPayloadIndex(payloadLocatorsByPath, chunkLengths);
        }
    }

    @NotNull
    public static String syntheticPayloadPath(int payloadId) {
        if (payloadId < 0) {
            throw new IllegalArgumentException("AFMA payload id is negative: " + payloadId);
        }
        return "payload/" + Integer.toUnsignedString(payloadId, 36);
    }

    @NotNull
    public static String chunkEntryPath(int chunkId) {
        if (chunkId < 0) {
            throw new IllegalArgumentException("AFMA chunk id is negative: " + chunkId);
        }
        return PAYLOAD_CHUNK_DIRECTORY + "chunk_" + Integer.toUnsignedString(chunkId, 36) + PAYLOAD_CHUNK_EXTENSION;
    }

    public static boolean isWholeChunkPayload(@NotNull PayloadLocator payloadLocator, int[] chunkLengths) {
        Objects.requireNonNull(payloadLocator);
        Objects.requireNonNull(chunkLengths);
        int chunkId = payloadLocator.chunkId();
        return chunkId >= 0
                && chunkId < chunkLengths.length
                && payloadLocator.offset() == 0
                && payloadLocator.length() == chunkLengths[chunkId];
    }

    protected static void addChunkPlan(@NotNull List<ChunkPlan> chunkPlans, @NotNull List<String> chunkPayloadPaths, int chunkLength) {
        int chunkId = chunkPlans.size();
        chunkPlans.add(new ChunkPlan(chunkEntryPath(chunkId), List.copyOf(chunkPayloadPaths), chunkLength));
    }

    protected static boolean shouldRepeatMainAccessTrace(@NotNull List<AfmaFrameDescriptor> mainFrames, int loopCount) {
        return !mainFrames.isEmpty() && mainFrames.size() > 1 && loopCount != 1;
    }

    protected static int appendSequenceAccessFrames(@NotNull List<PayloadAccessFrame> accessFrames, @NotNull List<AfmaFrameDescriptor> sequence,
                                                    int sequenceKind, int nextRegionId) {
        if (sequence.isEmpty()) {
            return nextRegionId;
        }

        int currentRegionId = -1;
        for (AfmaFrameDescriptor descriptor : sequence) {
            AfmaFrameDescriptor resolvedDescriptor = Objects.requireNonNull(descriptor);
            if (currentRegionId < 0 || resolvedDescriptor.isKeyframe()) {
                currentRegionId = nextRegionId++;
            }
            accessFrames.add(createAccessFrame(resolvedDescriptor, currentRegionId, sequenceKind));
        }
        return nextRegionId;
    }

    protected static void addAccessPayloadPath(@NotNull List<String> payloadPaths, @Nullable String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        for (String existingPath : payloadPaths) {
            if (existingPath.equals(path)) {
                return;
            }
        }
        payloadPaths.add(path);
    }

    @NotNull
    protected static ArchivePackingHints resolveArchivePackingHints(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                                   @Nullable ArchivePackingHints packingHints) {
        if (packingHints != null && !packingHints.isEmpty()) {
            return packingHints;
        }

        ArrayList<PayloadAccessFrame> fallbackFrames = new ArrayList<>(payloadDataByPath.size());
        for (String payloadPath : payloadDataByPath.keySet()) {
            fallbackFrames.add(new PayloadAccessFrame(List.of(payloadPath), 0, SEQUENCE_KIND_MAIN));
        }
        return new ArchivePackingHints(fallbackFrames);
    }

    @NotNull
    protected static PackingModel buildPackingModel(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                    @NotNull ArchivePackingHints packingHints) {
        LinkedHashMap<String, PayloadPackingStats> statsByPath = new LinkedHashMap<>();
        HashMap<String, String> actualPathsByNormalized = new HashMap<>();
        for (PayloadData payloadData : payloadDataByPath.values()) {
            statsByPath.put(payloadData.path(), new PayloadPackingStats(payloadData));
            actualPathsByNormalized.put(normalizePayloadPath(payloadData.path()), payloadData.path());
        }

        HashMap<String, Map<String, Integer>> edgeWeightsByPath = new HashMap<>();
        ArrayList<PayloadAccessFrame> resolvedAccessFrames = new ArrayList<>(packingHints.accessFrames().size());
        PayloadAccessFrame previousFrame = null;
        int accessIndex = 0;
        for (PayloadAccessFrame accessFrame : packingHints.accessFrames()) {
            ArrayList<String> resolvedPayloadPaths = new ArrayList<>(accessFrame.payloadPaths().size());
            for (String payloadPath : accessFrame.payloadPaths()) {
                String actualPath = actualPathsByNormalized.get(normalizePayloadPath(payloadPath));
                if (actualPath != null && !resolvedPayloadPaths.contains(actualPath)) {
                    resolvedPayloadPaths.add(actualPath);
                }
            }

            PayloadAccessFrame resolvedFrame = new PayloadAccessFrame(resolvedPayloadPaths, accessFrame.keyframeRegionId(), accessFrame.sequenceKind());
            resolvedAccessFrames.add(resolvedFrame);

            for (String payloadPath : resolvedPayloadPaths) {
                PayloadPackingStats stats = statsByPath.get(payloadPath);
                if (stats != null) {
                    stats.recordAccess(accessIndex, accessFrame.keyframeRegionId(), accessFrame.sequenceKind());
                }
            }

            for (int i = 0; i < resolvedPayloadPaths.size(); i++) {
                for (int j = i + 1; j < resolvedPayloadPaths.size(); j++) {
                    addEdgeWeight(edgeWeightsByPath, resolvedPayloadPaths.get(i), resolvedPayloadPaths.get(j), SAME_FRAME_EDGE_WEIGHT);
                }
            }

            if (previousFrame != null) {
                for (String previousPath : previousFrame.payloadPaths()) {
                    for (String currentPath : resolvedPayloadPaths) {
                        addEdgeWeight(edgeWeightsByPath, previousPath, currentPath, CONSECUTIVE_FRAME_EDGE_WEIGHT);
                    }
                }
            }

            previousFrame = resolvedFrame;
            accessIndex++;
        }

        return new PackingModel(List.copyOf(resolvedAccessFrames), statsByPath, edgeWeightsByPath);
    }

    protected static void addEdgeWeight(@NotNull Map<String, Map<String, Integer>> edgeWeightsByPath,
                                        @NotNull String firstPath, @NotNull String secondPath, int weight) {
        if (weight <= 0 || firstPath.equals(secondPath)) {
            return;
        }
        edgeWeightsByPath.computeIfAbsent(firstPath, key -> new HashMap<>()).merge(secondPath, weight, Integer::sum);
        edgeWeightsByPath.computeIfAbsent(secondPath, key -> new HashMap<>()).merge(firstPath, weight, Integer::sum);
    }

    @NotNull
    protected static PackedPayloadArchive packPayloads(@NotNull Map<String, PayloadData> payloadDataByPath,
                                                       @NotNull PackingModel packingModel,
                                                       int targetChunkBytes,
                                                       boolean approximateCompression) {
        ArrayList<String> orderedPayloadPaths = new ArrayList<>(payloadDataByPath.keySet());
        orderedPayloadPaths.sort(Comparator
                .comparing((String path) -> !packingModel.statsByPath().get(path).wasAccessed())
                .thenComparingDouble(path -> packingModel.statsByPath().get(path).center())
                .thenComparingInt(path -> packingModel.statsByPath().get(path).dominantRegionId())
                .thenComparingInt(path -> payloadDataByPath.get(path).insertionOrder()));

        LinkedHashSet<String> remainingPayloadPaths = new LinkedHashSet<>(orderedPayloadPaths);
        ArrayList<ChunkPlanBuilder> chunkBuilders = new ArrayList<>();
        while (!remainingPayloadPaths.isEmpty()) {
            String seedPayloadPath = Objects.requireNonNull(remainingPayloadPaths.iterator().next());
            remainingPayloadPaths.remove(seedPayloadPath);

            PayloadData seedPayload = Objects.requireNonNull(payloadDataByPath.get(seedPayloadPath));
            PayloadPackingStats seedStats = Objects.requireNonNull(packingModel.statsByPath().get(seedPayloadPath));
            ChunkPlanBuilder chunkBuilder = new ChunkPlanBuilder();
            chunkBuilder.add(seedPayloadPath, seedPayload, seedStats);

            if (seedPayload.bytes().length < targetChunkBytes) {
                while (true) {
                    String nextPayloadPath = chooseNextChunkPayload(chunkBuilder, remainingPayloadPaths, payloadDataByPath, packingModel, targetChunkBytes);
                    if (nextPayloadPath == null) {
                        break;
                    }

                    remainingPayloadPaths.remove(nextPayloadPath);
                    chunkBuilder.add(
                            nextPayloadPath,
                            Objects.requireNonNull(payloadDataByPath.get(nextPayloadPath)),
                            Objects.requireNonNull(packingModel.statsByPath().get(nextPayloadPath))
                    );
                }
            }

            chunkBuilders.add(chunkBuilder);
        }

        LinkedHashMap<String, Integer> payloadIdsByPath = new LinkedHashMap<>();
        ArrayList<PayloadLocator> payloadLocators = new ArrayList<>();
        ArrayList<ChunkPlan> chunkPlans = new ArrayList<>();
        HashMap<String, PayloadLocator> payloadLocatorsByPath = new HashMap<>();
        long payloadCompressedBytes = 0L;
        long payloadIndexBodyBytes = 0L;

        for (ChunkPlanBuilder chunkBuilder : chunkBuilders) {
            int chunkId = chunkPlans.size();
            int offset = 0;
            ArrayList<String> chunkPayloadPaths = new ArrayList<>(chunkBuilder.payloadPaths());
            for (String payloadPath : chunkPayloadPaths) {
                PayloadData payloadData = Objects.requireNonNull(payloadDataByPath.get(payloadPath));
                PayloadLocator locator = new PayloadLocator(chunkId, offset, payloadData.bytes().length);
                payloadIdsByPath.put(payloadPath, payloadIdsByPath.size());
                payloadLocators.add(locator);
                payloadLocatorsByPath.put(payloadPath, locator);
                payloadIndexBodyBytes += estimateVarIntBytes(chunkId)
                        + estimateVarIntBytes(offset)
                        + estimateVarIntBytes(payloadData.bytes().length);
                offset += payloadData.bytes().length;
            }

            chunkPlans.add(new ChunkPlan(chunkEntryPath(chunkId), chunkPayloadPaths, chunkBuilder.uncompressedLength()));
            payloadIndexBodyBytes += estimateVarIntBytes(chunkBuilder.uncompressedLength());
            payloadCompressedBytes += estimateChunkCompressedBytes(chunkPayloadPaths, payloadDataByPath, approximateCompression);
        }

        long payloadIndexBytes = 5L
                + estimateVarIntBytes(chunkPlans.size())
                + estimateVarIntBytes(payloadLocators.size())
                + payloadIndexBodyBytes;
        long chunkOverheadBytes = (long) chunkPlans.size() * ESTIMATED_ZIP_CHUNK_OVERHEAD_BYTES;
        CacheSimulationMetrics cacheMetrics = simulateChunkCache(packingModel.accessFrames(), payloadLocatorsByPath);
        long predictedArchiveBytes = payloadCompressedBytes + chunkOverheadBytes + payloadIndexBytes;
        long localityPenaltyBytes = (cacheMetrics.archiveReads() * CHUNK_CACHE_MISS_PENALTY_BYTES)
                + ((long) cacheMetrics.multiChunkFrameCount() * MULTI_CHUNK_FRAME_PENALTY_BYTES);

        return new PackedPayloadArchive(
                Collections.unmodifiableMap(new LinkedHashMap<>(payloadIdsByPath)),
                List.copyOf(payloadLocators),
                List.copyOf(chunkPlans),
                new PackingMetrics(
                        targetChunkBytes,
                        payloadCompressedBytes,
                        payloadIndexBytes,
                        chunkOverheadBytes,
                        predictedArchiveBytes,
                        predictedArchiveBytes + localityPenaltyBytes,
                        cacheMetrics.cacheHits(),
                        cacheMetrics.cacheMisses(),
                        cacheMetrics.archiveReads(),
                        cacheMetrics.evictions(),
                        cacheMetrics.averageChunkReadsPerFrame(),
                        cacheMetrics.multiChunkFrameCount()
                )
        );
    }

    @Nullable
    protected static String chooseNextChunkPayload(@NotNull ChunkPlanBuilder chunkBuilder,
                                                   @NotNull Set<String> remainingPayloadPaths,
                                                   @NotNull Map<String, PayloadData> payloadDataByPath,
                                                   @NotNull PackingModel packingModel,
                                                   int targetChunkBytes) {
        int remainingCapacity = targetChunkBytes - chunkBuilder.uncompressedLength();
        if (remainingCapacity <= 0 || remainingPayloadPaths.isEmpty()) {
            return null;
        }

        String bestPayloadPath = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (String payloadPath : remainingPayloadPaths) {
            PayloadData payloadData = Objects.requireNonNull(payloadDataByPath.get(payloadPath));
            if (payloadData.bytes().length > remainingCapacity) {
                continue;
            }

            double score = scoreChunkCandidate(chunkBuilder, payloadPath, payloadData, packingModel, targetChunkBytes);
            if (score > bestScore) {
                bestScore = score;
                bestPayloadPath = payloadPath;
            }
        }

        if (bestPayloadPath == null) {
            return null;
        }
        if (chunkBuilder.uncompressedLength() >= (int) Math.round(targetChunkBytes * 0.8D) && bestScore < 96D) {
            return null;
        }
        if (chunkBuilder.uncompressedLength() >= (int) Math.round(targetChunkBytes * 0.55D) && bestScore < 0D) {
            return null;
        }
        return bestPayloadPath;
    }

    protected static double scoreChunkCandidate(@NotNull ChunkPlanBuilder chunkBuilder,
                                                @NotNull String payloadPath,
                                                @NotNull PayloadData payloadData,
                                                @NotNull PackingModel packingModel,
                                                int targetChunkBytes) {
        PayloadPackingStats stats = Objects.requireNonNull(packingModel.statsByPath().get(payloadPath));
        double score = 0D;
        score += chunkBuilder.affinityWeight(packingModel.edgeWeightsByPath(), payloadPath) * 256D;
        if (stats.dominantRegionId() >= 0 && chunkBuilder.regionIds().contains(stats.dominantRegionId())) {
            score += 192D;
        }
        if (stats.wasAccessed() && chunkBuilder.hasAccessWindow()) {
            if (stats.firstAccessIndex() <= (chunkBuilder.maxAccessIndex() + 2)
                    && stats.lastAccessIndex() >= (chunkBuilder.minAccessIndex() - 2)) {
                score += 96D;
            }
            score -= Math.abs(stats.center() - chunkBuilder.center()) * 1.5D;
        } else if (!stats.wasAccessed()) {
            score -= 64D;
        }

        double fillRatio = (double) (chunkBuilder.uncompressedLength() + payloadData.bytes().length) / Math.max(1, targetChunkBytes);
        score += fillRatio * 128D;
        score -= Math.max(0D, (targetChunkBytes - (chunkBuilder.uncompressedLength() + payloadData.bytes().length)) / 1024D);
        if ((chunkBuilder.uncompressedLength() + payloadData.bytes().length) == targetChunkBytes) {
            score += 64D;
        }
        return score;
    }

    protected static long estimateChunkCompressedBytes(@NotNull List<String> chunkPayloadPaths,
                                                       @NotNull Map<String, PayloadData> payloadDataByPath,
                                                       boolean approximateCompression) {
        if (chunkPayloadPaths.isEmpty()) {
            return 0L;
        }

        if (approximateCompression) {
            byte[] tail = EMPTY_BYTES;
            long compressedBytes = 0L;
            for (String payloadPath : chunkPayloadPaths) {
                byte[] payloadBytes = Objects.requireNonNull(payloadDataByPath.get(payloadPath)).bytes();
                compressedBytes += estimateChunkCompressionDelta(tail, payloadBytes);
                tail = appendDeflateTail(tail, payloadBytes);
            }
            return Math.max(1L, compressedBytes);
        }

        Deflater deflater = new Deflater(9, true);
        byte[] buffer = new byte[8192];
        long compressedBytes = 0L;
        try {
            for (String payloadPath : chunkPayloadPaths) {
                byte[] payloadBytes = Objects.requireNonNull(payloadDataByPath.get(payloadPath)).bytes();
                if (payloadBytes.length <= 0) {
                    continue;
                }

                deflater.setInput(payloadBytes);
                while (!deflater.needsInput()) {
                    compressedBytes += deflater.deflate(buffer);
                }
            }

            deflater.finish();
            while (!deflater.finished()) {
                compressedBytes += deflater.deflate(buffer);
            }
        } finally {
            deflater.end();
        }
        return Math.max(1L, compressedBytes);
    }

    @NotNull
    protected static CacheSimulationMetrics simulateChunkCache(@NotNull List<PayloadAccessFrame> accessFrames,
                                                               @NotNull Map<String, PayloadLocator> payloadLocatorsByPath) {
        if (accessFrames.isEmpty()) {
            return new CacheSimulationMetrics(0L, 0L, 0L, 0L, 0D, 0);
        }

        LinkedHashMap<Integer, Integer> cachedChunkIds = new LinkedHashMap<>(AfmaDecoder.MAX_CACHED_PAYLOAD_CHUNKS, 0.75F, true);
        long cacheHits = 0L;
        long cacheMisses = 0L;
        long archiveReads = 0L;
        long evictions = 0L;
        long chunkReads = 0L;
        int multiChunkFrameCount = 0;

        for (PayloadAccessFrame accessFrame : accessFrames) {
            LinkedHashSet<Integer> accessedChunkIds = new LinkedHashSet<>();
            for (String payloadPath : accessFrame.payloadPaths()) {
                PayloadLocator locator = payloadLocatorsByPath.get(payloadPath);
                if (locator != null) {
                    accessedChunkIds.add(locator.chunkId());
                }
            }

            chunkReads += accessedChunkIds.size();
            if (accessedChunkIds.size() > 1) {
                multiChunkFrameCount++;
            }

            for (Integer chunkId : accessedChunkIds) {
                if (cachedChunkIds.get(chunkId) != null) {
                    cacheHits++;
                    continue;
                }

                cacheMisses++;
                archiveReads++;
                if (cachedChunkIds.size() >= AfmaDecoder.MAX_CACHED_PAYLOAD_CHUNKS) {
                    var iterator = cachedChunkIds.entrySet().iterator();
                    if (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                        evictions++;
                    }
                }
                cachedChunkIds.put(chunkId, chunkId);
            }
        }

        return new CacheSimulationMetrics(
                cacheHits,
                cacheMisses,
                archiveReads,
                evictions,
                (double) chunkReads / Math.max(1, accessFrames.size()),
                multiChunkFrameCount
        );
    }

    @NotNull
    protected static String normalizePayloadPath(@NotNull String payloadPath) {
        return AfmaDecoder.normalizeEntryPath(payloadPath).toLowerCase(Locale.ROOT);
    }

    protected static void writeVarInt(@NotNull DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    protected static int readVarInt(@NotNull DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (position < 32) {
            int currentByte = in.readUnsignedByte();
            value |= (currentByte & 0x7F) << position;
            if ((currentByte & 0x80) == 0) {
                return value;
            }
            position += 7;
        }
        throw new IOException("AFMA payload index varint is too large");
    }

    public record PayloadLocator(int chunkId, int offset, int length) {
    }

    public record ChunkPlan(@NotNull String entryPath, @NotNull List<String> payloadPaths, int uncompressedLength) {
        public ChunkPlan {
            entryPath = Objects.requireNonNull(entryPath);
            payloadPaths = List.copyOf(Objects.requireNonNull(payloadPaths));
        }
    }

    public record PackedPayloadArchive(@NotNull Map<String, Integer> payloadIdsByPath,
                                       @NotNull List<PayloadLocator> payloadLocators,
                                       @NotNull List<ChunkPlan> chunkPlans,
                                       @NotNull PackingMetrics packingMetrics) {
        public PackedPayloadArchive {
            payloadIdsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payloadIdsByPath)));
            payloadLocators = List.copyOf(Objects.requireNonNull(payloadLocators));
            chunkPlans = List.copyOf(Objects.requireNonNull(chunkPlans));
            packingMetrics = Objects.requireNonNull(packingMetrics);
        }
    }

    public record DecodedPayloadIndex(@NotNull Map<String, PayloadLocator> payloadLocatorsByPath,
                                      int[] chunkLengths) {
        public DecodedPayloadIndex {
            payloadLocatorsByPath = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payloadLocatorsByPath)));
            chunkLengths = Objects.requireNonNull(chunkLengths).clone();
        }
    }

    public record PayloadAccessFrame(@NotNull List<String> payloadPaths, int keyframeRegionId, int sequenceKind) {
        public PayloadAccessFrame {
            payloadPaths = List.copyOf(Objects.requireNonNull(payloadPaths));
        }
    }

    public record ArchivePackingHints(@NotNull List<PayloadAccessFrame> accessFrames) {
        public ArchivePackingHints {
            accessFrames = List.copyOf(Objects.requireNonNull(accessFrames));
        }

        @NotNull
        public static ArchivePackingHints empty() {
            return new ArchivePackingHints(List.of());
        }

        public boolean isEmpty() {
            return this.accessFrames.isEmpty();
        }

        @NotNull
        public ArchivePackingHints append(@NotNull PayloadAccessFrame accessFrame) {
            ArrayList<PayloadAccessFrame> nextFrames = new ArrayList<>(this.accessFrames.size() + 1);
            nextFrames.addAll(this.accessFrames);
            nextFrames.add(Objects.requireNonNull(accessFrame));
            return new ArchivePackingHints(nextFrames);
        }
    }

    public record PackingMetrics(int targetChunkBytes,
                                 long estimatedCompressedPayloadBytes,
                                 long estimatedPayloadIndexBytes,
                                 long estimatedChunkOverheadBytes,
                                 long predictedArchiveBytes,
                                 long scoredArchiveBytes,
                                 long predictedChunkCacheHits,
                                 long predictedChunkCacheMisses,
                                 long predictedChunkArchiveReads,
                                 long predictedChunkCacheEvictions,
                                 double averageChunkReadsPerFrame,
                                 int multiChunkFrameCount) {
        @NotNull
        public static PackingMetrics empty(int targetChunkBytes) {
            return new PackingMetrics(targetChunkBytes, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0D, 0);
        }
    }

    protected record PayloadData(@NotNull String path, @NotNull byte[] bytes, int insertionOrder) {
    }

    protected record PackingModel(@NotNull List<PayloadAccessFrame> accessFrames,
                                  @NotNull Map<String, PayloadPackingStats> statsByPath,
                                  @NotNull Map<String, Map<String, Integer>> edgeWeightsByPath) {
    }

    protected record CacheSimulationMetrics(long cacheHits, long cacheMisses, long archiveReads, long evictions,
                                            double averageChunkReadsPerFrame, int multiChunkFrameCount) {
    }

    protected static final class PayloadPackingStats {

        protected final int insertionOrder;
        protected long accessCount = 0L;
        protected long weightedAccessIndexSum = 0L;
        protected int firstAccessIndex = Integer.MAX_VALUE;
        protected int lastAccessIndex = -1;
        protected int dominantRegionId = -1;
        protected int dominantRegionWeight = 0;
        @NotNull
        protected final Map<Integer, Integer> regionWeights = new HashMap<>();

        protected PayloadPackingStats(@NotNull PayloadData payloadData) {
            this.insertionOrder = payloadData.insertionOrder();
        }

        protected void recordAccess(int accessIndex, int regionId, int sequenceKind) {
            this.accessCount++;
            this.weightedAccessIndexSum += accessIndex;
            this.firstAccessIndex = Math.min(this.firstAccessIndex, accessIndex);
            this.lastAccessIndex = Math.max(this.lastAccessIndex, accessIndex);
            int nextRegionWeight = this.regionWeights.merge(regionId, 1, Integer::sum);
            if (nextRegionWeight > this.dominantRegionWeight) {
                this.dominantRegionWeight = nextRegionWeight;
                this.dominantRegionId = regionId;
            }
        }

        public boolean wasAccessed() {
            return this.accessCount > 0L;
        }

        public double center() {
            if (this.accessCount <= 0L) {
                return this.insertionOrder * 1024D;
            }
            return (double) this.weightedAccessIndexSum / (double) this.accessCount;
        }

        public int firstAccessIndex() {
            return this.wasAccessed() ? this.firstAccessIndex : Integer.MAX_VALUE;
        }

        public int lastAccessIndex() {
            return this.wasAccessed() ? this.lastAccessIndex : -1;
        }

        public int dominantRegionId() {
            return this.dominantRegionId;
        }

    }

    protected static final class ChunkPlanBuilder {

        @NotNull
        protected final ArrayList<String> payloadPaths = new ArrayList<>();
        protected int uncompressedLength = 0;
        protected double centerSum = 0D;
        protected int centerCount = 0;
        protected int minAccessIndex = Integer.MAX_VALUE;
        protected int maxAccessIndex = -1;
        @NotNull
        protected final Set<Integer> regionIds = new HashSet<>();

        public void add(@NotNull String payloadPath, @NotNull PayloadData payloadData, @NotNull PayloadPackingStats stats) {
            this.payloadPaths.add(payloadPath);
            this.uncompressedLength += payloadData.bytes().length;
            this.centerSum += stats.center();
            this.centerCount++;
            if (stats.wasAccessed()) {
                this.minAccessIndex = Math.min(this.minAccessIndex, stats.firstAccessIndex());
                this.maxAccessIndex = Math.max(this.maxAccessIndex, stats.lastAccessIndex());
            }
            if (stats.dominantRegionId() >= 0) {
                this.regionIds.add(stats.dominantRegionId());
            }
        }

        @NotNull
        public List<String> payloadPaths() {
            return this.payloadPaths;
        }

        public int uncompressedLength() {
            return this.uncompressedLength;
        }

        public boolean hasAccessWindow() {
            return this.minAccessIndex != Integer.MAX_VALUE && this.maxAccessIndex >= 0;
        }

        public int minAccessIndex() {
            return this.minAccessIndex;
        }

        public int maxAccessIndex() {
            return this.maxAccessIndex;
        }

        @NotNull
        public Set<Integer> regionIds() {
            return this.regionIds;
        }

        public double center() {
            return (this.centerCount > 0) ? (this.centerSum / (double) this.centerCount) : 0D;
        }

        public int affinityWeight(@NotNull Map<String, Map<String, Integer>> edgeWeightsByPath, @NotNull String candidatePath) {
            int weight = 0;
            for (String payloadPath : this.payloadPaths) {
                Map<String, Integer> edgeWeights = edgeWeightsByPath.get(payloadPath);
                if (edgeWeights != null) {
                    weight += edgeWeights.getOrDefault(candidatePath, 0);
                }
            }
            return weight;
        }

    }

}
