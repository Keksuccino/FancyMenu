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
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * Streaming texture backend that loads frames on-demand from the decoder
 */
public class FmaTextureBackend extends AbstractTexture {

    private static final Logger LOGGER = LogManager.getLogger();
    
    // Buffer configuration
    private static final int FRAMES_AHEAD = 8;  // Preload 8 frames ahead
    private static final int FRAMES_BEHIND = 3; // Keep 3 frames behind
    private static final int PRELOAD_BATCH_SIZE = 3; // Load 3 frames at once in background
    
    // The OpenGL texture ID that we update directly
    private int glTextureId = -1;
    private int textureWidth = 0;
    private int textureHeight = 0;

    // The single ResourceLocation that always points to our texture
    private final ResourceLocation textureLocation;

    // Reference to the decoder (kept alive during playback)
    private FmaDecoder decoder;
    private FmaDecoder.FmaMetadata metadata;
    
    // Animation state
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicBoolean introFinished = new AtomicBoolean(false);
    private volatile long lastFrameTime = 0;
    private final AtomicInteger currentFrameIndex = new AtomicInteger(-1); // -1 means not started
    private volatile boolean isIntroPlaying = false;
    private int totalFrames = 0;
    private int totalIntroFrames = 0;
    private int minFrameIndex = 0;  // Minimum frame index (frames might start at 1)
    private int minIntroFrameIndex = 0;
    
    // Loading state
    private final AtomicBoolean dimensionsSet = new AtomicBoolean(false);
    private final AtomicBoolean glInitialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean decoderReady = new AtomicBoolean(false);
    
    // Current frame on GPU (to avoid re-uploading same frame)
    private int lastUploadedFrameIndex = -1;
    private boolean lastUploadedWasIntro = false;
    
