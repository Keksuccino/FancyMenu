package de.keksuccino.fancymenu.util.resource.resources.texture.afma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaAtlasPixelOffsetPlacementProgramCodec;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedAnimation;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.codec.AfmaDecodedFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Reads the current in-dev AFMA file format.
 *
 * <p>The new AFMA container is a single binary stream with metadata JSON,
 * one atlas/program animation payload, and one optional thumbnail payload.
 * Unlike the old ZIP-backed implementation, the decoder no longer resolves
 * per-entry payload paths at runtime.
 */
public class AfmaDecoder implements Closeable {

    private static final int MAGIC = 0x41464D35; // AFM5
    private static final int CONTAINER_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().create();
    private static final AfmaAtlasPixelOffsetPlacementProgramCodec CODEC = AfmaAtlasPixelOffsetPlacementProgramCodec.production();

    @Nullable
    protected AfmaMetadata metadata = null;
    @Nullable
    protected AfmaDecodedAnimation animation = null;
    @Nullable
    protected byte[] thumbnailBytes = null;

    /**
     * Reads an AFMA stream from an arbitrary input source.
     */
    public void read(@NotNull InputStream in) throws IOException {
        Objects.requireNonNull(in);
        if (this.animation != null) {
            throw new IllegalStateException("The decoder is already reading a file!");
        }

        try (InputStream closeableInput = in) {
            this.readBytes(closeableInput.readAllBytes(), "[stream]");
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }

    /**
     * Reads an AFMA stream from a local file.
     */
    public void read(@NotNull File afmaFile) throws IOException {
        Objects.requireNonNull(afmaFile);
        if (this.animation != null) {
            throw new IllegalStateException("The decoder is already reading a file!");
        }

        try {
            this.readBytes(Files.readAllBytes(afmaFile.toPath()), afmaFile.getName());
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }

    protected void readBytes(@NotNull byte[] fileBytes, @NotNull String animationName) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(fileBytes);
        DataInputStream dataIn = new DataInputStream(in);
        if (dataIn.readInt() != MAGIC) {
            throw new IOException("Unsupported AFMA stream magic");
        }

        int containerVersion = dataIn.readUnsignedByte();
        if (containerVersion != CONTAINER_VERSION) {
            throw new IOException("Unsupported AFMA container version: " + containerVersion);
        }

        byte[] metadataBytes = readSection(dataIn);
        byte[] animationBytes = readSection(dataIn);
        byte[] thumbnailSection = readSection(dataIn);

        String metadataJson = new String(metadataBytes, StandardCharsets.UTF_8);
        AfmaMetadata parsedMetadata = GSON.fromJson(metadataJson, AfmaMetadata.class);
        if (parsedMetadata == null) {
            throw new IOException("Unable to parse embedded AFMA metadata");
        }
        parsedMetadata.validate();

        AfmaDecodedAnimation decodedAnimation = CODEC.decompress(animationName, animationBytes);
        if ((decodedAnimation.mainFrames().isEmpty()) && decodedAnimation.introFrames().isEmpty()) {
            throw new IOException("AFMA file does not contain any frames");
        }
        if ((parsedMetadata.getCanvasWidth() != decodedAnimation.width()) || (parsedMetadata.getCanvasHeight() != decodedAnimation.height())) {
            throw new IOException("AFMA metadata canvas size does not match the embedded animation stream");
        }

        this.metadata = parsedMetadata;
        this.animation = decodedAnimation;
        this.thumbnailBytes = (thumbnailSection.length > 0) ? thumbnailSection : null;
    }

    protected static byte @NotNull [] readSection(@NotNull DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            throw new IOException("AFMA stream section length is invalid");
        }
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("AFMA stream section ended early");
        }
        return bytes;
    }

    /**
     * Runs lightweight structural validation on the already parsed AFMA stream.
     */
    public void validateAllReferencedPayloadHeaders() throws IOException {
        AfmaDecodedAnimation activeAnimation = Objects.requireNonNull(this.animation, "AFMA animation was NULL");
        if ((activeAnimation.mainFrames().isEmpty()) && activeAnimation.introFrames().isEmpty()) {
            throw new IOException("AFMA file does not contain any decoded frames");
        }
        if (this.thumbnailBytes != null) {
            AfmaBinIntraPayloadHelper.decodePayload(this.thumbnailBytes);
        }
    }

    public int getFrameCount() {
        return (this.animation != null) ? this.animation.mainFrames().size() : 0;
    }

    public int getIntroFrameCount() {
        return (this.animation != null) ? this.animation.introFrames().size() : 0;
    }

    public boolean hasIntroFrames() {
        return this.getIntroFrameCount() > 0;
    }

    @Nullable
    public AfmaMetadata getMetadata() {
        return this.metadata;
    }

    @Nullable
    public AfmaDecodedFrame getFrame(int index) {
        if (this.animation == null) {
            return null;
        }
        if (index < 0 || index >= this.animation.mainFrames().size()) {
            return null;
        }
        return this.animation.mainFrames().get(index);
    }

    @Nullable
    public AfmaDecodedFrame getIntroFrame(int index) {
        if (this.animation == null) {
            return null;
        }
        if (index < 0 || index >= this.animation.introFrames().size()) {
            return null;
        }
        return this.animation.introFrames().get(index);
    }

    @Nullable
    public InputStream openThumbnail() {
        return (this.thumbnailBytes != null) ? new ByteArrayInputStream(this.thumbnailBytes) : null;
    }

    @Override
    public void close() {
        this.metadata = null;
        this.animation = null;
        this.thumbnailBytes = null;
    }
}
