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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Streaming FMA decoder that reads frames on-demand from the ZIP file
 * instead of loading everything into memory at once.
 */
public class FmaDecoder implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    
    @Nullable
    protected ZipFile zipFile = null;
    @Nullable
    protected FmaMetadata metadata = null;
    
    // Frame index maps: frame number -> ZIP entry name (package-private for backend access)
    final Map<Integer, String> frameEntries = new TreeMap<>();
    final Map<Integer, String> introFrameEntries = new TreeMap<>();
    
    // Cache for recently decoded frames (thread-safe)
    protected final Map<FrameKey, FrameData> frameCache = new ConcurrentHashMap<>();
    protected static final int MAX_CACHE_SIZE = 15; // Keep up to 15 frames in cache
    
    // Thread safety for ZIP file access
    protected final ReadWriteLock zipLock = new ReentrantReadWriteLock();
    
    // Frame dimensions (detected from first frame)
    protected int frameWidth = 0;
    protected int frameHeight = 0;
    protected int totalFrames = 0;
    protected int totalIntroFrames = 0;
    
    // Track if we're using in-memory ZIP (from InputStream)
    protected boolean isInMemory = false;
    
    /**
     * Key for frame cache that includes intro flag
     */
    protected static class FrameKey {
        public final int index;
        public final boolean isIntro;
        
        public FrameKey(int index, boolean isIntro) {
            this.index = index;
            this.isIntro = isIntro;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FrameKey frameKey = (FrameKey) o;
            return index == frameKey.index && isIntro == frameKey.isIntro;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(index, isIntro);
        }
    }
    
    /**
     * Represents a single frame with its pixel data ready for OpenGL upload
     */
    public static class FrameData {
        public ByteBuffer pixelData;  // Direct ByteBuffer in RGBA format
        public final int width;
        public final int height;
        public final int frameIndex;
        public final long timestamp; // When this frame was cached
        
        public FrameData(ByteBuffer pixelData, int width, int height, int frameIndex) {
            this.pixelData = pixelData;
            this.width = width;
            this.height = height;
            this.frameIndex = frameIndex;
            this.timestamp = System.currentTimeMillis();
        }
        
        public void free() {
            if (pixelData != null) {
                STBImage.stbi_image_free(pixelData);
                pixelData = null;
            }
        }
    }
    
    /**
     * Opens an FMA file from an InputStream for streaming playback.
     * The entire ZIP is kept in memory for random access.
     */
    public void openStream(@NotNull InputStream in) throws IOException {
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        try {
            byte[] data = in.readAllBytes();
            this.zipFile = ZipFile.builder()
                .setSeekableByteChannel(new SeekableInMemoryByteChannel(data))
                .get();
            this.isInMemory = true;
            this.initializeDecoder();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        } finally {
            in.close();
        }
    }
    
    /**
     * Opens an FMA file from a File for streaming playback.
     * The file remains open for on-demand frame reading.
     */
    public void openFile(@NotNull File fmaFile) throws IOException {
        if (this.zipFile != null) throw new IllegalStateException("The decoder is already reading a file!");
        try {
            this.zipFile = ZipFile.builder().setFile(fmaFile).get();
            this.isInMemory = false;
            this.initializeDecoder();
        } catch (Exception ex) {
            this.close();
            throw new IOException(ex);
        }
    }
    
    /**
     * Initialize the decoder by reading metadata and indexing frames
     */
    protected void initializeDecoder() throws IOException {
        try {
            this.readMetadata();
            this.indexFrames();
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to initialize FMA decoder", e);
            throw new IOException("Failed to initialize decoder: " + e.getMessage(), e);
        }
    }
    
    /**
     * Index all frames in the ZIP without loading them
     */
    protected void indexFrames() throws IOException {
        Objects.requireNonNull(this.zipFile);
        
        zipLock.readLock().lock();
        try {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (name.startsWith("frames/") && name.endsWith(".png")) {
                    String frameName = name.substring("frames/".length(), name.length() - 4);
                    try {
                        int frameIndex = Integer.parseInt(frameName);
                        frameEntries.put(frameIndex, name);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Invalid frame name: " + frameName);
                    }
                } else if (name.startsWith("intro_frames/") && name.endsWith(".png")) {
                    String frameName = name.substring("intro_frames/".length(), name.length() - 4);
                    try {
                        int frameIndex = Integer.parseInt(frameName);
                        introFrameEntries.put(frameIndex, name);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Invalid intro frame name: " + frameName);
                    }
                }
            }
            
            totalFrames = frameEntries.size();
            totalIntroFrames = introFrameEntries.size();
            
            // Check if we found any frames
            if (totalFrames == 0 && totalIntroFrames == 0) {
                LOGGER.error("[FANCYMENU] No frames found in FMA file!");
                throw new IOException("No frames found in FMA file");
            }
            
            // Load first frame to get dimensions - try regular frames first, then intro
            boolean dimensionsFound = false;
            
            if (!frameEntries.isEmpty()) {
                // Try to find the first frame (could be 0 or 1 based)
                Integer firstIndex = frameEntries.keySet().stream().min(Integer::compareTo).orElse(null);
                if (firstIndex != null) {
                    try {
                        FrameData firstFrame = loadFrameInternal(firstIndex, false);
                        if (firstFrame != null) {
                            frameWidth = firstFrame.width;
                            frameHeight = firstFrame.height;
                            // Keep it in cache
                            frameCache.put(new FrameKey(firstIndex, false), firstFrame);
                            dimensionsFound = true;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[FANCYMENU] Failed to load first regular frame (index={}) for dimensions", firstIndex, e);
                    }
                }
            }
            
            // If regular frames failed, try intro frames
            if (!dimensionsFound && !introFrameEntries.isEmpty()) {
                Integer firstIntroIndex = introFrameEntries.keySet().stream().min(Integer::compareTo).orElse(null);
                if (firstIntroIndex != null) {
                    try {
                        FrameData firstIntroFrame = loadFrameInternal(firstIntroIndex, true);
                        if (firstIntroFrame != null) {
                            frameWidth = firstIntroFrame.width;
                            frameHeight = firstIntroFrame.height;
                            // Keep it in cache
                            frameCache.put(new FrameKey(firstIntroIndex, true), firstIntroFrame);
                            dimensionsFound = true;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[FANCYMENU] Failed to load first intro frame (index={}) for dimensions", firstIntroIndex, e);
                    }
                }
            }
            
            if (!dimensionsFound) {
                throw new IOException("Failed to determine frame dimensions from FMA file");
            }
            
            LOGGER.debug("[FANCYMENU] Indexed FMA: {} frames, {} intro frames, {}x{}", 
                totalFrames, totalIntroFrames, frameWidth, frameHeight);
            
        } finally {
            zipLock.readLock().unlock();
        }
    }
    
    /**
     * Load a specific frame on-demand. Uses cache if available.
     * 
     * @param index Frame index
     * @param isIntro Whether this is an intro frame
     * @return Frame data or null if not found
     */
    @Nullable
    public FrameData loadFrame(int index, boolean isIntro) {
        FrameKey key = new FrameKey(index, isIntro);
        
        // Check cache first
        FrameData cached = frameCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        // Load from ZIP
        FrameData frame = loadFrameInternal(index, isIntro);
        if (frame != null) {
            // Add to cache
            frameCache.put(key, frame);
            
            // Clean old entries if cache is too large
            cleanCache();
        }
        
        return frame;
    }
    
    /**
     * Internal method to load a frame from the ZIP file
     */
    @Nullable
    protected FrameData loadFrameInternal(int index, boolean isIntro) {
        Map<Integer, String> entries = isIntro ? introFrameEntries : frameEntries;
        String entryName = entries.get(index);
        
        if (entryName == null) {
            LOGGER.debug("[FANCYMENU] Frame entry not found: index={}, isIntro={}", index, isIntro);
            return null;
        }
        
        zipLock.readLock().lock();
        try {
            ZipArchiveEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                LOGGER.error("[FANCYMENU] ZIP entry not found: {}", entryName);
                return null;
            }
            
            try (InputStream entryStream = zipFile.getInputStream(entry)) {
                byte[] pngData = entryStream.readAllBytes();
                
                if (pngData.length == 0) {
                    LOGGER.error("[FANCYMENU] Empty PNG data for frame: {}", entryName);
                    return null;
                }
                
                // Decode PNG to RGBA ByteBuffer
                ByteBuffer pngBuffer = MemoryUtil.memAlloc(pngData.length);
                pngBuffer.put(pngData);
                pngBuffer.flip();
                
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer widthBuf = stack.mallocInt(1);
                    IntBuffer heightBuf = stack.mallocInt(1);
                    IntBuffer channelsBuf = stack.mallocInt(1);
                    
                    ByteBuffer pixelData = STBImage.stbi_load_from_memory(
                        pngBuffer, 
                        widthBuf, 
                        heightBuf, 
                        channelsBuf, 
                        4  // Force RGBA
                    );
                    
                    if (pixelData == null) {
                        LOGGER.error("[FANCYMENU] Failed to decode frame {}: {}", entryName, STBImage.stbi_failure_reason());
                        return null;
                    }
                    
                    int width = widthBuf.get(0);
                    int height = heightBuf.get(0);
                    
                    if (width <= 0 || height <= 0) {
                        LOGGER.error("[FANCYMENU] Invalid frame dimensions: {}x{} for {}", width, height, entryName);
                        STBImage.stbi_image_free(pixelData);
                        return null;
                    }
                    
                    return new FrameData(pixelData, width, height, index);
                } finally {
                    MemoryUtil.memFree(pngBuffer);
                }
            }
        } catch (IOException e) {
            LOGGER.error("[FANCYMENU] Failed to load frame {} (intro={}): {}", index, isIntro, e.getMessage(), e);
            return null;
        } finally {
            zipLock.readLock().unlock();
        }
    }
    
    /**
     * Preload multiple frames for smooth playback
     * This method is kept for backward compatibility but isn't used by the new streaming backend
     * 
     * @param startIndex Starting frame index
     * @param count Number of frames to preload
     * @param isIntro Whether these are intro frames
     */
    public void preloadFrames(int startIndex, int count, boolean isIntro) {
        Map<Integer, String> entries = isIntro ? introFrameEntries : frameEntries;
        
        // Get actual frame indices starting from startIndex
        List<Integer> indicesToLoad = entries.keySet().stream()
            .filter(i -> i >= startIndex)
            .sorted()
            .limit(count)
            .collect(Collectors.toList());
        
        for (int index : indicesToLoad) {
            FrameKey key = new FrameKey(index, isIntro);
            if (!frameCache.containsKey(key)) {
                loadFrame(index, isIntro);
            }
        }
    }
    
    /**
     * Clean old frames from cache when it gets too large
     */
    protected void cleanCache() {
        if (frameCache.size() <= MAX_CACHE_SIZE) {
            return;
        }
        
        // Find and remove oldest entries
        List<Map.Entry<FrameKey, FrameData>> entries = new ArrayList<>(frameCache.entrySet());
        entries.sort(Comparator.comparingLong(e -> e.getValue().timestamp));
        
        int toRemove = frameCache.size() - MAX_CACHE_SIZE + 5; // Remove a few extra to avoid frequent cleaning
        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            Map.Entry<FrameKey, FrameData> entry = entries.get(i);
            FrameData removed = frameCache.remove(entry.getKey());
            if (removed != null) {
                removed.free();
            }
        }
    }
    
    /**
     * Clear all cached frames
     */
    public void clearCache() {
        for (FrameData frame : frameCache.values()) {
            frame.free();
        }
        frameCache.clear();
    }
    
    /**
     * Get a frame's pixel data as a direct ByteBuffer ready for OpenGL upload.
     * This loads the frame on-demand if not cached.
     */
    @Nullable
    public ByteBuffer getFramePixelData(int index, boolean isIntro) {
        FrameData frame = loadFrame(index, isIntro);
        return frame != null ? frame.pixelData : null;
    }
    
    protected void readMetadata() throws IOException {
        Objects.requireNonNull(this.zipFile);
        
        zipLock.readLock().lock();
        try {
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
        } finally {
            zipLock.readLock().unlock();
        }
    }
    
    public int getFrameCount() {
        return totalFrames;
    }
    
    public int getIntroFrameCount() {
        return totalIntroFrames;
    }
    
    public boolean hasIntroFrames() {
        return totalIntroFrames > 0;
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
    
    /**
     * Check if the decoder is ready (has indexed frames and knows dimensions)
     */
    public boolean isReady() {
        return zipFile != null && frameWidth > 0 && frameHeight > 0;
    }
    
    @Override
    public void close() throws IOException {
        // Clear cache first
        clearCache();
        
        // Close ZIP file
        if (zipFile != null) {
            zipLock.writeLock().lock();
            try {
                zipFile.close();
                zipFile = null;
            } finally {
                zipLock.writeLock().unlock();
            }
        }
        
        frameEntries.clear();
        introFrameEntries.clear();
        metadata = null;
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
                return this.getFrameTimeIntro();
            } else {
                if ((custom_frame_times != null) && custom_frame_times.containsKey(frame)) 
                    return custom_frame_times.get(frame);
                return this.getFrameTime();
            }
        }
    }
}
