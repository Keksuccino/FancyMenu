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
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;

/**
 * Optimized FMA texture backend that uses zero-copy OpenGL uploads like MCEF.
 * This stores frames as direct ByteBuffers and uploads them directly to OpenGL
 * without any intermediate conversions.
 */
public class FmaTextureBackend extends AbstractTexture {

    private static final Logger LOGGER = LogManager.getLogger();

    // The OpenGL texture ID that we update directly
    private int glTextureId = -1;
    private int textureWidth = 0;
    private int textureHeight = 0;

    // The single ResourceLocation that always points to our texture
    private final ResourceLocation textureLocation;

    // Frame data storage - direct ByteBuffers ready for OpenGL
    private final ConcurrentLinkedQueue<FrameData> frames = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FrameData> introFrames = new ConcurrentLinkedQueue<>();

    // Current frame being displayed
    private final AtomicReference<FrameData> currentFrame = new AtomicReference<>();

    // Animation state
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicBoolean introFinished = new AtomicBoolean(false);
    private volatile long lastFrameTime = 0;
    private final AtomicInteger currentFrameIndex = new AtomicInteger(0);

    // Loading state
    private final AtomicBoolean dimensionsSet = new AtomicBoolean(false);
    private final AtomicBoolean glInitialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FmaTextureBackend() {
        // Generate a unique ResourceLocation for this animated texture
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        this.textureLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "optimized_fma_" + uniqueId);
    }

    /**
     * Set the dimensions for the texture. This can be called from any thread.
     * We immediately schedule GL initialization on the render thread.
     */
    public void initialize(int width, int height) {
        if (dimensionsSet.getAndSet(true)) return;

        this.textureWidth = width;
        this.textureHeight = height;

        // Schedule GL initialization on the main/render thread
        if (RenderSystem.isOnRenderThread()) {
            initializeGL();
        } else {
            // Use MainThreadTaskExecutor to schedule on main thread
            MainThreadTaskExecutor.executeInMainThread(this::initializeGL, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
        }
    }

    /**
     * Initialize OpenGL resources. This must be called on the render thread.
     */
    private void initializeGL() {
        if (glInitialized.getAndSet(true)) return;
        if (!dimensionsSet.get()) return;

        // Must be on render thread
        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("[FANCYMENU] Attempted to create GL texture from wrong thread!");
            return;
        }

        // Create the OpenGL texture directly
        glTextureId = GlStateManager._genTexture();
        GlStateManager._bindTexture(glTextureId);

        // Set texture parameters
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Allocate texture memory with empty data
        GlStateManager._texImage2D(
            GL_TEXTURE_2D,
            0,  // mipmap level
            GL_RGBA,  // internal format
            textureWidth,
            textureHeight,
            0,  // border
            GL_RGBA,  // format
            GL_UNSIGNED_BYTE,
            null  // no initial data
        );

        LOGGER.debug("[FANCYMENU] Created GL texture with ID {} for {}x{} FMA animation", glTextureId, textureWidth, textureHeight);

        // Create a custom texture wrapper (like MCEFDirectTexture)
        DirectFmaTexture directTexture = new DirectFmaTexture(glTextureId, textureWidth, textureHeight);

        // Register with TextureManager
        Minecraft.getInstance().getTextureManager().register(textureLocation, directTexture);
    }

    /**
     * Add a frame to the animation using PNG bytes that will be decoded using STBImage.
     */
    public void addFrame(byte[] pngData, long delayMs, boolean isIntro) {
        // Decode the PNG data immediately using STBImage to get a direct ByteBuffer
        ByteBuffer pixelData = decodePngToDirectBuffer(pngData);
        if (pixelData != null) {
            FrameData frame = new FrameData(pixelData, textureWidth, textureHeight, delayMs);
            if (isIntro) {
                introFrames.offer(frame);
            } else {
                frames.offer(frame);
            }
        }
    }
    
    /**
     * Add a frame to the animation using a direct ByteBuffer already in RGBA format.
     * This is the most efficient method as it avoids any decoding.
     * The ByteBuffer should be allocated with STBImage.stbi_load_from_memory() to ensure
     * it can be freed with STBImage.stbi_image_free().
     * 
     * IMPORTANT: This method takes ownership of the ByteBuffer. It will be freed
     * when this FmaTextureBackend is closed.
     */
    public void addFrameDirect(ByteBuffer rgbaPixelData, long delayMs, boolean isIntro) {
        if (rgbaPixelData == null) {
            LOGGER.warn("[FANCYMENU] Attempted to add null frame data");
            return;
        }
        
        // Validate the buffer
        if (!rgbaPixelData.isDirect()) {
            LOGGER.error("[FANCYMENU] Frame data must be a direct ByteBuffer!");
            return;
        }
        
        int expectedSize = textureWidth * textureHeight * 4; // RGBA = 4 bytes per pixel
        if (rgbaPixelData.remaining() < expectedSize) {
            LOGGER.error("[FANCYMENU] Frame data size mismatch: expected at least {} bytes, got {}", 
                expectedSize, rgbaPixelData.remaining());
            return;
        }
        
        FrameData frame = new FrameData(rgbaPixelData, textureWidth, textureHeight, delayMs);
        if (isIntro) {
            introFrames.offer(frame);
        } else {
            frames.offer(frame);
        }
    }

    /**
     * Decode PNG data directly to a ByteBuffer using STBImage.
     * This returns a direct ByteBuffer in RGBA format ready for OpenGL upload.
     */
    private ByteBuffer decodePngToDirectBuffer(byte[] pngData) {
        // Allocate a direct ByteBuffer for the PNG data
        ByteBuffer pngBuffer = MemoryUtil.memAlloc(pngData.length);
        pngBuffer.put(pngData);
        pngBuffer.flip();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuf = stack.mallocInt(1);
            IntBuffer heightBuf = stack.mallocInt(1);
            IntBuffer channelsBuf = stack.mallocInt(1);
            
            // Decode to RGBA format (4 channels) - this returns a direct ByteBuffer
            // This is exactly what MCEF does with browser frames
            ByteBuffer pixelData = STBImage.stbi_load_from_memory(
                pngBuffer, 
                widthBuf, 
                heightBuf, 
                channelsBuf, 
                4  // Force RGBA
            );
            
            if (pixelData == null) {
                LOGGER.error("[FANCYMENU] Failed to decode frame: " + STBImage.stbi_failure_reason());
                return null;
            }
            
            // Verify dimensions match
            if (widthBuf.get(0) != textureWidth || heightBuf.get(0) != textureHeight) {
                LOGGER.warn("[FANCYMENU] Frame dimensions mismatch: expected {}x{}, got {}x{}", 
                    textureWidth, textureHeight, widthBuf.get(0), heightBuf.get(0));
            }
            
            return pixelData;
        } finally {
            MemoryUtil.memFree(pngBuffer);
        }
    }

    /**
     * Update the texture with the current frame's data
     */
    public void updateFrame() {
        if (closed.get() || !dimensionsSet.get()) return;

        // Initialize GL resources if needed (will only happen on render thread)
        if (!glInitialized.get() && RenderSystem.isOnRenderThread()) {
            initializeGL();
        }

        if (!glInitialized.get() || glTextureId < 0) return;

        long currentTime = System.currentTimeMillis();
        FrameData current = currentFrame.get();

        // Check if we need to advance to the next frame
        if (current == null || (currentTime - lastFrameTime) >= current.delayMs) {
            advanceFrame();
            lastFrameTime = currentTime;
        }

        // Upload the GPU texture if we have a new frame
        FrameData frameToRender = currentFrame.get();
        if (frameToRender != null && frameToRender.needsUpload) {
            uploadFrameToGPU(frameToRender);
            frameToRender.needsUpload = false;
        }
    }

    /**
     * Advance to the next frame in the animation
     */
    private void advanceFrame() {
        if (!playing.get()) return;

        FrameData nextFrame = null;
        int frameIndex = currentFrameIndex.get();

        // Determine which frame to show next
        if (!introFinished.get() && !introFrames.isEmpty()) {
            // Playing intro frames
            nextFrame = getFrameAt(introFrames, frameIndex);

            if (nextFrame == null) {
                // Intro finished
                introFinished.set(true);
                currentFrameIndex.set(0);
            }
        }

        if (nextFrame == null && !frames.isEmpty()) {
            // Playing normal frames
            nextFrame = getFrameAt(frames, frameIndex);

            if (nextFrame == null) {
                // Loop back to start
                currentFrameIndex.set(0);
                nextFrame = getFrameAt(frames, 0);
            }
        }

        if (nextFrame != null) {
            currentFrame.set(nextFrame);
            currentFrameIndex.incrementAndGet();
        }
    }

    /**
     * Get frame at specific index from a queue
     */
    private FrameData getFrameAt(ConcurrentLinkedQueue<FrameData> frameQueue, int index) {
        int i = 0;
        for (FrameData frame : frameQueue) {
            if (i == index) return frame;
            i++;
        }
        return null;
    }

    /**
     * Upload frame data to the GPU texture using direct OpenGL calls.
     * This is the zero-copy upload path, exactly like MCEF does it.
     */
    private void uploadFrameToGPU(FrameData frame) {
        if (glTextureId < 0 || frame == null || frame.pixelData == null) {
            if (frame != null && frame.pixelData == null) {
                LOGGER.error("[FANCYMENU] Frame has null pixelData - may have been freed already!");
            }
            return;
        }

        // Must be on render thread
        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("[FANCYMENU] Attempted to upload texture from wrong thread!");
            return;
        }

        try {
            // Bind our texture
            GlStateManager._bindTexture(glTextureId);
            
            // Set pixel unpacking parameters
            GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, frame.width);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
            
            // Zero-copy upload directly from the ByteBuffer to OpenGL
            // The ByteBuffer is already in RGBA format from STBImage
            glTexImage2D(
                GL_TEXTURE_2D, 
                0,              // mipmap level
                GL_RGBA,        // internal format
                frame.width,    // width
                frame.height,   // height
                0,              // border (must be 0)
                GL_RGBA,        // format of pixel data
                GL_UNSIGNED_BYTE, // data type
                frame.pixelData   // the actual pixel data - direct ByteBuffer
            );
            
            // Note: For better performance after the first frame, we could use glTexSubImage2D
            // but since we're replacing the entire texture anyway, glTexImage2D is fine
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to upload frame to GPU", e);
        }
    }

    /**
     * Get the ResourceLocation for this animated texture.
     * This always returns the same ResourceLocation, but the underlying texture data changes.
     */
    public ResourceLocation getTextureLocation() {
        // Wait for GL initialization if not ready yet
        if (!glInitialized.get()) {
            // If we're on the render thread and not initialized, try to initialize now
            if (RenderSystem.isOnRenderThread() && dimensionsSet.get()) {
                initializeGL();
            }

            // If still not initialized, return null
            if (!glInitialized.get()) {
                return null;
            }
        }

        // Update the current frame if needed
        updateFrame();

        return textureLocation;
    }

    /**
     * Start playing the animation
     */
    public void play() {
        playing.set(true);
        lastFrameTime = System.currentTimeMillis();

        // If no current frame is set, advance to the first frame immediately
        if (currentFrame.get() == null) {
            advanceFrame();

            // Upload the first frame immediately
            FrameData firstFrame = currentFrame.get();
            if (firstFrame != null && RenderSystem.isOnRenderThread()) {
                uploadFrameToGPU(firstFrame);
                firstFrame.needsUpload = false;
            }
        }
    }

    /**
     * Stop playing the animation
     */
    public void stop() {
        playing.set(false);
        currentFrameIndex.set(0);
        introFinished.set(false);
        currentFrame.set(null);
    }

    /**
     * Reset the animation to the beginning
     */
    public void reset() {
        playing.set(false);
        currentFrameIndex.set(0);
        introFinished.set(false);
        currentFrame.set(null);
        lastFrameTime = 0;

        // Set the first frame as current (intro if available, otherwise normal)
        FrameData firstFrame = null;
        if (!introFrames.isEmpty()) {
            firstFrame = introFrames.peek();
        } else if (!frames.isEmpty()) {
            firstFrame = frames.peek();
        }

        if (firstFrame != null) {
            currentFrame.set(firstFrame);
            firstFrame.needsUpload = true;

            // Upload the first frame immediately if on render thread
            if (RenderSystem.isOnRenderThread() && glTextureId >= 0) {
                uploadFrameToGPU(firstFrame);
                firstFrame.needsUpload = false;
            }
        }
    }

    /**
     * Pause the animation
     */
    public void pause() {
        playing.set(false);
    }

    /**
     * Check if animation is playing
     */
    public boolean isPlaying() {
        return playing.get();
    }

    /**
     * Get texture width
     */
    public int getWidth() {
        return textureWidth;
    }

    /**
     * Get texture height
     */
    public int getHeight() {
        return textureHeight;
    }

    /**
     * Clean up resources
     */
    public void close() {
        if (closed.getAndSet(true)) return;

        // Delete the OpenGL texture (only if on render thread)
        if (glTextureId >= 0) {
            if (RenderSystem.isOnRenderThread()) {
                GlStateManager._deleteTexture(glTextureId);
            } else {
                // Schedule deletion on render thread
                int textureToDelete = glTextureId;
                MainThreadTaskExecutor.executeInMainThread(() -> GlStateManager._deleteTexture(textureToDelete), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
            glTextureId = -1;
        }

        // Clean up frame data - free STBImage-allocated buffers
        for (FrameData frame : frames) {
            frame.cleanup();
        }
        for (FrameData frame : introFrames) {
            frame.cleanup();
        }
        frames.clear();
        introFrames.clear();

        // Unregister from TextureManager (only if initialized)
        if (glInitialized.get()) {
            if (RenderSystem.isOnRenderThread()) {
                Minecraft.getInstance().getTextureManager().release(textureLocation);
            } else {
                MainThreadTaskExecutor.executeInMainThread(() -> Minecraft.getInstance().getTextureManager().release(textureLocation), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
        }
    }

    /**
     * Custom texture wrapper that directly references our OpenGL texture ID
     * This is the same approach as MCEFDirectTexture
     */
    private static class DirectFmaTexture extends AbstractTexture {

        DirectFmaTexture(int glId, int width, int height) {
            // Create and set the texture field in AbstractTexture
            DirectGlTexture glTexture = new DirectGlTexture(glId, width, height);
            this.texture = glTexture;
            this.textureView = new DirectGlTextureView(glTexture, 0, 1);
        }

        @Override
        public void close() {
            // Don't delete the texture - FmaTextureBackend manages it
            this.texture = null;
            this.textureView = null;
        }

        /**
         * Custom GlTexture implementation that wraps an existing OpenGL texture ID
         * without managing its lifecycle.
         */
        private static class DirectGlTexture extends GlTexture {

            protected DirectGlTexture(int textureId, int width, int height) {
                super(
                    GpuTexture.USAGE_TEXTURE_BINDING,  // usage flags
                    "FMA Direct Texture",               // label
                    TextureFormat.RGBA8,                // format
                    width,                              // width
                    height,                             // height
                    1,                                  // depthOrLayers
                    1,                                  // mipLevels
                    textureId                           // glId
                );
                // Mark as not closed initially
                this.closed = false;
            }

            @Override
            public void close() {
                // Don't actually delete the texture - we don't own it
                this.closed = true;
            }
        }

        private static class DirectGlTextureView extends GlTextureView {
            protected DirectGlTextureView(GlTexture texture, int baseMipLevel, int mipLevels) {
                super(texture, baseMipLevel, mipLevels);
            }
        }
    }

    /**
     * Frame data holder - stores direct ByteBuffer ready for OpenGL upload
     */
    private static class FrameData {
        final ByteBuffer pixelData;  // Direct ByteBuffer in RGBA format from STBImage
        final int width;
        final int height;
        final long delayMs;
        boolean needsUpload = true;

        FrameData(ByteBuffer pixelData, int width, int height, long delayMs) {
            this.pixelData = pixelData;
            this.width = width;
            this.height = height;
            this.delayMs = delayMs;
        }
        
        void cleanup() {
            if (pixelData != null) {
                // Free the STBImage-allocated buffer
                STBImage.stbi_image_free(pixelData);
            }
        }
    }
}