    // Background preloading
    private final ExecutorService preloadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FMA-Preloader");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY + 1);
        return t;
    });
    
    // Track preload tasks to avoid duplicates
    private final Set<String> pendingPreloads = ConcurrentHashMap.newKeySet();
    
    // Frame that's ready to upload (loaded in background)
    private final AtomicReference<FmaDecoder.FrameData> nextFrameReady = new AtomicReference<>();

    public FmaTextureBackend() {
        // Generate a unique ResourceLocation for this animated texture
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        this.textureLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "streaming_fma_" + uniqueId);
    }

    /**
     * Initialize with a decoder for streaming playback
     */
    public void initializeWithDecoder(FmaDecoder decoder) {
        if (this.decoder != null) {
            LOGGER.warn("[FANCYMENU] Decoder already set, ignoring new decoder");
            return;
        }
        
        this.decoder = decoder;
        this.metadata = decoder.getMetadata();
        
        if (!decoder.isReady()) {
            LOGGER.error("[FANCYMENU] Decoder not ready!");
            return;
        }
        
        this.textureWidth = decoder.getFrameWidth();
        this.textureHeight = decoder.getFrameHeight();
        this.totalFrames = decoder.getFrameCount();
        this.totalIntroFrames = decoder.getIntroFrameCount();
        
        // Get minimum frame indices (frames might not start at 0)
        if (!decoder.frameEntries.isEmpty()) {
            this.minFrameIndex = decoder.frameEntries.keySet().stream()
                .min(Integer::compareTo).orElse(0);
        }
        if (!decoder.introFrameEntries.isEmpty()) {
            this.minIntroFrameIndex = decoder.introFrameEntries.keySet().stream()
                .min(Integer::compareTo).orElse(0);
        }
        
        dimensionsSet.set(true);
        decoderReady.set(true);
        
        // Initialize OpenGL on render thread
        if (RenderSystem.isOnRenderThread()) {
            initializeGL();
        } else {
            MainThreadTaskExecutor.executeInMainThread(this::initializeGL, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
        
        // Start preloading first frames
        int startIndex = totalIntroFrames > 0 ? minIntroFrameIndex : minFrameIndex;
        preloadFramesAsync(startIndex, FRAMES_AHEAD, totalIntroFrames > 0);
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

        LOGGER.debug("[FANCYMENU] Created streaming GL texture with ID {} for {}x{} FMA animation", 
            glTextureId, textureWidth, textureHeight);

        // Register with TextureManager
        DirectFmaTexture directTexture = new DirectFmaTexture(glTextureId, textureWidth, textureHeight);
        Minecraft.getInstance().getTextureManager().register(textureLocation, directTexture);
    }
    
    /**
     * Preload frames asynchronously in the background
     */
    private void preloadFramesAsync(int startIndex, int count, boolean isIntro) {
        if (decoder == null || closed.get()) return;
        
        // Get the actual frame indices to preload
        Map<Integer, String> entries = isIntro ? decoder.introFrameEntries : decoder.frameEntries;
        List<Integer> indicesToPreload = entries.keySet().stream()
            .filter(i -> i >= startIndex)
            .sorted()
            .limit(count)
            .collect(Collectors.toList());
        
        if (indicesToPreload.isEmpty()) return;
        
        // Submit preload task
        String taskKey = startIndex + "_" + count + "_" + isIntro;
        if (pendingPreloads.add(taskKey)) {
            preloadExecutor.submit(() -> {
                try {
                    for (int idx : indicesToPreload) {
                        decoder.loadFrame(idx, isIntro);
                    }
                } catch (Exception e) {
                    LOGGER.error("[FANCYMENU] Failed to preload frames", e);
                } finally {
                    pendingPreloads.remove(taskKey);
                }
            });
        }
    }

    /**
     * Update animation and upload current frame
     */
    public void updateFrame() {
        if (closed.get() || !decoderReady.get()) return;

        if (!glInitialized.get() && RenderSystem.isOnRenderThread()) {
            initializeGL();
        }

        if (!glInitialized.get() || glTextureId < 0) return;
        if (!playing.get()) return;

        long currentTime = System.currentTimeMillis();
        
        // Initialize frame index if not started
        int frameIndex = currentFrameIndex.get();
        if (frameIndex == -1) {
            // Start with first frame
            if (totalIntroFrames > 0) {
                frameIndex = minIntroFrameIndex;
                isIntroPlaying = true;
            } else {
                frameIndex = minFrameIndex;
                isIntroPlaying = false;
            }
            currentFrameIndex.set(frameIndex);
        }
        
        isIntroPlaying = !introFinished.get() && totalIntroFrames > 0;
        
        // Get the actual frame indices from decoder
        Map<Integer, String> currentEntries = isIntroPlaying ? 
            decoder.introFrameEntries : decoder.frameEntries;
        
        if (currentEntries.isEmpty()) return;
        
        // Check if current frame exists
        if (!currentEntries.containsKey(frameIndex)) {
            // Find next valid frame
            final int currentFrameForLambda = frameIndex;  // Final copy for lambda
            Integer nextFrame = currentEntries.keySet().stream()
                .filter(i -> i > currentFrameForLambda)
                .min(Integer::compareTo)
                .orElse(null);
                
            if (nextFrame == null) {
                // No more frames in current sequence
                if (isIntroPlaying) {
                    // Transition to main animation
                    introFinished.set(true);
                    isIntroPlaying = false;
                    frameIndex = minFrameIndex;
                    currentFrameIndex.set(frameIndex);
                    nextFrameReady.set(null);
                } else {
                    // Loop main animation
                    frameIndex = minFrameIndex;
                    currentFrameIndex.set(frameIndex);
                }
            } else {
                frameIndex = nextFrame;
                currentFrameIndex.set(frameIndex);
            }
        }
        
        // Get frame timing
        long frameDelay = metadata != null ? 
            metadata.getFrameTimeForFrame(frameIndex, isIntroPlaying) : 
            50; // Default 50ms if no metadata
        
        // Check if it's time for next frame
        if (currentTime - lastFrameTime >= frameDelay) {
            // Upload current frame if not already uploaded
            if (lastUploadedFrameIndex != frameIndex || lastUploadedWasIntro != isIntroPlaying) {
                
                // Try to use preloaded frame first
                FmaDecoder.FrameData frame = null;
                if (nextFrameReady.get() != null && nextFrameReady.get().frameIndex == frameIndex) {
                    frame = nextFrameReady.getAndSet(null);
                }
                
                // If not preloaded, load now (will use cache if available)
                if (frame == null) {
                    frame = decoder.loadFrame(frameIndex, isIntroPlaying);
                }
                
                if (frame != null && frame.pixelData != null) {
                    uploadFrameToGPU(frame.pixelData, frame.width, frame.height);
                    lastUploadedFrameIndex = frameIndex;
                    lastUploadedWasIntro = isIntroPlaying;
                }
            }
            
            // Find next frame index
            Map<Integer, String> entries = isIntroPlaying ? 
                decoder.introFrameEntries : decoder.frameEntries;
            final int finalFrameIndex = frameIndex;  // Final copy for lambda
            Integer nextIndex = entries.keySet().stream()
                .filter(i -> i > finalFrameIndex)
                .min(Integer::compareTo)
                .orElse(null);
                
            if (nextIndex != null) {
                // Preload next frame
                preloadNextFrameIndex(nextIndex, isIntroPlaying);
                currentFrameIndex.set(nextIndex);
            } else {
                // Handle wrap-around or transition
                if (isIntroPlaying) {
                    // Transition to main
                    introFinished.set(true);
                    currentFrameIndex.set(minFrameIndex);
                    preloadNextFrameIndex(minFrameIndex, false);
                } else {
                    // Loop main
                    currentFrameIndex.set(minFrameIndex);
                    preloadNextFrameIndex(minFrameIndex, false);
                }
            }
            
            lastFrameTime = currentTime;
        }
    }
    
    /**
     * Load a specific frame index in background
     */
    private void preloadNextFrameIndex(int frameIndex, boolean isIntro) {
        if (decoder == null || closed.get()) return;
        
        // Load frame in background
        preloadExecutor.submit(() -> {
            try {
                FmaDecoder.FrameData frame = decoder.loadFrame(frameIndex, isIntro);
                if (frame != null) {
                    // Store for quick access
                    FmaDecoder.FrameData oldFrame = nextFrameReady.getAndSet(frame);
                    // Note: We don't free oldFrame here as it might still be in decoder's cache
                }
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Failed to preload frame {}", frameIndex, e);
            }
        });
        
        // Also trigger batch preload for frames further ahead
        Map<Integer, String> entries = isIntro ? decoder.introFrameEntries : decoder.frameEntries;
        List<Integer> upcomingIndices = entries.keySet().stream()
            .filter(i -> i > frameIndex)
            .sorted()
            .limit(FRAMES_AHEAD)
            .collect(Collectors.toList());
            
        if (!upcomingIndices.isEmpty()) {
            for (int idx : upcomingIndices) {
                String taskKey = idx + "_1_" + isIntro;
                if (pendingPreloads.add(taskKey)) {
                    preloadExecutor.submit(() -> {
                        try {
                            decoder.loadFrame(idx, isIntro);
                        } catch (Exception e) {
                            LOGGER.debug("[FANCYMENU] Failed to preload frame {}", idx, e);
                        } finally {
                            pendingPreloads.remove(taskKey);
                        }
                    });
                }
            }
        }
    }

    /**
     * Upload frame to GPU
     */
    private void uploadFrameToGPU(ByteBuffer pixelData, int width, int height) {
        if (glTextureId < 0 || pixelData == null) return;

        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("[FANCYMENU] Attempted to upload texture from wrong thread!");
            return;
        }

        try {
            GlStateManager._bindTexture(glTextureId);
            GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, width);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
            
            // Use glTexSubImage2D for better performance (updates existing texture)
            glTexSubImage2D(
                GL_TEXTURE_2D,
                0, 0, 0,  // mipmap level and offset
                width,
                height,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                pixelData
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
        
        // Initialize frame index if not started
        if (currentFrameIndex.get() == -1) {
            if (totalIntroFrames > 0) {
                currentFrameIndex.set(minIntroFrameIndex);
            } else {
                currentFrameIndex.set(minFrameIndex);
            }
        }
        
        // Start preloading frames
        int currentIndex = currentFrameIndex.get();
        boolean isIntro = !introFinished.get() && totalIntroFrames > 0;
        preloadFramesAsync(currentIndex, FRAMES_AHEAD, isIntro);
    }

    public void stop() {
        playing.set(false);
        currentFrameIndex.set(-1);  // Reset to not started
        introFinished.set(false);
        lastUploadedFrameIndex = -1;
        nextFrameReady.set(null);
        
        // Clear decoder cache to free memory
        if (decoder != null) {
            decoder.clearCache();
        }
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
    
    public boolean isReady() {
        return decoderReady.get();
    }
    
    // For backwards compatibility - these methods now do nothing as we stream
    public void initialize(int width, int height) {
        // No-op - dimensions come from decoder
    }
    
    public void addFrame(byte[] pngData, long delayMs, boolean isIntro) {
        // No-op - frames come from decoder
    }
    
    public void addFrameDirect(ByteBuffer rgbaPixelData, long delayMs, boolean isIntro) {
        // No-op - frames come from decoder
    }

    public void close() {
        if (closed.getAndSet(true)) return;
        
        // Shutdown preload executor
        preloadExecutor.shutdown();
        try {
            if (!preloadExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                preloadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            preloadExecutor.shutdownNow();
        }
        
        // Clear next frame
        nextFrameReady.set(null);
        
        // Close decoder
        if (decoder != null) {
            try {
                decoder.close();
            } catch (Exception e) {
                LOGGER.error("[FANCYMENU] Failed to close decoder", e);
            }
            decoder = null;
        }

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
                    "FMA Streaming Direct Texture",
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
