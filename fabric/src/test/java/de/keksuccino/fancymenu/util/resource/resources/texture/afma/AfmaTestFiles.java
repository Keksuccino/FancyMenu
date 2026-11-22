package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.google.gson.Gson;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator.AfmaArchiveWriter;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator.AfmaEncodePlan;
import org.jetbrains.annotations.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes small AFMA files with the real encoder and archive writer: full frames of noise that loop forever, every frame shows for 100 ms.
 * Noise doesn't compress, so a few frames of 160x160 pixels already need payload chunks that are bigger than one locked read of the decoder.
 */
final class AfmaTestFiles {

    static final int WIDTH = 160;
    static final int HEIGHT = 160;

    static {
        // The payloads of the encoder may spill into a temporary directory, which belongs to the build directory of the tests and not to the temporary directory of the system
        if (System.getProperty(AfmaIoHelper.TEMP_DIR_PROPERTY) == null) {
            System.setProperty(AfmaIoHelper.TEMP_DIR_PROPERTY, new File("afma_temp").getAbsolutePath());
        }
    }

    private AfmaTestFiles() {
    }

    /**
     * Writes a v2 AFMA container.
     */
    @NotNull
    static Path writeContainer(@NotNull Path file, int frameCount) throws IOException {
        LinkedHashMap<String, AfmaStoredPayload> payloads = encodeFrames(frameCount);
        try (AfmaEncodePlan plan = new AfmaEncodePlan(metadata(), frameIndex(payloads), payloads)) {
            new AfmaArchiveWriter().write(plan, file.toFile());
        }
        return file;
    }

    /**
     * Writes a legacy AFMA archive, a ZIP file with the metadata, the payload index, the frame index and the payload chunks as entries.
     */
    @NotNull
    static Path writeLegacyZip(@NotNull Path file, int frameCount) throws IOException {
        LinkedHashMap<String, AfmaStoredPayload> payloads = encodeFrames(frameCount);
        try {
            AfmaChunkedPayloadHelper.PackedPayloadArchive layout = AfmaChunkedPayloadHelper.buildArchiveLayout(payloads);
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
                writeEntry(out, "metadata.json", new Gson().toJson(metadata()).getBytes(StandardCharsets.UTF_8));
                writeEntry(out, AfmaChunkedPayloadHelper.PAYLOAD_INDEX_ENTRY_PATH, AfmaChunkedPayloadHelper.encodePayloadIndex(layout));
                writeEntry(out, AfmaBinaryFrameIndexHelper.FRAME_INDEX_ENTRY_PATH, AfmaBinaryFrameIndexHelper.encodeFrameIndex(frameIndex(payloads), layout.payloadIdsByPath()));
                for (AfmaChunkedPayloadHelper.ChunkPlan chunk : layout.chunkPlans()) {
                    ByteArrayOutputStream chunkBytes = new ByteArrayOutputStream();
                    for (String payloadPath : chunk.payloadPaths()) {
                        chunkBytes.write(payloads.get(payloadPath).readAllBytes());
                    }
                    writeEntry(out, chunk.entryPath(), chunkBytes.toByteArray());
                }
            }
        } finally {
            for (AfmaStoredPayload payload : payloads.values()) payload.close();
        }
        return file;
    }

    /**
     * The payload of a frame, as the encoder wrote it into the file.
     */
    @NotNull
    static byte[] framePayload(int frame) throws IOException {
        return AfmaBinIntraPayloadHelper.encodePayload(WIDTH, HEIGHT, noise(frame));
    }

    @NotNull
    private static LinkedHashMap<String, AfmaStoredPayload> encodeFrames(int frameCount) throws IOException {
        LinkedHashMap<String, AfmaStoredPayload> payloads = new LinkedHashMap<>();
        for (int i = 0; i < frameCount; i++) {
            payloads.put("frames/" + i + ".bin", AfmaStoredPayload.fromBytes(framePayload(i)));
        }
        return payloads;
    }

    @NotNull
    private static AfmaFrameIndex frameIndex(@NotNull Map<String, AfmaStoredPayload> payloads) {
        List<AfmaFrameDescriptor> frames = new ArrayList<>();
        for (String path : payloads.keySet()) {
            frames.add(AfmaFrameDescriptor.full(path));
        }
        return new AfmaFrameIndex(frames, List.of());
    }

    @NotNull
    private static AfmaMetadata metadata() {
        return AfmaMetadata.create(WIDTH, HEIGHT, 0, 100L, 100L, null, null, AfmaMetadata.DEFAULT_KEYFRAME_INTERVAL, true, true);
    }

    @NotNull
    private static int[] noise(int seed) {
        Random random = new Random(seed);
        int[] pixels = new int[WIDTH * HEIGHT];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFF000000 | random.nextInt(0x1000000);
        }
        return pixels;
    }

    private static void writeEntry(@NotNull ZipOutputStream out, @NotNull String name, @NotNull byte[] bytes) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

}
