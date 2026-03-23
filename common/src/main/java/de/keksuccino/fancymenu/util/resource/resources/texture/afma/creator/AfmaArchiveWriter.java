package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public class AfmaArchiveWriter {

    private static final Gson GSON = new GsonBuilder().create();
    private static final FileTime ZIP_EPOCH = FileTime.fromMillis(0L);

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile) throws IOException {
        this.write(plan, outputFile, null);
    }

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested) throws IOException {
        this.write(plan, outputFile, cancellationRequested, null);
    }

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile, @Nullable BooleanSupplier cancellationRequested,
                      @Nullable ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(outputFile);

        File parent = outputFile.getParentFile();
        if ((parent != null) && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create AFMA output directory: " + parent.getAbsolutePath());
        }

        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            out.setEncoding(StandardCharsets.UTF_8.name());
            out.setLevel(9);
            int totalEntries = plan.getPayloads().size() + 2;
            int writtenEntries = 0;

            checkCancelled(cancellationRequested);
            this.writeJsonEntry(out, "metadata.json", GSON.toJson(plan.getMetadata()));
            writtenEntries++;
            reportProgress(progressListener, "metadata.json", writtenEntries, totalEntries);
            checkCancelled(cancellationRequested);
            this.writeJsonEntry(out, "frame_index.json", GSON.toJson(plan.getFrameIndex()));
            writtenEntries++;
            reportProgress(progressListener, "frame_index.json", writtenEntries, totalEntries);
            for (Map.Entry<String, byte[]> entry : plan.getPayloads().entrySet()) {
                checkCancelled(cancellationRequested);
                this.writeBinaryEntry(out, entry.getKey(), entry.getValue());
                writtenEntries++;
                reportProgress(progressListener, entry.getKey(), writtenEntries, totalEntries);
            }
            out.finish();
        }
    }

    protected void writeJsonEntry(@NotNull ZipArchiveOutputStream out, @NotNull String path, @NotNull String json) throws IOException {
        this.writeBinaryEntry(out, path, json.getBytes(StandardCharsets.UTF_8));
    }

    protected void writeBinaryEntry(@NotNull ZipArchiveOutputStream out, @NotNull String path, @NotNull byte[] bytes) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(path);
        entry.setSize(bytes.length);
        entry.setTime(0L);
        entry.setCreationTime(ZIP_EPOCH);
        entry.setLastModifiedTime(ZIP_EPOCH);
        entry.setLastAccessTime(ZIP_EPOCH);
        entry.setUnixMode(UnixStat.FILE_FLAG | 0644);
        out.putArchiveEntry(entry);
        out.write(bytes);
        out.closeArchiveEntry();
    }

    protected static void checkCancelled(@Nullable BooleanSupplier cancellationRequested) {
        if ((cancellationRequested != null) && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("AFMA archive writing was cancelled");
        }
    }

    protected static void reportProgress(@Nullable ProgressListener progressListener, @NotNull String path, int writtenEntries, int totalEntries) {
        if (progressListener != null) {
            progressListener.update(path, (double) writtenEntries / Math.max(1, totalEntries));
        }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(@NotNull String path, double progress);
    }

}
