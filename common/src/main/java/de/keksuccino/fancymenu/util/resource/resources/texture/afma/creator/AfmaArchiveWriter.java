package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaMetadata;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaAtlasPixelOffsetPlacementProgramCodec;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedAnimation;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        File parent = outputFile.getParentFile();
        if ((parent != null) && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create AFMA output directory: " + parent.getAbsolutePath());
        }

        EncodedAfmaStream encodedStream = this.encodeStream(metadata, mainSequence, introSequence, thumbnailBytes, cancellationRequested, progressListener);

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            out.writeInt(MAGIC);
            out.writeByte(CONTAINER_VERSION);
            writeSection(out, encodedStream.metadataBytes());
            reportProgress(progressListener, "metadata", 0.84D);
            writeSection(out, encodedStream.animationBytes());
            reportProgress(progressListener, "animation", 0.96D);
            writeSection(out, encodedStream.thumbnailBytes());
            reportProgress(progressListener, "thumbnail", 1.0D);
            out.flush();
        }
    }

    /**
     * Estimates the final native AFMA file size by running the same frame
     * normalization and animation compression path that the writer uses for the
     * on-disk stream, without writing a file.
     */
    public long estimateBytes(@NotNull AfmaMetadata metadata, @NotNull AfmaSourceSequence mainSequence, @NotNull AfmaSourceSequence introSequence,
                              @Nullable byte[] thumbnailBytes, @Nullable BooleanSupplier cancellationRequested,
                              @Nullable ProgressListener progressListener) throws IOException {
        EncodedAfmaStream encodedStream = this.encodeStream(metadata, mainSequence, introSequence, thumbnailBytes, cancellationRequested, progressListener);
        return Integer.BYTES
                + Byte.BYTES
                + estimateSectionBytes(encodedStream.metadataBytes())
                + estimateSectionBytes(encodedStream.animationBytes())
                + estimateSectionBytes(encodedStream.thumbnailBytes());
    }

    protected @NotNull EncodedAfmaStream encodeStream(@NotNull AfmaMetadata metadata, @NotNull AfmaSourceSequence mainSequence,
                                                      @NotNull AfmaSourceSequence introSequence, @Nullable byte[] thumbnailBytes,
                                                      @Nullable BooleanSupplier cancellationRequested,
                                                      @Nullable ProgressListener progressListener) throws IOException {
        AfmaDecodedAnimation animation = this.loadDecodedAnimation(mainSequence, introSequence, cancellationRequested, progressListener);
        checkCancelled(cancellationRequested);
        reportProgress(progressListener, "animation", 0.72D);
        byte[] animationBytes = CODEC.compress(animation);
        checkCancelled(cancellationRequested);

        byte[] metadataBytes = GSON.toJson(metadata).getBytes(StandardCharsets.UTF_8);
        byte[] thumbnailSection = (thumbnailBytes != null) ? thumbnailBytes : new byte[0];
        return new EncodedAfmaStream(metadataBytes, animationBytes, thumbnailSection);
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

        List<AfmaDecodedFrame> decodedIntro = this.loadSequenceFrames(introFiles, normalizer, expectedWidth, expectedHeight, loadedFrames, totalFrames, cancellationRequested, progressListener);
        List<AfmaDecodedFrame> decodedMain = this.loadSequenceFrames(mainFiles, normalizer, expectedWidth, expectedHeight, loadedFrames, totalFrames, cancellationRequested, progressListener);

        return new AfmaDecodedAnimation(expectedWidth[0], expectedHeight[0], decodedIntro, decodedMain);
    }

    protected @NotNull List<AfmaDecodedFrame> loadSequenceFrames(@NotNull List<File> files, @NotNull AfmaFrameNormalizer normalizer,
                                                                 int[] expectedWidth, int[] expectedHeight, int[] loadedFrames, int totalFrames,
                                                                 @Nullable BooleanSupplier cancellationRequested,
                                                                 @Nullable ProgressListener progressListener) throws IOException {
        ArrayList<AfmaDecodedFrame> result = new ArrayList<>(files.size());
        for (File file : files) {
            checkCancelled(cancellationRequested);
            try (AfmaPixelFrame frame = normalizer.loadFrame(file)) {
                if (expectedWidth[0] < 0) {
                    expectedWidth[0] = frame.getWidth();
                    expectedHeight[0] = frame.getHeight();
                } else if ((expectedWidth[0] != frame.getWidth()) || (expectedHeight[0] != frame.getHeight())) {
                    throw new IOException("AFMA source frame dimensions do not match the expected canvas size: " + file.getAbsolutePath());
                }
                result.add(new AfmaDecodedFrame(frame.getWidth(), frame.getHeight(), frame.copyPixels()));
            }
            loadedFrames[0]++;
            reportProgress(progressListener, file.getName(), (0.70D * loadedFrames[0]) / Math.max(1, totalFrames));
        }
        return result;
    }

    protected static void writeSection(@NotNull DataOutputStream out, @NotNull byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    protected static long estimateSectionBytes(byte @NotNull [] bytes) {
        return Integer.BYTES + bytes.length;
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

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String detail, double progress);
    }

    protected record EncodedAfmaStream(byte @NotNull [] metadataBytes, byte @NotNull [] animationBytes, byte @NotNull [] thumbnailBytes) {
    }
}
