package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaAtlasPixelOffsetPlacementProgramCodec;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedAnimation;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Writes the production AFMA file format.
 *
 * <p>The old container stored JSON metadata and every payload as separate ZIP
 * entries. The new in-dev format is a single binary stream with:
 * <ol>
 *     <li>a fixed AFMA stream magic and container version,</li>
 *     <li>one UTF-8 metadata JSON section,</li>
 *     <li>one exact atlas/program animation section compressed by AFMA's native
 *     animation codec,</li>
 *     <li>and one optional thumbnail section.</li>
 * </ol>
 *
 * <p>This keeps AFMA metadata human-readable while removing ZIP container
 * overhead and promoting the native AFMA codec into the real on-disk format.
 */
public class AfmaArchiveWriter {

    private static final int MAGIC = 0x41464D35; // AFM5
    private static final int CONTAINER_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().create();
    private static final AfmaAtlasPixelOffsetPlacementProgramCodec CODEC = AfmaAtlasPixelOffsetPlacementProgramCodec.production();

    /**
     * Encodes the supplied source sequences into the native AFMA stream format.
     */
    public void write(@NotNull AfmaMetadata metadata, @NotNull AfmaSourceSequence mainSequence, @NotNull AfmaSourceSequence introSequence,
                      @Nullable byte[] thumbnailBytes, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested,
                      @Nullable ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(mainSequence);
        Objects.requireNonNull(introSequence);
        Objects.requireNonNull(outputFile);

        Path outputDirectory = resolveOutputDirectory(outputFile);
        try (StagedAfmaStream stagedStream = this.stageStream(metadata, mainSequence, introSequence, thumbnailBytes, outputDirectory, cancellationRequested, progressListener);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            out.writeInt(MAGIC);
            out.writeByte(CONTAINER_VERSION);
            writeSection(out, stagedStream.metadataSection());
            reportProgress(progressListener, "Writing metadata section...", 0.92D);
            writeSection(out, stagedStream.animationSection());
            reportProgress(progressListener, "Writing animation section...", 0.98D);
            writeSection(out, stagedStream.thumbnailSection());
            reportProgress(progressListener, "Writing thumbnail section...", 1.0D);
            out.flush();
        }
    }

    /**
     * Produces a cheap creator-side size estimate without running the native
     * AFMA animation codec.
     *
     * <p>The native codec is intentionally memory-heavy during export because it
     * analyzes the full decoded animation globally. Running that same path again
     * just to populate the creator's "Estimated AFMA Size" summary would double
     * the peak memory pressure and can exhaust the heap before the real export
     * even starts. The creator therefore uses the already computed frame plan as
     * a heuristic estimate instead of triggering a second full encode.
     */
    public long estimateBytes(@NotNull AfmaEncodePlan plan, @Nullable byte[] thumbnailBytes) {
        Objects.requireNonNull(plan);
        long metadataBytes = GSON.toJson(plan.getMetadata()).getBytes(StandardCharsets.UTF_8).length;
        long frameIndexBytes = GSON.toJson(plan.getFrameIndex()).getBytes(StandardCharsets.UTF_8).length;
        long animationBytes = frameIndexBytes + plan.getTotalPayloadBytes();
        long thumbnailSectionBytes = (thumbnailBytes != null) ? thumbnailBytes.length : 0L;
        return Integer.BYTES
                + Byte.BYTES
                + estimateSectionBytes(metadataBytes)
                + estimateSectionBytes(animationBytes)
                + estimateSectionBytes(thumbnailSectionBytes);
    }

    protected @NotNull StagedAfmaStream stageStream(@NotNull AfmaMetadata metadata, @NotNull AfmaSourceSequence mainSequence,
                                                    @NotNull AfmaSourceSequence introSequence, @Nullable byte[] thumbnailBytes,
                                                    @NotNull Path outputDirectory, @Nullable BooleanSupplier cancellationRequested,
                                                    @Nullable ProgressListener progressListener) throws IOException {
        TempSection metadataSection = null;
        TempSection animationSection = null;
        TempSection thumbnailSection = null;
        boolean success = false;
        try {
            checkCancelled(cancellationRequested);
            metadataSection = writeTempSection(outputDirectory, "metadata", GSON.toJson(metadata).getBytes(StandardCharsets.UTF_8));

            checkCancelled(cancellationRequested);
            reportProgress(progressListener, "Loading source frames...", 0.04D);
            AfmaDecodedAnimation animation = this.loadDecodedAnimation(mainSequence, introSequence, cancellationRequested, progressListener);
            checkCancelled(cancellationRequested);
            reportProgress(progressListener, "Encoding animation stream...", 0.62D);
            animationSection = writeTempSection(outputDirectory, "animation", out -> CODEC.compress(animation, out));
            checkCancelled(cancellationRequested);

            checkCancelled(cancellationRequested);
            reportProgress(progressListener, (thumbnailBytes != null) ? "Writing thumbnail section..." : "Finalizing archive sections...", 0.99D);
            thumbnailSection = writeTempSection(outputDirectory, "thumbnail", (thumbnailBytes != null) ? thumbnailBytes : new byte[0]);
            success = true;
            return new StagedAfmaStream(metadataSection, animationSection, thumbnailSection);
        } finally {
            if (!success) {
                deleteQuietly(metadataSection);
                deleteQuietly(animationSection);
                deleteQuietly(thumbnailSection);
            }
        }
    }

