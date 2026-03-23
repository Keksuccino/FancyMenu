package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class AfmaArchiveWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void write(@NotNull AfmaEncodePlan plan, @NotNull File outputFile) throws IOException {
        Objects.requireNonNull(plan);
        Objects.requireNonNull(outputFile);

        File parent = outputFile.getParentFile();
        if ((parent != null) && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create AFMA output directory: " + parent.getAbsolutePath());
        }

        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            out.setEncoding(StandardCharsets.UTF_8.name());

            this.writeJsonEntry(out, "metadata.json", GSON.toJson(plan.getMetadata()));
            this.writeJsonEntry(out, "frame_index.json", GSON.toJson(plan.getFrameIndex()));
            for (Map.Entry<String, byte[]> entry : plan.getPayloads().entrySet()) {
                this.writeBinaryEntry(out, entry.getKey(), entry.getValue());
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
        out.putArchiveEntry(entry);
        out.write(bytes);
        out.closeArchiveEntry();
    }

}
