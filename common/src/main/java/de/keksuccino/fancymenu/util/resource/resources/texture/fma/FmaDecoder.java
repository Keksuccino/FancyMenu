package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FmaDecoder implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final File TEMP_DIR = FileUtils.createDirectory(new File(FancyMenu.TEMP_DATA_DIR, "/decoded_fma_images"));
    private static final Pattern FRAME_ENTRY_PATTERN = Pattern.compile("^frames/(\\d+)\\.png$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTRO_FRAME_ENTRY_PATTERN = Pattern.compile("^intro_frames/(\\d+)\\.png$", Pattern.CASE_INSENSITIVE);
    private static final int EXPENSIVE_FRAME_SAMPLE_COUNT = 10;
    private static final int PNG_HEADER_PROBE_BYTES = 33;
    private static final byte[] PNG_SIGNATURE = new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    @Nullable
    protected ZipFile zipFile = null;
    @Nullable
    protected FmaMetadata metadata = null;
    @NotNull
    protected final List<String> orderedFramePaths = new ArrayList<>();
    @NotNull
    protected final List<String> orderedIntroFramePaths = new ArrayList<>();
    @NotNull
    protected final Map<String, ZipArchiveEntry> entriesByNormalizedPath = new HashMap<>();

    @Nullable
    protected File tempArchiveFile = null;
    protected boolean deleteTempArchiveOnClose = false;

    /**
     * Reads an FMA file from an {@link InputStream}. Spools the stream into a temporary archive file.
     * Closes the provided {@link InputStream} at the end.
     */
    public void read(@NotNull InputStream in) throws IOException {
        Objects.requireNonNull(in);
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");

        try {
            File tempArchive = this.spoolToTempArchive(in);
            this.openArchive(tempArchive, true);
            this.initializeArchiveState();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Reads an FMA file from a {@link File}.
     */
    public void read(@NotNull File fmaFile) throws IOException {
        Objects.requireNonNull(fmaFile);
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");

        try {
            this.openArchive(fmaFile, false);
            this.initializeArchiveState();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }

    @NotNull
    protected File spoolToTempArchive(@NotNull InputStream in) throws IOException {
        File tempArchive = File.createTempFile("fma_stream_", ".fma", TEMP_DIR);
        try (BufferedInputStream bufferedIn = new BufferedInputStream(in);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(tempArchive))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bufferedIn.read(buffer)) >= 0) {
                bufferedOut.write(buffer, 0, read);
            }
            bufferedOut.flush();
        } catch (Exception ex) {
            if (tempArchive.exists() && !tempArchive.delete()) {
                LOGGER.warn("[FANCYMENU] Failed to delete temporary FMA archive after spool failure: {}", tempArchive.getAbsolutePath());
            }
            throw ex;
        }
        return tempArchive;
    }

    protected void openArchive(@NotNull File archiveFile, boolean deleteOnClose) throws IOException {
        this.zipFile = new ZipFile(archiveFile);
        this.tempArchiveFile = deleteOnClose ? archiveFile : null;
        this.deleteTempArchiveOnClose = deleteOnClose;
    }

    protected void initializeArchiveState() throws IOException {
        this.indexEntries();
        this.readMetadata();
        boolean loadedFromFrameIndex = this.readFrameIndexIfPresent();
        if (!loadedFromFrameIndex) {
            this.readFramePaths();
            this.readIntroFramePaths();
        }

        if (this.orderedFramePaths.isEmpty()) {
            throw new FileNotFoundException("No frames found in FMA file!");
        }
    }

    /**
     * Optional fast-path index for newer FMA files.
     * If this index is absent or invalid, decoder falls back to classic directory scanning.
     */
    protected boolean readFrameIndexIfPresent() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.orderedFramePaths.clear();
        this.orderedIntroFramePaths.clear();

        ZipArchiveEntry frameIndexEntry = this.findEntry("frame_index.json");
        if (frameIndexEntry == null) return false;

        try {
            String indexJson;
            try (InputStream indexIn = this.zipFile.getInputStream(frameIndexEntry)) {
                indexJson = new String(indexIn.readAllBytes(), StandardCharsets.UTF_8);
            }

            FmaFrameIndex frameIndex = GSON.fromJson(indexJson, FmaFrameIndex.class);
            if (frameIndex == null) return false;

            if (frameIndex.frames != null) {
                for (String rawPath : frameIndex.frames) {
                    if (rawPath == null) continue;
                    String normalizedPath = normalizeEntryPath(rawPath).toLowerCase(Locale.ROOT);
                    if (!FRAME_ENTRY_PATTERN.matcher(normalizedPath).matches()) continue;
                    if (!this.entriesByNormalizedPath.containsKey(normalizedPath)) continue;
                    this.orderedFramePaths.add(normalizedPath);
                }
            }

            if (frameIndex.intro_frames != null) {
                for (String rawPath : frameIndex.intro_frames) {
                    if (rawPath == null) continue;
                    String normalizedPath = normalizeEntryPath(rawPath).toLowerCase(Locale.ROOT);
                    if (!INTRO_FRAME_ENTRY_PATTERN.matcher(normalizedPath).matches()) continue;
                    if (!this.entriesByNormalizedPath.containsKey(normalizedPath)) continue;
                    this.orderedIntroFramePaths.add(normalizedPath);
                }
            }

            if (this.orderedFramePaths.isEmpty()) {
                this.orderedIntroFramePaths.clear();
                return false;
            }

            return true;
        } catch (Exception ex) {
            // Any issue in optional index should gracefully fall back to classic scan path.
            this.orderedFramePaths.clear();
            this.orderedIntroFramePaths.clear();
            return false;
        }
    }

    protected void indexEntries() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.entriesByNormalizedPath.clear();

        var entries = this.zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry == null || entry.isDirectory()) continue;
            String normalized = normalizeEntryPath(entry.getName()).toLowerCase(Locale.ROOT);
            this.entriesByNormalizedPath.putIfAbsent(normalized, entry);
        }
    }

    protected void readFramePaths() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.orderedFramePaths.clear();

        try {
            for (String normalizedPath : this.entriesByNormalizedPath.keySet()) {
                Matcher matcher = FRAME_ENTRY_PATTERN.matcher(normalizedPath);
                if (!matcher.matches()) continue;

                String indexString = matcher.group(1);
                if (MathUtils.isInteger(indexString)) {
                    this.orderedFramePaths.add(normalizedPath);
                } else {
                    LOGGER.error("[FANCYMENU] Invalid PNG frame found in FMA file!", new IllegalStateException("Frame file name is not a valid number: " + normalizedPath));
                }
            }

            this.orderedFramePaths.sort(Comparator.comparingInt(FmaDecoder::readFrameIndex));
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected void readIntroFramePaths() throws IOException {
        Objects.requireNonNull(this.zipFile);
        this.orderedIntroFramePaths.clear();

        try {
            for (String normalizedPath : this.entriesByNormalizedPath.keySet()) {
                Matcher matcher = INTRO_FRAME_ENTRY_PATTERN.matcher(normalizedPath);
                if (!matcher.matches()) continue;

                String indexString = matcher.group(1);
                if (MathUtils.isInteger(indexString)) {
                    this.orderedIntroFramePaths.add(normalizedPath);
                } else {
                    LOGGER.error("[FANCYMENU] Invalid PNG intro frame found in FMA file!", new IllegalStateException("Frame file name is not a valid number: " + normalizedPath));
                }
            }

            this.orderedIntroFramePaths.sort(Comparator.comparingInt(FmaDecoder::readFrameIndex));
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    protected void readMetadata() throws IOException {
        Objects.requireNonNull(this.zipFile);

        try {
            ZipArchiveEntry metadataEntry = this.findEntry("metadata.json");
            if (metadataEntry == null) {
                throw new FileNotFoundException("No metadata.json found in FMA file! Unable to read metadata!");
            }

            String metadataString;
            try (InputStream metadataIn = this.zipFile.getInputStream(metadataEntry)) {
                metadataString = new String(metadataIn.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (metadataString.trim().isEmpty()) {
                throw new IOException("metadata.json of FMA file is empty!");
            }

            FmaMetadata parsedMetadata = GSON.fromJson(metadataString, FmaMetadata.class);
            if (parsedMetadata == null) {
                throw new IOException("Unable to parse metadata.json of FMA file!");
            }

            this.metadata = parsedMetadata;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public int getFrameCount() {
        return this.orderedFramePaths.size();
    }

    public int getIntroFrameCount() {
        return this.orderedIntroFramePaths.size();
    }

    public boolean hasIntroFrames() {
        return (this.getIntroFrameCount() > 0);
    }

    @Nullable
    public FmaMetadata getMetadata() {
        return this.metadata;
    }

    @Nullable
    public ExpensiveFrameSample findExpensiveFrameSample() throws IOException {
        Objects.requireNonNull(this.zipFile);

        for (String framePath : this.pickSampleFramePaths()) {
            PngFrameHeader pngHeader = this.readPngFrameHeader(framePath);
            if ((pngHeader != null) && pngHeader.isExpensive()) {
                return new ExpensiveFrameSample(framePath, pngHeader.width(), pngHeader.height(), pngHeader.bitDepth(), pngHeader.colorType(), pngHeader.interlaceMethod());
            }
        }

        return null;
    }

    @Nullable
    public InputStream getFrame(int index) throws IOException {
        Objects.requireNonNull(this.zipFile);
        if (this.orderedFramePaths.isEmpty()) return null;
        if (index < 0) return null;
        if (this.getFrameCount() - 1 < index) return null;

        try {
            String framePath = this.orderedFramePaths.get(index);
            ZipArchiveEntry frameEntry = this.findEntry(framePath);
            if (frameEntry == null) {
                throw new FileNotFoundException("Frame file of FMA not found: " + framePath);
            }
            return this.zipFile.getInputStream(frameEntry);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Nullable
    public InputStream getFirstFrame() throws IOException {
        return this.getFrame(0);
    }

    @Nullable
    public BufferedImage getFirstFrameAsBufferedImage() throws IOException {
        InputStream in = this.getFirstFrame();
        if (in == null) return null;

        try (InputStream closeableIn = in) {
            return ImageIO.read(closeableIn);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Nullable
    public InputStream getIntroFrame(int index) throws IOException {
        Objects.requireNonNull(this.zipFile);
        if (this.orderedIntroFramePaths.isEmpty()) return null;
        if (index < 0) return null;
        if (this.getIntroFrameCount() - 1 < index) return null;

        try {
            String framePath = this.orderedIntroFramePaths.get(index);
            ZipArchiveEntry frameEntry = this.findEntry(framePath);
            if (frameEntry == null) {
                throw new FileNotFoundException("Intro frame file of FMA not found: " + framePath);
            }
            return this.zipFile.getInputStream(frameEntry);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Returns the background.png image of the FMA file, if present. Returns NULL if no background.png file is present.<br>
     * This is UNUSED at the moment. The background is not used by FancyMenu's {@link FmaTexture} class.
     */
    @Nullable
    public InputStream getBackgroundImage() throws IOException {
        Objects.requireNonNull(this.zipFile);

        ZipArchiveEntry backgroundImageEntry = this.findEntry("background.png");
        if (backgroundImageEntry == null) return null;

        try {
            return this.zipFile.getInputStream(backgroundImageEntry);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Nullable
    protected ZipArchiveEntry findEntry(@NotNull String path) {
        String normalizedPath = normalizeEntryPath(path).toLowerCase(Locale.ROOT);
        return this.entriesByNormalizedPath.get(normalizedPath);
    }

    @NotNull
    protected List<String> pickSampleFramePaths() {
        List<String> allFramePaths = new ArrayList<>(this.orderedIntroFramePaths.size() + this.orderedFramePaths.size());
        allFramePaths.addAll(this.orderedIntroFramePaths);
        allFramePaths.addAll(this.orderedFramePaths);

        if (allFramePaths.isEmpty()) return List.of();

        int sampleCount = Math.min(EXPENSIVE_FRAME_SAMPLE_COUNT, allFramePaths.size());
        if (sampleCount >= allFramePaths.size()) {
            return allFramePaths;
        }

        Set<Integer> sampleIndexes = new LinkedHashSet<>();
        if (sampleCount == 1) {
            sampleIndexes.add(0);
        } else {
            for (int i = 0; i < sampleCount; i++) {
                int index = (int)Math.round((i * (allFramePaths.size() - 1D)) / (sampleCount - 1D));
                sampleIndexes.add(Math.max(0, Math.min(allFramePaths.size() - 1, index)));
            }
        }

        for (int i = 0; (sampleIndexes.size() < sampleCount) && (i < allFramePaths.size()); i++) {
            sampleIndexes.add(i);
        }

        List<String> sampledPaths = new ArrayList<>(sampleIndexes.size());
        for (int index : sampleIndexes) {
            sampledPaths.add(allFramePaths.get(index));
        }
        return sampledPaths;
    }

    @Nullable
    protected PngFrameHeader readPngFrameHeader(@NotNull String framePath) throws IOException {
        Objects.requireNonNull(this.zipFile);

        ZipArchiveEntry frameEntry = this.findEntry(framePath);
        if (frameEntry == null) return null;

        byte[] headerBytes = new byte[PNG_HEADER_PROBE_BYTES];
        int totalRead = 0;
        try (InputStream frameIn = this.zipFile.getInputStream(frameEntry)) {
            while (totalRead < headerBytes.length) {
                int read = frameIn.read(headerBytes, totalRead, headerBytes.length - totalRead);
                if (read < 0) break;
                totalRead += read;
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        if (totalRead < PNG_HEADER_PROBE_BYTES) return null;
        if (!matchesPngSignature(headerBytes)) return null;
        if ((headerBytes[12] != 'I') || (headerBytes[13] != 'H') || (headerBytes[14] != 'D') || (headerBytes[15] != 'R')) return null;

        return new PngFrameHeader(
                readIntBigEndian(headerBytes, 16),
                readIntBigEndian(headerBytes, 20),
                Byte.toUnsignedInt(headerBytes[24]),
                Byte.toUnsignedInt(headerBytes[25]),
                Byte.toUnsignedInt(headerBytes[28])
        );
    }

    protected static boolean matchesPngSignature(@NotNull byte[] headerBytes) {
        if (headerBytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (headerBytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }

    protected static int readIntBigEndian(@NotNull byte[] bytes, int offset) {
        return ((bytes[offset] & 255) << 24)
                | ((bytes[offset + 1] & 255) << 16)
                | ((bytes[offset + 2] & 255) << 8)
                | (bytes[offset + 3] & 255);
    }

    protected static int readFrameIndex(@NotNull String normalizedPath) {
        String fileName = normalizedPath;
        int separatorIndex = normalizedPath.lastIndexOf('/');
        if (separatorIndex >= 0 && separatorIndex < (normalizedPath.length() - 1)) {
            fileName = normalizedPath.substring(separatorIndex + 1);
        }
        return Integer.parseInt(Files.getNameWithoutExtension(fileName));
    }

    @NotNull
    protected static String normalizeEntryPath(@NotNull String entryPath) {
        String normalized = entryPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    @Override
    public void close() throws IOException {
        this.orderedFramePaths.clear();
        this.orderedIntroFramePaths.clear();
        this.entriesByNormalizedPath.clear();
        this.metadata = null;

        if (this.zipFile != null) {
            try {
                this.zipFile.close();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to close FMA ZipFile", ex);
            }
            this.zipFile = null;
        }

        File tempArchive = this.tempArchiveFile;
        boolean shouldDeleteTempArchive = this.deleteTempArchiveOnClose;
        this.tempArchiveFile = null;
        this.deleteTempArchiveOnClose = false;

        if (shouldDeleteTempArchive && (tempArchive != null) && tempArchive.exists() && !tempArchive.delete()) {
            LOGGER.warn("[FANCYMENU] Failed to delete temporary FMA archive: {}", tempArchive.getAbsolutePath());
        }
    }

    public static class FmaMetadata {

        protected int loop_count;
        protected long frame_time;
        protected long frame_time_intro;
        protected Map<Integer, Long> custom_frame_times;
        protected Map<Integer, Long> custom_frame_times_intro;

        public int getLoopCount() {
            return this.loop_count;
        }

        public long getFrameTime() {
            return this.frame_time;
        }

        public long getFrameTimeIntro() {
            return this.frame_time_intro;
        }

        @Nullable
        public Map<Integer, Long> getCustomFrameTimes() {
            return this.custom_frame_times;
        }

        @Nullable
        public Map<Integer, Long> getCustomFrameTimesIntro() {
            return this.custom_frame_times_intro;
        }

        public long getFrameTimeForFrame(int frame, boolean isIntroFrame) {
            if (isIntroFrame) {
                if ((custom_frame_times_intro != null) && custom_frame_times_intro.containsKey(frame)) return custom_frame_times_intro.get(frame);
            } else {
                if ((custom_frame_times != null) && custom_frame_times.containsKey(frame)) return custom_frame_times.get(frame);
            }
            return isIntroFrame ? this.getFrameTimeIntro() : this.getFrameTime();
        }

    }

    public static class FmaFrameIndex {
        @Nullable
        protected List<String> frames;
        @Nullable
        protected List<String> intro_frames;
    }

    public record ExpensiveFrameSample(@NotNull String framePath, int width, int height, int bitDepth, int colorType, int interlaceMethod) {
    }

    protected record PngFrameHeader(int width, int height, int bitDepth, int colorType, int interlaceMethod) {
        protected boolean isExpensive() {
            return this.bitDepth > 8;
        }
    }

}
