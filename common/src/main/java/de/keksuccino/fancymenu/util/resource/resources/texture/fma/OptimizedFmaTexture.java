package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Optimized FMA texture that uses a single OpenGL texture and updates it in-place
 * for each frame using direct OpenGL calls, exactly like MCEF handles browser textures.
 * This provides zero-copy updates and minimal overhead.
 */
public class OptimizedFmaTexture extends AbstractTexture {
    private static final Logger LOGGER = LogManager.getLogger();

    // The OpenGL texture ID that we update directly
    private int glTextureId = -1;
    private int textureWidth = 0;
    private int textureHeight = 0;

    // The single ResourceLocation that always points to our texture
    private final ResourceLocation textureLocation;

    // Frame data storage - we only decode frames as needed
    private final ConcurrentLinkedQueue<FrameData> frames = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<FrameData> introFrames = new ConcurrentLinkedQueue<>();

    // Current frame being displayed
    private final AtomicReference<FrameData> currentFrame = new AtomicReference<>();
    private final AtomicReference<NativeImage> currentNativeImage = new AtomicReference<>();

    // Animation state
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private final AtomicBoolean introFinished = new AtomicBoolean(false);
    private volatile long lastFrameTime = 0;
    private final AtomicInteger currentFrameIndex = new AtomicInteger(0);

    // Loading state
    private final AtomicBoolean dimensionsSet = new AtomicBoolean(false);
    private final AtomicBoolean glInitialized = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public OptimizedFmaTexture() {
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
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        // Allocate texture memory with empty data
        GlStateManager._texImage2D(
            GL11.GL_TEXTURE_2D,
            0,  // mipmap level
            GL11.GL_RGBA,  // internal format - standard RGBA
            textureWidth,
            textureHeight,
            0,  // border
            GL11.GL_RGBA,  // format for initial data
            GL11.GL_UNSIGNED_BYTE,
            null  // no initial data
        );

        LOGGER.debug("[FANCYMENU] Created GL texture with ID {} for {}x{} FMA animation", glTextureId, textureWidth, textureHeight);

        // Create a custom texture wrapper (like MCEFDirectTexture)
        DirectFmaTexture directTexture = new DirectFmaTexture(glTextureId, textureWidth, textureHeight);

        // Register with TextureManager
        Minecraft.getInstance().getTextureManager().register(textureLocation, directTexture);
    }

