package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

//TODO übernehmen (animation update)
public class FmaDecoder implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    @Nullable
    protected ZipFile zipFile = null;
    @Nullable
    protected FmaMetadata metadata = null;
    @NotNull
    protected final List<AutoCloseable> closeables = new ArrayList<>();
    @NotNull
    protected final List<String> orderedFrameNames = new ArrayList<>();

    /**
     * Reads an FMA file from an {@link InputStream}. Copies the FMA to the memory.<br>
     * Closes the provided {@link InputStream} at the end.
     */
    public void read(@NotNull InputStream in) throws Exception {
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        Exception exception = null;
        try {
            this.zipFile = this.putCloseable(ZipFile.builder().setSeekableByteChannel(this.putCloseable(new SeekableInMemoryByteChannel(in.readAllBytes()))).get());
            this.readMetadata();
            this.readFrameNames();
        } catch (Exception ex) {
            exception = ex;
        }
        CloseableUtils.closeQuietly(in);
        if (exception != null) {
            CloseableUtils.closeQuietly(this);
            this.zipFile = null;
            this.metadata = null;
            throw exception;
        }
    }

    /**
     * Reads an FMA file from a {@link File}.
     */
    public void read(@NotNull File fmaFile) throws Exception {
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        this.zipFile = this.putCloseable(ZipFile.builder().setFile(fmaFile).get());
        this.readMetadata();
        this.readFrameNames();
    }

    protected void readFrameNames() {
        this.orderedFrameNames.clear();
        Objects.requireNonNull(this.zipFile).getEntries().asIterator().forEachRemaining(zipArchiveEntry -> {
            String name = zipArchiveEntry.getName();
            String universalName = name.replace("\\", "/");
            if (universalName.startsWith("frames/") && !universalName.equals("frames/") && name.toLowerCase().endsWith(".png")) {
                String withoutExtension = Files.getNameWithoutExtension(name);
                if (MathUtils.isInteger(withoutExtension)) {
                    this.orderedFrameNames.add(name);
                } else {
                    LOGGER.error("[FANCYMENU] Invalid PNG frame found in FMA file!", new IllegalStateException("Frame file name is not a valid number: " + name));
                }
            } else if (!name.toLowerCase().endsWith(".png")) {
                LOGGER.error("[FANCYMENU] Invalid frame found in FMA file!", new IllegalStateException("Frame file is not a valid PNG image: " + name));
            }
        });
        this.orderedFrameNames.sort((o1, o2) -> {
            int i1 = Integer.parseInt(Files.getNameWithoutExtension(o1));
            int i2 = Integer.parseInt(Files.getNameWithoutExtension(o2));
            return Integer.compare(i1, i2);
        });
    }

    protected void readMetadata() throws Exception {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry entry = Objects.requireNonNull(this.zipFile.getEntry("metadata.json"), "No metadata.json found in FMA file! Unable to read metadata!");
        List<String> metadataLines = FileUtils.readTextLinesFrom(this.zipFile.getInputStream(entry));
        StringBuilder metadataString = new StringBuilder();
        for (String line : metadataLines) {
            metadataString.append(line).append("\n");
        }
        this.metadata = Objects.requireNonNull(GSON.fromJson(metadataString.toString(), FmaMetadata.class), "Unable to parse metadata.json of FMA file!");
    }

    public int getFrameCount() {
        return this.orderedFrameNames.size();
    }

    @Nullable
    public FmaMetadata getMetadata() {
        return this.metadata;
    }

    @Nullable
    public BufferedImage getFrame(int index) throws IOException {
        Objects.requireNonNull(this.zipFile);
        InputStream in = null;
        try {
            if (this.getFrameCount()-1 < index) return null;
            ZipArchiveEntry entry = Objects.requireNonNull(this.zipFile.getEntry(this.orderedFrameNames.get(index)), "Failed to get FMA frame entry!");
            in = this.zipFile.getInputStream(entry);
            BufferedImage image = ImageIO.read(in);
            CloseableUtils.closeQuietly(in);
            return image;
        } catch (Exception ex) {
            CloseableUtils.closeQuietly(in);
            throw new IOException(ex);
        }
    }

    @Nullable
    public BufferedImage getFirstFrame() throws IOException {
        return this.getFrame(0);
    }

    /**
     * Returns the background.png image of the FMA file, if present. Returns NULL if no background.png file is present.
     */
    @Nullable
    public BufferedImage getBackgroundImage() throws IOException {
        InputStream in = null;
        ZipArchiveEntry entry = Objects.requireNonNull(this.zipFile).getEntry("background.png");
        if (entry != null) {
            try {
                in = this.zipFile.getInputStream(entry);
                BufferedImage image = ImageIO.read(in);
                CloseableUtils.closeQuietly(in);
                return image;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
        CloseableUtils.closeQuietly(in);
        return null;
    }

    protected <T extends AutoCloseable> T putCloseable(@NotNull T closeable) {
        this.closeables.add(closeable);
        return closeable;
    }

    @Override
    public void close() throws IOException {
        this.closeables.forEach(CloseableUtils::closeQuietly);
        this.closeables.clear();
    }

    public static class FmaMetadata {

        protected int loop_count;
        protected long frame_time;
        protected Map<Integer, Long> custom_frame_times;

        public int getLoopCount() {
            return this.loop_count;
        }

        public long getFrameTime() {
            return this.frame_time;
        }

        @Nullable
        public Map<Integer, Long> getCustomFrameTimes() {
            return this.custom_frame_times;
        }

        public long getFrameTimeForFrame(int frame) {
            if ((custom_frame_times != null) && custom_frame_times.containsKey(frame)) return custom_frame_times.get(frame);
            return this.getFrameTime();
        }

    }

}
