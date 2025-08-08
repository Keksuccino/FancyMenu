package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Adapter class that provides the same API as FmaTexture but uses the optimized
 * single-texture approach internally. This allows existing code to benefit from
 * the optimization with minimal changes.
 */
public class FmaTexture implements ITexture, PlayableResource {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // The optimized texture backend
    private final OptimizedFmaTexture optimizedTexture;
    
    // Compatibility fields from original FmaTexture
    private volatile int width = 10;
    private volatile int height = 10;
    private volatile AspectRatio aspectRatio = new AspectRatio(10, 10);
    private final AtomicBoolean decoded = new AtomicBoolean(false);
    private final AtomicBoolean loadingCompleted = new AtomicBoolean(false);
    private final AtomicBoolean loadingFailed = new AtomicBoolean(false);
    private final AtomicInteger cycles = new AtomicInteger(0);
    private final AtomicInteger numPlays = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    // Source tracking
    private ResourceLocation sourceLocation;
    private File sourceFile;
    private String sourceURL;
    
    // Frame buffering for async loading
    private final List<PendingFrame> pendingFrames = new ArrayList<>();
    private final List<PendingFrame> pendingIntroFrames = new ArrayList<>();
    private volatile boolean framesProcessed = false;
    
    public FmaTexture() {
        this.optimizedTexture = new OptimizedFmaTexture();
    }
    
    /**
     * Create from ResourceLocation (compatible with FmaTexture.location())
     */
    @NotNull
    public static FmaTexture location(@NotNull ResourceLocation location) {
        FmaTexture adapter = new FmaTexture();
        adapter.sourceLocation = location;
        
        // Load asynchronously
        new Thread(() -> {
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(location)) {
                adapter.loadFromStream(in, location.toString());
            } catch (Exception ex) {
                adapter.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from ResourceLocation: " + location, ex);
            }
        }).start();
        