    /**
     * Add a frame to the animation
     */
    public void addFrame(byte[] imageData, long delayMs, boolean isIntro) {
        FrameData frame = new FrameData(imageData, delayMs);
        if (isIntro) {
            introFrames.offer(frame);
        } else {
            frames.offer(frame);
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

        // Update the GPU texture if we have a new frame
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
     * Upload frame data to the GPU texture using direct OpenGL calls
     */
    private void uploadFrameToGPU(FrameData frame) {
        if (glTextureId < 0 || frame.imageData == null) return;

        // Must be on render thread
        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("[FANCYMENU] Attempted to upload texture from wrong thread!");
            return;
        }

        try {
            // Decode the frame data into a NativeImage if needed
            NativeImage oldImage = currentNativeImage.get();
            NativeImage newImage = frame.getNativeImage();

            if (newImage != null) {
                // Bind our texture
                GlStateManager._bindTexture(glTextureId);

                // Set pixel unpacking parameters
                GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
                GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);

                // Debug: Let's thoroughly check the pixel data
                if (newImage.getWidth() > 0 && newImage.getHeight() > 0) {
                    // Sample multiple pixels to understand the format
                    int pixel = newImage.getPixel(0, 0); // Returns ARGB
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    // Also check the raw memory
                    int abgr = ObjectUtils.build(() -> {
                        try {
                            Method m = NativeImage.class.getDeclaredMethod("getPixelABGR", int.class, int.class);
                            m.setAccessible(true);
                            return (int) m.invoke(newImage, 0, 0);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return -1;
                    });

                    // Get the actual bytes from memory
                    long ptr = newImage.getPointer();
                    byte b0 = MemoryUtil.memGetByte(ptr + 0);
                    byte b1 = MemoryUtil.memGetByte(ptr + 1);
                    byte b2 = MemoryUtil.memGetByte(ptr + 2);
                    byte b3 = MemoryUtil.memGetByte(ptr + 3);

                    LOGGER.info("[FANCYMENU] Pixel analysis:");
                    LOGGER.info("  - getPixel() ARGB: A={}, R={}, G={}, B={}", a, r, g, b);
                    LOGGER.info("  - getPixelABGR() raw: 0x{}", Integer.toHexString(abgr));
                    LOGGER.info("  - Memory bytes [0-3]: [{}, {}, {}, {}]",
                        Byte.toUnsignedInt(b0), Byte.toUnsignedInt(b1),
                        Byte.toUnsignedInt(b2), Byte.toUnsignedInt(b3));
                    LOGGER.info("  - Expected: If R={}, G={}, B={}, then bytes should be [{}, {}, {}, {}]",
                        r, g, b, r, g, b, a);
                }

                // ABSOLUTE DIRECT UPLOAD - No modifications at all!
                // NativeImage memory layout on little-endian (x86/x64):
                // ABGR integer 0xAABBGGRR is stored as bytes [RR, GG, BB, AA]
                // So byte order in memory is R,G,B,A

                // The simplest possible upload - GL_RGBA with GL_UNSIGNED_BYTE
                GlStateManager._texSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,  // mipmap level
                    0,  // x offset
                    0,  // y offset
                    newImage.getWidth(),
                    newImage.getHeight(),
                    GL11.GL_RGBA,  // OpenGL expects R,G,B,A byte order
                    GL11.GL_UNSIGNED_BYTE,  // Read as individual bytes
                    newImage.getPointer()  // Direct pointer to NativeImage data
                );

                LOGGER.info("[FANCYMENU] Using direct GL_RGBA upload for frame");

                // If this still has wrong colors, the issue is somewhere else in the pipeline

                LOGGER.info("[FANCYMENU] Direct upload to texture {} ({}x{})", glTextureId, newImage.getWidth(), newImage.getHeight());

                currentNativeImage.set(newImage);

                // Clean up old image if different
                if (oldImage != null && oldImage != newImage) {
                    oldImage.close();
                }
            } else {
                LOGGER.warn("[FANCYMENU] Frame getNativeImage() returned null");
            }
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

        // Clean up current native image
        NativeImage img = currentNativeImage.get();
        if (img != null) {
            img.close();
        }

        // Clean up frame data
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
            // IMPORTANT: Create and set the texture field in AbstractTexture
            // This is what GuiGraphics.innerBlit() looks for
            DirectGlTexture glTexture = new DirectGlTexture(glId, width, height);
            this.texture = glTexture;
            // Fix: Use (0, 1) for baseMipLevel=0, mipLevels=1
            this.textureView = new DirectGlTextureView(glTexture, 0, 1);
        }

        @Override
        public void close() {
            // Don't delete the texture - OptimizedFmaTexture manages it
            this.texture = null;
            this.textureView = null;
        }

        /**
         * Custom GlTexture implementation that wraps an existing OpenGL texture ID
         * without managing its lifecycle.
         */
        private static class DirectGlTexture extends GlTexture {

            protected DirectGlTexture(int textureId, int width, int height) {
                // GlTexture constructor parameters in 1.21.7:
                // int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels, int glId
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

            protected DirectGlTextureView(GlTexture $$0, int $$1, int $$2) {
                super($$0, $$1, $$2);
            }

        }

    }

    /**
     * Frame data holder - stores compressed image data until needed
     */
    private static class FrameData {
        final byte[] imageData;
        final long delayMs;
        boolean needsUpload = true;
        private NativeImage cachedNativeImage;

        FrameData(byte[] imageData, long delayMs) {
            this.imageData = imageData;
            this.delayMs = delayMs;
        }

        NativeImage getNativeImage() {
            if (cachedNativeImage == null && imageData != null) {
                try {
                    // Read the PNG data into a NativeImage
                    // This uses STBImage internally which should give us proper RGBA data
                    cachedNativeImage = NativeImage.read(imageData);

                    if (cachedNativeImage != null && cachedNativeImage.getWidth() > 0 && cachedNativeImage.getHeight() > 0) {
                        // Debug: Check pixel format
                        int pixel = cachedNativeImage.getPixel(0, 0); // Returns ARGB
                        int a = (pixel >> 24) & 0xFF;
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        LOGGER.debug("[FANCYMENU] Frame decoded: {}x{}, sample pixel ARGB: A={}, R={}, G={}, B={}",
                            cachedNativeImage.getWidth(), cachedNativeImage.getHeight(), a, r, g, b);
                    }
                } catch (Exception e) {
                    LOGGER.error("[FANCYMENU] Failed to decode frame data", e);
                }
            }
            return cachedNativeImage;
        }
        
        void cleanup() {
            if (cachedNativeImage != null) {
                cachedNativeImage.close();
                cachedNativeImage = null;
            }
        }
    }
}
