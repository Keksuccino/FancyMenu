package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FmaDecoder implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    
    @Nullable
    protected ZipFile zipFile = null;
    @Nullable
    protected FmaMetadata metadata = null;
    
    // Store decoded frames as direct ByteBuffers ready for OpenGL
    protected final List<FrameData> frames = new ArrayList<>();
    protected final List<FrameData> introFrames = new ArrayList<>();
    protected final List<AutoCloseable> closeables = new ArrayList<>();
    
    // Frame dimensions (assuming all frames have the same size)
    protected int frameWidth = 0;
    protected int frameHeight = 0;
    
    /**
     * Represents a single frame with its pixel data ready for OpenGL upload
     */
    public static class FrameData {
        public ByteBuffer pixelData;  // Direct ByteBuffer in RGBA format (non-final for ownership transfer)
        public final int width;
        public final int height;
        public final int frameIndex;
        
        public FrameData(ByteBuffer pixelData, int width, int height, int frameIndex) {
            this.pixelData = pixelData;
            this.width = width;
            this.height = height;
            this.frameIndex = frameIndex;
        }
        
        public void free() {
            if (pixelData != null) {
                STBImage.stbi_image_free(pixelData);
                pixelData = null; // Clear reference after freeing
            }
        }
    }
    
    /**
     * Reads an FMA file from an InputStream, keeping everything in memory
     */
    public void read(@NotNull InputStream in) throws IOException {
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        try {
            byte[] data = in.readAllBytes();
            this.zipFile = ZipFile.builder()
                .setSeekableByteChannel(new SeekableInMemoryByteChannel(data))
                .get();
            this.readMetadata();
            this.loadAllFramesToMemory();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        } finally {
            in.close();
        }
    }
    
    /**
     * Reads an FMA file from a File
     */
    public void read(@NotNull File fmaFile) throws IOException {
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        try {
            this.zipFile = ZipFile.builder().setFile(fmaFile).get();
            this.readMetadata();
            this.loadAllFramesToMemory();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }
    
    /**
     * Loads all frames directly into memory as ByteBuffers ready for OpenGL
     */
    protected void loadAllFramesToMemory() throws IOException {
        Objects.requireNonNull(this.zipFile);
        
        // Load main frames
        Map<Integer, FrameData> frameMap = new TreeMap<>();
        
        Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String name = entry.getName();
            
            if (name.startsWith("frames/") && name.endsWith(".png")) {
                String frameName = name.substring("frames/".length(), name.length() - 4);
                try {
                    int frameIndex = Integer.parseInt(frameName);
                    FrameData frameData = loadFrameFromZipEntry(entry, frameIndex);
                    if (frameData != null) {
                        frameMap.put(frameIndex, frameData);
                        // Set frame dimensions from first frame
                        if (frameWidth == 0) {
                            frameWidth = frameData.width;
                            frameHeight = frameData.height;
                        }
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid frame name: " + frameName);
                }
            } else if (name.startsWith("intro_frames/") && name.endsWith(".png")) {
                String frameName = name.substring("intro_frames/".length(), name.length() - 4);
                try {
                    int frameIndex = Integer.parseInt(frameName);
                    FrameData frameData = loadFrameFromZipEntry(entry, frameIndex);
                    if (frameData != null) {
                        introFrames.add(frameData);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid intro frame name: " + frameName);
                }
            }
        }
        
        // Add frames in order
        frames.addAll(frameMap.values());
        
        // Sort intro frames by index
        introFrames.sort(Comparator.comparingInt(f -> f.frameIndex));
    }
    
    /**
     * Loads a single frame from a ZIP entry and decodes it directly to a ByteBuffer
     * using STBImage (same library Minecraft uses for texture loading)
     */
    @Nullable
    protected FrameData loadFrameFromZipEntry(ZipArchiveEntry entry, int frameIndex) throws IOException {
        try (InputStream entryStream = zipFile.getInputStream(entry)) {
            byte[] pngData = entryStream.readAllBytes();
            
            // Use STBImage to decode PNG directly to RGBA ByteBuffer
            ByteBuffer pngBuffer = MemoryUtil.memAlloc(pngData.length);
            pngBuffer.put(pngData);
            pngBuffer.flip();
            
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer widthBuf = stack.mallocInt(1);
                IntBuffer heightBuf = stack.mallocInt(1);
                IntBuffer channelsBuf = stack.mallocInt(1);
                
                // Decode to RGBA format (4 channels) - this returns a direct ByteBuffer
                // This is the same format OpenGL expects with GL_RGBA
                ByteBuffer pixelData = STBImage.stbi_load_from_memory(
                    pngBuffer, 
                    widthBuf, 
                    heightBuf, 
                    channelsBuf, 
                    4  // Force RGBA
                );
                
                if (pixelData == null) {
                    LOGGER.error("Failed to decode frame: " + STBImage.stbi_failure_reason());
                    return null;
                }
                
                // Note: pixelData is now a direct ByteBuffer allocated by STBImage
                // It must be freed with STBImage.stbi_image_free() when done
                
                return new FrameData(
                    pixelData,
                    widthBuf.get(0),
                    heightBuf.get(0),
                    frameIndex
                );
            } finally {
                MemoryUtil.memFree(pngBuffer);
            }
        }
    }
    
    /**
     * Gets a frame's pixel data as a direct ByteBuffer ready for OpenGL upload.
     * This can be uploaded directly with glTexImage2D or glTexSubImage2D.
     * 
     * Format: RGBA, 8 bits per channel
     * The ByteBuffer is direct (off-heap) and should NOT be modified.
     * 
     * IMPORTANT: The returned ByteBuffer is still owned by this decoder.
     * If you need to keep it after the decoder is closed, use takeFramePixelData() instead.
     */
    @Nullable
    public ByteBuffer getFramePixelData(int index) {
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index).pixelData;
    }
    
    /**
     * Gets an intro frame's pixel data as a direct ByteBuffer.
     * 
     * IMPORTANT: The returned ByteBuffer is still owned by this decoder.
     * If you need to keep it after the decoder is closed, use takeIntroFramePixelData() instead.
     */
    @Nullable
    public ByteBuffer getIntroFramePixelData(int index) {
        if (index < 0 || index >= introFrames.size()) return null;
        return introFrames.get(index).pixelData;
    }
    
    /**
     * Takes ownership of a frame's pixel data. After calling this method,
     * the decoder will no longer free this ByteBuffer when closed.
     * The caller is responsible for freeing it with STBImage.stbi_image_free().
     */
    @Nullable
    public ByteBuffer takeFramePixelData(int index) {
        if (index < 0 || index >= frames.size()) return null;
        FrameData frame = frames.get(index);
        ByteBuffer data = frame.pixelData;
        if (data == null) {
            LOGGER.warn("[FANCYMENU] Frame {} already had ownership transferred or was null", index);
            return null;
        }
        frame.pixelData = null; // Remove reference so it won't be freed
        return data;
    }
    
    /**
     * Takes ownership of an intro frame's pixel data. After calling this method,
     * the decoder will no longer free this ByteBuffer when closed.
     * The caller is responsible for freeing it with STBImage.stbi_image_free().
     */
    @Nullable
    public ByteBuffer takeIntroFramePixelData(int index) {
        if (index < 0 || index >= introFrames.size()) return null;
        FrameData frame = introFrames.get(index);
        ByteBuffer data = frame.pixelData;
        if (data == null) {
            LOGGER.warn("[FANCYMENU] Intro frame {} already had ownership transferred or was null", index);
            return null;
        }
        frame.pixelData = null; // Remove reference so it won't be freed
        return data;
    }
    
    /**
     * Example of how to upload a frame to OpenGL (similar to MCEF)
     */
    public void uploadFrameToOpenGL(int frameIndex, int textureId) {
        ByteBuffer pixelData = getFramePixelData(frameIndex);
        if (pixelData == null) return;
        
        // This is how you'd upload it - exactly like MCEF does:
        // GlStateManager._bindTexture(textureId);
        // GL11.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, frameWidth, frameHeight, 0,
        //                   GL_RGBA, GL_UNSIGNED_BYTE, pixelData);
    }
    
    protected void readMetadata() throws IOException {
        Objects.requireNonNull(this.zipFile);
        ZipArchiveEntry metadataEntry = zipFile.getEntry("metadata.json");
        if (metadataEntry == null) {
            throw new FileNotFoundException("No metadata.json found in FMA file!");
        }
        
        try (InputStream in = zipFile.getInputStream(metadataEntry);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append("\n");
            }
            
            this.metadata = GSON.fromJson(json.toString(), FmaMetadata.class);
        }
    }
    
    public int getFrameCount() {
        return frames.size();
    }
    
    public int getIntroFrameCount() {
        return introFrames.size();
    }
    
    public boolean hasIntroFrames() {
        return !introFrames.isEmpty();
    }
    
    public int getFrameWidth() {
        return frameWidth;
    }
    
    public int getFrameHeight() {
        return frameHeight;
    }
    
    @Nullable
    public FmaMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public void close() throws IOException {
        // Free all STBImage-allocated buffers
        for (FrameData frame : frames) {
            frame.free();
        }
        frames.clear();
        
        for (FrameData frame : introFrames) {
            frame.free();
        }
        introFrames.clear();
        
        if (zipFile != null) {
            zipFile.close();
            zipFile = null;
        }
        
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        closeables.clear();
    }
    
    // FmaMetadata class remains the same
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
                if ((custom_frame_times_intro != null) && custom_frame_times_intro.containsKey(frame)) 
                    return custom_frame_times_intro.get(frame);
            } else {
                if ((custom_frame_times != null) && custom_frame_times.containsKey(frame)) 
                    return custom_frame_times.get(frame);
            }
            return isIntroFrame ? this.getFrameTimeIntro() : this.getFrameTime();
        }
    }
}