        return adapter;
    }
    
    /**
     * Create from File (compatible with FmaTexture.local())
     */
    @NotNull
    public static FmaTexture local(@NotNull File fmaFile) {
        FmaTexture adapter = new FmaTexture();
        adapter.sourceFile = fmaFile;
        
        if (!fmaFile.isFile()) {
            adapter.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from file! File not found: " + fmaFile.getPath());
            return adapter;
        }
        
        // Load asynchronously
        new Thread(() -> {
            try (InputStream in = new FileInputStream(fmaFile)) {
                adapter.loadFromStream(in, fmaFile.getPath());
            } catch (Exception ex) {
                adapter.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from file: " + fmaFile.getPath(), ex);
            }
        }).start();
        
        return adapter;
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl) {
        return web(fmaUrl, null);
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl, @Nullable FmaTexture writeTo) {
        return new FmaTexture();
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in, @Nullable String gifTextureName, @Nullable FmaTexture writeTo) {
        return new FmaTexture();
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }
    
    /**
     * Load FMA data from stream
     */
    private void loadFromStream(InputStream in, String sourceName) {
        try {
            // Decode the FMA using existing decoder
            FmaTexture.DecodedFmaImage decodedImage = FmaTexture.decodeFma(in, sourceName);
            if (decodedImage == null) {
                loadingFailed.set(true);
                return;
            }
            
            // Set dimensions
            this.width = decodedImage.imageWidth();
            this.height = decodedImage.imageHeight();
            this.aspectRatio = new AspectRatio(width, height);
            this.numPlays.set(decodedImage.numPlays());
            
            // Initialize the optimized texture with the dimensions
            optimizedTexture.initialize(width, height);
            
            // Load intro frames if present
            if (decodedImage.decoder().hasIntroFrames()) {
                FmaTexture.deliverFmaIntroFrames(decodedImage.decoder(), sourceName, frame -> {
                    if (frame != null) {
                        try {
                            // Convert to byte array for efficient storage
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = frame.frameInputStream.read(buffer)) != -1) {
                                baos.write(buffer, 0, bytesRead);
                            }
                            pendingIntroFrames.add(new PendingFrame(baos.toByteArray(), frame.delayMs));
                        } catch (Exception ex) {
                            LOGGER.error("[FANCYMENU] Failed to process intro frame", ex);
                        } finally {
                            try {
                                frame.frameInputStream.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                });
            }
            
            // Load normal frames
            FmaTexture.deliverFmaFrames(decodedImage.decoder(), sourceName, frame -> {
                if (frame != null) {
                    try {
                        // Convert to byte array for efficient storage
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = frame.frameInputStream.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        pendingFrames.add(new PendingFrame(baos.toByteArray(), frame.delayMs));
                    } catch (Exception ex) {
                        LOGGER.error("[FANCYMENU] Failed to process frame", ex);
                    } finally {
                        try {
                            frame.frameInputStream.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            });
            
            // Process frames into the optimized texture
            processFrames();
            
            decoded.set(true);
            loadingCompleted.set(true);
            
            // Clean up decoder
            decodedImage.decoder().close();
            
        } catch (Exception ex) {
            loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to load optimized FMA image: " + sourceName, ex);
        }
    }
    
    /**
     * Process pending frames into the optimized texture
     */
    private void processFrames() {
        if (framesProcessed) return;
        framesProcessed = true;
        
        // Add intro frames
        for (PendingFrame frame : pendingIntroFrames) {
            optimizedTexture.addFrame(frame.imageData, frame.delayMs, true);
        }
        
        // Add normal frames
        for (PendingFrame frame : pendingFrames) {
            optimizedTexture.addFrame(frame.imageData, frame.delayMs, false);
        }
        
        // Clear pending frames to free memory
        pendingIntroFrames.clear();
        pendingFrames.clear();
    }
    
    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (closed.get() || !decoded.get()) return null;
        
        // Start playing if not already
        if (!optimizedTexture.isPlaying()) {
            optimizedTexture.play();
        }
        
        return optimizedTexture.getTextureLocation();
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return aspectRatio;
    }
    
    @Override
    public @Nullable InputStream open() throws IOException {
        if (sourceFile != null) return new FileInputStream(sourceFile);
        if (sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(sourceLocation);
        return null;
    }
    
    @Override
    public boolean isReady() {
        return decoded.get();
    }
    
    @Override
    public boolean isLoadingCompleted() {
        return !closed.get() && !loadingFailed.get() && loadingCompleted.get();
    }
    
    @Override
    public boolean isLoadingFailed() {
        return loadingFailed.get();
    }
    
    @Override
    public void play() {
        optimizedTexture.play();
    }
    
    @Override
    public boolean isPlaying() {
        return optimizedTexture.isPlaying();
    }
    
    @Override
    public void pause() {
        optimizedTexture.pause();
    }
    
    @Override
    public boolean isPaused() {
        return !optimizedTexture.isPlaying();
    }
    
    @Override
    public void stop() {
        optimizedTexture.stop();
        cycles.set(0);
    }
    
    /**
     * Reset the animation to the beginning.
     * Compatible with FmaTexture.reset()
     */
    public void reset() {
        // Reset the optimized texture
        optimizedTexture.reset();
        cycles.set(0);
        
        // The animation stays in the "ready to play" state
        // matching the behavior of the original FmaTexture
    }
    
    @Override
    public boolean isClosed() {
        return closed.get();
    }
    
    @Override
    public void close() {
        if (closed.getAndSet(true)) return;
        optimizedTexture.close();
        sourceLocation = null;
        sourceFile = null;
        sourceURL = null;
    }

    @Nullable
    public static FmaTexture.DecodedFmaImage decodeFma(@NotNull InputStream in, @NotNull String fmaName) {
        try {
            FmaDecoder decoder = new FmaDecoder();
            decoder.read(in);
            BufferedImage firstFrame = Objects.requireNonNull(decoder.getFirstFrameAsBufferedImage(), "Failed to get first frame of FMA image!");
            return new FmaTexture.DecodedFmaImage(decoder, firstFrame.getWidth(), firstFrame.getHeight(), Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!").getLoopCount()); //loopCount == 0 == infinite loops | loopCount > 0 == number of loops
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to decode FMA image: " + fmaName, ex);
        }
        return null;
    }

    public static void deliverFmaFrames(@NotNull FmaDecoder decoder, @NotNull String fmaName, @NotNull Consumer<FmaTexture.FmaFrame> frameDelivery) {
        int gifFrameCount = decoder.getFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                long delay = Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!").getFrameTimeForFrame(i, false);
                InputStream image = Objects.requireNonNull(decoder.getFrame(i), "FmaDecoder returned NULL for frame!");
                frameDelivery.accept(new FmaTexture.FmaFrame(index, image, delay));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get frame '" + i + "' of FMA image '" + fmaName + "!", ex);
            }
            i++;
        }
    }

    public static void deliverFmaIntroFrames(@NotNull FmaDecoder decoder, @NotNull String fmaName, @NotNull Consumer<FmaTexture.FmaFrame> frameDelivery) {
        if (!decoder.hasIntroFrames()) return;
        int gifFrameCount = decoder.getIntroFrameCount();
        int i = 0;
        int index = 0;
        while (i < gifFrameCount) {
            try {
                long delay = Objects.requireNonNull(decoder.getMetadata(), "FmaDecoder returned NULL for metadata!").getFrameTimeForFrame(i, true);
                InputStream image = Objects.requireNonNull(decoder.getIntroFrame(i), "FmaDecoder returned NULL for intro frame!");
                frameDelivery.accept(new FmaTexture.FmaFrame(index, image, delay));
                index++;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get intro frame '" + i + "' of FMA image '" + fmaName + "!", ex);
            }
            i++;
        }
    }
    
    /**
     * Helper class to store frame data before processing
     */
    private static class PendingFrame {
        final byte[] imageData;
        final long delayMs;
        
        PendingFrame(byte[] imageData, long delayMs) {
            this.imageData = imageData;
            this.delayMs = delayMs;
        }
    }

    public static class FmaFrame {

        protected final int index;
        protected final InputStream frameInputStream;
        protected final long delayMs;
        protected DynamicTexture dynamicTexture;
        protected volatile NativeImage nativeImage;
        protected ResourceLocation resourceLocation;
        protected boolean loaded = false;

        protected FmaFrame(int index, InputStream frameInputStream, long delayMs) {
            this.index = index;
            this.frameInputStream = frameInputStream;
            this.delayMs = delayMs;
        }

    }

    public record DecodedFmaImage(@NotNull FmaDecoder decoder, int imageWidth, int imageHeight, int numPlays) {
    }

}