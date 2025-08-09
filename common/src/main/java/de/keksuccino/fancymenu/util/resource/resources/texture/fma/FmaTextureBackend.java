package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

public class FmaTextureBackend extends AbstractTexture {

    private static final Logger LOGGER = LogManager.getLogger();
    
    // How many frames to keep decoded at once (sliding window)
    private static final int FRAME_CACHE_SIZE = 3;
    
    // The OpenGL texture ID that we update directly
    private int glTextureId = -1;
    private int textureWidth = 0;
    private int textureHeight = 0;

    // The single ResourceLocation that always points to our texture
    private final ResourceLocation textureLocation;

    // Frame data storage - compressed PNG bytes (memory efficient)
    private final List<CompressedFrame> frames = new ArrayList<>();
    private final List<CompressedFrame> introFrames = new ArrayList<>();
    
    // Cache of decoded frames (sliding window)
    private final Map<Integer, DecodedFrame> frameCache = new LinkedHashMap<>(FRAME_CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, DecodedFrame> eldest) {
            if (size() > FRAME_CACHE_SIZE) {
                // Free the ByteBuffer when evicting from cache
                eldest.getValue().free();
                return true;
            }
            return false;
        }
    };

    // Animation state
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicBoolean introFinished = new AtomicBoolean(false);
    private volatile long lastFrameTime = 0;
    private final AtomicInteger currentFrameIndex = new AtomicInteger(0);
    private volatile boolean isIntroPlaying = false;

    // Loading state
    private final AtomicBoolean dimensionsSet = new AtomicBoolean(false);
    private final AtomicBoolean glInitialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    // Current frame on GPU (to avoid re-uploading same frame)
    private int lastUploadedFrameIndex = -1;
    private boolean lastUploadedWasIntro = false;

    public FmaTextureBackend() {
        // Generate a unique ResourceLocation for this animated texture
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        this.textureLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "efficient_fma_" + uniqueId);
    }

    /**
     * Initialize the texture dimensions
     */
    public void initialize(int width, int height) {
        if (dimensionsSet.getAndSet(true)) return;

        this.textureWidth = width;
        this.textureHeight = height;

        // Schedule GL initialization on the main/render thread
        if (RenderSystem.isOnRenderThread()) {
            initializeGL();
        } else {
            MainThreadTaskExecutor.executeInMainThread(this::initializeGL, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
    }

    /**
     * Initialize OpenGL resources
     */
    private void initializeGL() {
        if (glInitialized.getAndSet(true)) return;
        if (!dimensionsSet.get()) return;

        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("[FANCYMENU] Attempted to create GL texture from wrong thread!");
            return;
        }

        // Create the OpenGL texture
        glTextureId = GlStateManager._genTexture();
        GlStateManager._bindTexture(glTextureId);

        // Set texture parameters
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Allocate texture memory
        GlStateManager._texImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            textureWidth,
            textureHeight,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null
        );

        LOGGER.debug("[FANCYMENU] Created efficient GL texture with ID {} for {}x{} FMA animation", 
            glTextureId, textureWidth, textureHeight);

        // Register with TextureManager
        DirectFmaTexture directTexture = new DirectFmaTexture(glTextureId, textureWidth, textureHeight);
        Minecraft.getInstance().getTextureManager().register(textureLocation, directTexture);
    }

    /**
     * Add a frame as compressed PNG bytes (memory efficient)
     */
    public void addFrame(byte[] pngData, long delayMs, boolean isIntro) {
        CompressedFrame frame = new CompressedFrame(pngData, delayMs);
        if (isIntro) {
            introFrames.add(frame);
        } else {
            frames.add(frame);
        }
    }
    
    /**
     * Add a frame from a ByteBuffer (takes ownership and stores it directly)
     * This avoids copying to heap memory, keeping the data off-heap.
     */
    public void addFrameDirect(ByteBuffer rgbaPixelData, long delayMs, boolean isIntro) {
        // Store the ByteBuffer directly without copying to avoid heap usage
        // The ByteBuffer is already off-heap from STBImage
        
        // Don't free it here - we'll keep it and free it when the frame is evicted or closed
        CompressedFrame frame = new CompressedFrame(rgbaPixelData, delayMs);
        if (isIntro) {
            introFrames.add(frame);
        } else {
            frames.add(frame);
        }
    }

    /**
     * Get or decode a frame on demand
     */
    private DecodedFrame getDecodedFrame(int index, boolean isIntro) {
        return getDecodedFrame(index, isIntro, true);
    }
    
    /**
     * Get or decode a frame on demand with optional preloading
     */
    private DecodedFrame getDecodedFrame(int index, boolean isIntro, boolean preloadNext) {
        // Create cache key that includes intro flag
        int cacheKey = isIntro ? -(index + 1) : index;
        
        // Check cache first
        DecodedFrame cached = frameCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Decode the frame
        List<CompressedFrame> frameList = isIntro ? introFrames : frames;
        if (index < 0 || index >= frameList.size()) return null;
        
        CompressedFrame compressed = frameList.get(index);
        ByteBuffer decoded = compressed.decode(textureWidth, textureHeight);
        
        if (decoded == null) {
            LOGGER.error("[FANCYMENU] Failed to decode frame {}", index);
            return null;
        }
        
        // Create DecodedFrame with correct allocator flag
        // If it's a direct frame (ByteBuffer from decoder), we shouldn't free it
        // If it was decoded from PNG or wrapped from byte array, we should free it
        boolean shouldFree = !compressed.isDirect;
        boolean isStbAllocated = !compressed.isRawRGBA || compressed.isDirect;
        
        DecodedFrame decodedFrame = new DecodedFrame(decoded, textureWidth, textureHeight, 
            isStbAllocated, shouldFree);
        
        // Cache it (will automatically evict oldest if cache is full)
        frameCache.put(cacheKey, decodedFrame);
        
        // Pre-decode next frame for smooth playback (only if requested to avoid recursion)
        if (preloadNext) {
            preloadNextFrame(index, isIntro);
        }
        
        return decodedFrame;
    }
    
    /**
     * Preload the next frame in sequence (without recursive preloading)
     */
    private void preloadNextFrame(int currentIndex, boolean isIntro) {
        int nextIndex = currentIndex + 1;
        List<CompressedFrame> frameList = isIntro ? introFrames : frames;
        
        // Create cache key for next frame
        int nextCacheKey;
        boolean nextIsIntro = isIntro;
        
        if (nextIndex >= frameList.size()) {
            if (!isIntro && !frames.isEmpty()) {
                // Preload first frame for loop
                nextIndex = 0;
                nextCacheKey = 0;
                nextIsIntro = false;
            } else {
                return; // Nothing to preload
            }
        } else {
            nextCacheKey = isIntro ? -(nextIndex + 1) : nextIndex;
        }
        
        // Check if already cached
        if (frameCache.containsKey(nextCacheKey)) {
            return; // Already in cache
        }
        
        // Decode without triggering another preload (false parameter)
        getDecodedFrame(nextIndex, nextIsIntro, false);
    }

    /**
     * Update animation and upload current frame
     */
    public void updateFrame() {
        if (closed.get() || !dimensionsSet.get()) return;

        if (!glInitialized.get() && RenderSystem.isOnRenderThread()) {
            initializeGL();
        }

        if (!glInitialized.get() || glTextureId < 0) return;
        if (!playing.get()) return;

        long currentTime = System.currentTimeMillis();
        
        // Determine current frame
        int frameIndex = currentFrameIndex.get();
        isIntroPlaying = !introFinished.get() && !introFrames.isEmpty();
        List<CompressedFrame> currentList = isIntroPlaying ? introFrames : frames;
        
        if (currentList.isEmpty()) return;
        
        // Check if we need to advance
        if (frameIndex >= currentList.size()) {
            if (isIntroPlaying) {
                // Intro finished
                introFinished.set(true);
                isIntroPlaying = false;
                currentFrameIndex.set(0);
                frameIndex = 0;
                currentList = frames;
            } else {
                // Loop main animation
                currentFrameIndex.set(0);
                frameIndex = 0;
            }
        }
        
        CompressedFrame currentFrame = currentList.get(frameIndex);
        
        // Check if it's time for next frame
        if (currentTime - lastFrameTime >= currentFrame.delayMs) {
            // Upload current frame if not already uploaded
            if (lastUploadedFrameIndex != frameIndex || lastUploadedWasIntro != isIntroPlaying) {
                DecodedFrame decoded = getDecodedFrame(frameIndex, isIntroPlaying);
                if (decoded != null) {
                    uploadFrameToGPU(decoded);
                    lastUploadedFrameIndex = frameIndex;
                    lastUploadedWasIntro = isIntroPlaying;
                }
            }
            
            // Advance to next frame
            currentFrameIndex.incrementAndGet();
            lastFrameTime = currentTime;
        }
    }

    /**
     * Upload frame to GPU
     */
    private void uploadFrameToGPU(DecodedFrame frame) {
        if (glTextureId < 0 || frame == null || frame.pixelData == null) return;

        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("[FANCYMENU] Attempted to upload texture from wrong thread!");
            return;
        }

        try {
            GlStateManager._bindTexture(glTextureId);
            GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, frame.width);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
            
            // Use glTexSubImage2D for better performance (updates existing texture)
            glTexSubImage2D(
                GL_TEXTURE_2D,
                0, 0, 0,  // mipmap level and offset
                frame.width,
                frame.height,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                frame.pixelData
            );
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to upload frame to GPU", e);
        }
    }

    public ResourceLocation getTextureLocation() {
        if (!glInitialized.get()) {
            if (RenderSystem.isOnRenderThread() && dimensionsSet.get()) {
                initializeGL();
            }
            if (!glInitialized.get()) {
                return null;
            }
        }

        updateFrame();
        return textureLocation;
    }

    public void play() {
        playing.set(true);
        lastFrameTime = System.currentTimeMillis();
    }

    public void stop() {
        playing.set(false);
        currentFrameIndex.set(0);
        introFinished.set(false);
        lastUploadedFrameIndex = -1;
    }

    public void reset() {
        stop();
    }

    public void pause() {
        playing.set(false);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public int getWidth() {
        return textureWidth;
    }

    public int getHeight() {
        return textureHeight;
    }

    public void close() {
        if (closed.getAndSet(true)) return;

        // Clear frame cache
        frameCache.values().forEach(DecodedFrame::free);
        frameCache.clear();
        
        // Clear compressed frames and free any direct ByteBuffers
        frames.forEach(CompressedFrame::cleanup);
        introFrames.forEach(CompressedFrame::cleanup);
        frames.clear();
        introFrames.clear();

        // Delete GL texture
        if (glTextureId >= 0) {
            if (RenderSystem.isOnRenderThread()) {
                GlStateManager._deleteTexture(glTextureId);
            } else {
                int textureToDelete = glTextureId;
                MainThreadTaskExecutor.executeInMainThread(() -> 
                    GlStateManager._deleteTexture(textureToDelete), 
                    MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
            glTextureId = -1;
        }

        // Unregister from TextureManager
        if (glInitialized.get()) {
            if (RenderSystem.isOnRenderThread()) {
                Minecraft.getInstance().getTextureManager().release(textureLocation);
            } else {
                MainThreadTaskExecutor.executeInMainThread(() -> 
                    Minecraft.getInstance().getTextureManager().release(textureLocation), 
                    MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }
    }

    /**
     * Compressed frame data (PNG bytes or raw RGBA)
     * Can store either byte array (heap) or ByteBuffer (off-heap) to save memory
     */
    private static class CompressedFrame {
        final byte[] data;  // For PNG or manually added raw RGBA
        final ByteBuffer directBuffer;  // For ByteBuffers from decoder (off-heap)
        final long delayMs;
        final boolean isRawRGBA;
        final boolean isDirect;  // True if using directBuffer instead of data array

        // Constructor for byte array data
        CompressedFrame(byte[] data, long delayMs) {
            this(data, delayMs, false);
        }
        
        // Constructor for byte array data with raw RGBA flag
        CompressedFrame(byte[] data, long delayMs, boolean isRawRGBA) {
            this.data = data;
            this.directBuffer = null;
            this.delayMs = delayMs;
            this.isRawRGBA = isRawRGBA;
            this.isDirect = false;
        }
        
        // Constructor for direct ByteBuffer (always raw RGBA)
        CompressedFrame(ByteBuffer directBuffer, long delayMs) {
            this.data = null;
            this.directBuffer = directBuffer;
            this.delayMs = delayMs;
            this.isRawRGBA = true;
            this.isDirect = true;
        }

        ByteBuffer decode(int expectedWidth, int expectedHeight) {
            if (isDirect) {
                // Already have a direct ByteBuffer, just return it
                // Don't free it - it's owned by this CompressedFrame
                return directBuffer;
            } else if (isRawRGBA) {
                // Data is already RGBA in byte array, wrap it in a ByteBuffer
                ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
                buffer.put(data);
                buffer.flip();
                return buffer;
            } else {
                // Decode PNG to RGBA
                ByteBuffer pngBuffer = MemoryUtil.memAlloc(data.length);
                pngBuffer.put(data);
                pngBuffer.flip();
                
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer widthBuf = stack.mallocInt(1);
                    IntBuffer heightBuf = stack.mallocInt(1);
                    IntBuffer channelsBuf = stack.mallocInt(1);
                    
                    ByteBuffer decoded = STBImage.stbi_load_from_memory(
                        pngBuffer, widthBuf, heightBuf, channelsBuf, 4
                    );
                    
                    if (decoded == null) {
                        LOGGER.error("[FANCYMENU] Failed to decode frame: " + STBImage.stbi_failure_reason());
                        return null;
                    }
                    
                    return decoded;
                } finally {
                    MemoryUtil.memFree(pngBuffer);
                }
            }
        }
        
        void cleanup() {
            // Free the direct ByteBuffer if we own one
            if (isDirect && directBuffer != null) {
                STBImage.stbi_image_free(directBuffer);
            }
        }
    }

    /**
     * Decoded frame ready for GPU upload
     */
    private static class DecodedFrame {
        final ByteBuffer pixelData;
        final int width;
        final int height;
        final boolean isStbAllocated; // Track which allocator was used
        final boolean shouldFree; // Whether we should free this buffer

        DecodedFrame(ByteBuffer pixelData, int width, int height, boolean isStbAllocated, boolean shouldFree) {
            this.pixelData = pixelData;
            this.width = width;
            this.height = height;
            this.isStbAllocated = isStbAllocated;
            this.shouldFree = shouldFree;
        }

        void free() {
            // Only free if we should (not owned by a CompressedFrame)
            if (pixelData != null && pixelData.isDirect() && shouldFree) {
                if (isStbAllocated) {
                    // Free with STBImage if it was allocated by STBImage
                    STBImage.stbi_image_free(pixelData);
                } else {
                    // Free with MemoryUtil if it was allocated by MemoryUtil
                    MemoryUtil.memFree(pixelData);
                }
            }
        }
    }

    /**
     * Direct texture wrapper (same as before)
     */
    private static class DirectFmaTexture extends AbstractTexture {
        DirectFmaTexture(int glId, int width, int height) {
            DirectGlTexture glTexture = new DirectGlTexture(glId, width, height);
            this.texture = glTexture;
            this.textureView = new DirectGlTextureView(glTexture, 0, 1);
        }

        @Override
        public void close() {
            this.texture = null;
            this.textureView = null;
        }

        private static class DirectGlTexture extends GlTexture {
            protected DirectGlTexture(int textureId, int width, int height) {
                super(
                    GpuTexture.USAGE_TEXTURE_BINDING,
                    "FMA Efficient Direct Texture",
                    TextureFormat.RGBA8,
                    width, height, 1, 1, textureId
                );
                this.closed = false;
            }

            @Override
            public void close() {
                this.closed = true;
            }
        }

        private static class DirectGlTextureView extends GlTextureView {
            protected DirectGlTextureView(GlTexture texture, int baseMipLevel, int mipLevels) {
                super(texture, baseMipLevel, mipLevels);
            }
        }
    }
}