    protected static @NotNull TempSection writeTempSection(@NotNull Path outputDirectory, @NotNull String sectionName, byte @NotNull [] bytes) throws IOException {
        Path tempFile = Files.createTempFile(outputDirectory, "fancymenu_afma_" + sectionName + "_", ".tmp");
        boolean success = false;
        try {
            Files.write(tempFile, bytes);
            success = true;
            return new TempSection(tempFile, bytes.length);
        } finally {
            if (!success) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    protected static @NotNull TempSection writeTempSection(@NotNull Path outputDirectory, @NotNull String sectionName,
                                                           @NotNull SectionWriter writer) throws IOException {
        Path tempFile = Files.createTempFile(outputDirectory, "fancymenu_afma_" + sectionName + "_", ".tmp");
        boolean success = false;
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
            writer.write(out);
            out.flush();
            success = true;
            return new TempSection(tempFile, Files.size(tempFile));
        } finally {
            if (!success) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * Loads the creator source PNGs into an exact decoded animation model for
     * the production codec.
     */
    protected @NotNull AfmaDecodedAnimation loadDecodedAnimation(@NotNull AfmaSourceSequence mainSequence, @NotNull AfmaSourceSequence introSequence,
                                                                 @Nullable BooleanSupplier cancellationRequested,
                                                                 @Nullable ProgressListener progressListener) throws IOException {
        List<File> introFiles = introSequence.getFrames();
        List<File> mainFiles = mainSequence.getFrames();
        if (introFiles.isEmpty() && mainFiles.isEmpty()) {
            throw new IOException("AFMA encoding requires at least one source frame");
        }

        int totalFrames = introFiles.size() + mainFiles.size();
        AfmaFrameNormalizer normalizer = new AfmaFrameNormalizer();
        int[] expectedWidth = {-1};
        int[] expectedHeight = {-1};
        int[] loadedFrames = {0};

        List<AfmaDecodedFrame> decodedIntro = this.loadSequenceFrames(introFiles, "intro", normalizer, expectedWidth, expectedHeight, loadedFrames, totalFrames, cancellationRequested, progressListener);
        List<AfmaDecodedFrame> decodedMain = this.loadSequenceFrames(mainFiles, "main", normalizer, expectedWidth, expectedHeight, loadedFrames, totalFrames, cancellationRequested, progressListener);

        return new AfmaDecodedAnimation(expectedWidth[0], expectedHeight[0], decodedIntro, decodedMain);
    }

    protected @NotNull List<AfmaDecodedFrame> loadSequenceFrames(@NotNull List<File> files, @NotNull String sequenceName, @NotNull AfmaFrameNormalizer normalizer,
                                                                 int[] expectedWidth, int[] expectedHeight, int[] loadedFrames, int totalFrames,
                                                                 @Nullable BooleanSupplier cancellationRequested,
                                                                 @Nullable ProgressListener progressListener) throws IOException {
        ArrayList<AfmaDecodedFrame> result = new ArrayList<>(files.size());
        for (int index = 0; index < files.size(); index++) {
            File file = files.get(index);
            checkCancelled(cancellationRequested);
            try (AfmaPixelFrame frame = normalizer.loadFrame(file)) {
                if (expectedWidth[0] < 0) {
                    expectedWidth[0] = frame.getWidth();
                    expectedHeight[0] = frame.getHeight();
                } else if ((expectedWidth[0] != frame.getWidth()) || (expectedHeight[0] != frame.getHeight())) {
                    throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + file.getAbsolutePath());
                }
                result.add(new AfmaDecodedFrame(frame.getWidth(), frame.getHeight(), frame.borrowPixels()));
            }
            loadedFrames[0]++;
            reportProgress(progressListener,
                    "Loading " + sequenceName + " frame " + (index + 1) + "/" + files.size() + ": " + file.getName(),
                    0.08D + (0.48D * loadedFrames[0]) / Math.max(1, totalFrames));
        }
        return result;
    }

    protected static void writeSection(@NotNull DataOutputStream out, @NotNull TempSection section) throws IOException {
        if (section.length() > Integer.MAX_VALUE) {
            throw new IOException("AFMA section is too large to write: " + section.path().toAbsolutePath());
        }
        out.writeInt((int) section.length());
        try (InputStream in = new BufferedInputStream(Files.newInputStream(section.path()))) {
            in.transferTo(out);
        }
    }

    protected static long estimateSectionBytes(byte @NotNull [] bytes) {
        return estimateSectionBytes(bytes.length);
    }

    protected static long estimateSectionBytes(long bytes) {
        return Integer.BYTES + Math.max(0L, bytes);
    }

    protected static @NotNull Path resolveOutputDirectory(@NotNull File outputFile) throws IOException {
        Path outputPath = outputFile.toPath().toAbsolutePath();
        Path outputDirectory = outputPath.getParent();
        if (outputDirectory == null) {
            throw new IOException("Failed to resolve AFMA output directory for: " + outputFile.getAbsolutePath());
        }
        Files.createDirectories(outputDirectory);
        return outputDirectory;
    }

    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA archive writing was cancelled");
        }
    }

    protected static void reportProgress(@Nullable ProgressListener progressListener, @NotNull String detail, double progress) {
        if (progressListener != null) {
            progressListener.update(detail, Math.max(0.0D, Math.min(1.0D, progress)));
        }
    }

    protected static void deleteQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String detail, double progress);
    }

    @FunctionalInterface
    protected interface SectionWriter {
        void write(@NotNull OutputStream out) throws IOException;
    }

    protected record TempSection(@NotNull Path path, long length) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            Files.deleteIfExists(this.path);
        }
    }

    protected record StagedAfmaStream(@NotNull TempSection metadataSection, @NotNull TempSection animationSection,
                                      @NotNull TempSection thumbnailSection) implements AutoCloseable {
        @Override
        public void close() {
            deleteQuietly(this.metadataSection);
            deleteQuietly(this.animationSection);
            deleteQuietly(this.thumbnailSection);
        }
    }
}
