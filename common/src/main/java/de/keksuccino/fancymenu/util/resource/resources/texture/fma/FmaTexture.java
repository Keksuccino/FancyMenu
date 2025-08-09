package de.keksuccino.fancymenu.util.resource.resources.texture.fma;

import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized FMA texture that uses zero-copy OpenGL uploads like MCEF.
 * This class provides the same API as the original FmaTexture but uses
 * FmaDecoderOptimized and FmaTextureBackend for maximum performance.
 */
public class FmaTexture implements ITexture, PlayableResource {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // The optimized texture backend
    private final FmaTextureBackend optimizedTexture;
    
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
    
    public FmaTexture() {
        this.optimizedTexture = new FmaTextureBackend();
    }
    
    /**
     * Create from ResourceLocation (compatible with FmaTexture.location())
     */
    @NotNull
    public static FmaTexture location(@NotNull ResourceLocation location) {
        FmaTexture texture = new FmaTexture();
        texture.sourceLocation = location;
        
        // Load asynchronously
        new Thread(() -> {
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(location)) {
                texture.loadFromStream(in, location.toString());
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from ResourceLocation: " + location, ex);
            }
        }, "FMA-Loader-" + location).start();
        
        return texture;
    }
    
    /**
     * Create from File (compatible with FmaTexture.local())
     */
    @NotNull
    public static FmaTexture local(@NotNull File fmaFile) {
        FmaTexture texture = new FmaTexture();
        texture.sourceFile = fmaFile;
        
        if (!fmaFile.isFile()) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from file! File not found: " + fmaFile.getPath());
            return texture;
        }
        
        // Load asynchronously
        new Thread(() -> {
            try {
                texture.loadFromFile(fmaFile);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from file: " + fmaFile.getPath(), ex);
            }
        }, "FMA-Loader-" + fmaFile.getName()).start();
        
        return texture;
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl) {
        return web(fmaUrl, null);
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl, @Nullable FmaTexture writeTo) {
        // Web loading not implemented yet in optimized version
        FmaTexture texture = writeTo != null ? writeTo : new FmaTexture();
        texture.sourceURL = fmaUrl;
        texture.loadingFailed.set(true);
        LOGGER.warn("[FANCYMENU] Web loading not yet implemented for optimized FMA textures");
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in, @Nullable String gifTextureName, @Nullable FmaTexture writeTo) {
        FmaTexture texture = writeTo != null ? writeTo : new FmaTexture();
        
        // Load synchronously since we already have the stream
        try {
            texture.loadFromStream(in, gifTextureName != null ? gifTextureName : "stream");
        } catch (Exception ex) {
            texture.loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to read optimized FMA image from stream", ex);
        }
        
        return texture;
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static FmaTexture of(@NotNull InputStream in) {
        return of(in, null, null);
    }
    
    /**
     * Load FMA data from file using the optimized decoder
     */
    private void loadFromFile(File file) throws IOException {
        try (FmaDecoderOptimized decoder = new FmaDecoderOptimized()) {
            decoder.read(file);
            processDecodedFma(decoder, file.getPath());
        }
    }
    
    /**
     * Load FMA data from stream using the optimized decoder
     */
    private void loadFromStream(InputStream in, String sourceName) throws IOException {
        try (FmaDecoderOptimized decoder = new FmaDecoderOptimized()) {
            decoder.read(in);
            processDecodedFma(decoder, sourceName);
        }
    }
    
    /**
     * Process the decoded FMA data and set up the texture backend
     */
    private void processDecodedFma(FmaDecoderOptimized decoder, String sourceName) {
        try {
            // Get metadata
            FmaDecoderOptimized.FmaMetadata metadata = decoder.getMetadata();
            if (metadata == null) {
                throw new IOException("No metadata found in FMA file");
            }
            
            // Set dimensions
            this.width = decoder.getFrameWidth();
            this.height = decoder.getFrameHeight();
            this.aspectRatio = new AspectRatio(width, height);
            this.numPlays.set(metadata.getLoopCount());
            
            // Initialize the optimized texture with the dimensions
            optimizedTexture.initialize(width, height);
            
            // Add intro frames if present - TAKE ownership of ByteBuffers
            if (decoder.hasIntroFrames()) {
                for (int i = 0; i < decoder.getIntroFrameCount(); i++) {
                    // Use takeIntroFramePixelData to transfer ownership
                    ByteBuffer frameData = decoder.takeIntroFramePixelData(i);
                    if (frameData != null) {
                        long delayMs = metadata.getFrameTimeForFrame(i, true);
                        // FmaTextureBackend now owns this ByteBuffer
                        optimizedTexture.addFrameDirect(frameData, delayMs, true);
                    }
                }
            }
            
            // Add normal frames - TAKE ownership of ByteBuffers
            for (int i = 0; i < decoder.getFrameCount(); i++) {
                // Use takeFramePixelData to transfer ownership
                ByteBuffer frameData = decoder.takeFramePixelData(i);
                if (frameData != null) {
                    long delayMs = metadata.getFrameTimeForFrame(i, false);
                    // FmaTextureBackend now owns this ByteBuffer
                    optimizedTexture.addFrameDirect(frameData, delayMs, false);
                }
            }
            
            decoded.set(true);
            loadingCompleted.set(true);
            
        } catch (Exception ex) {
            loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to process optimized FMA image: " + sourceName, ex);
        }
    }
    
    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (closed.get() || !decoded.get()) return null;
        
        // Get the texture location from the backend
        ResourceLocation location = optimizedTexture.getTextureLocation();
        
        // If the texture isn't ready yet, return null
        if (location == null) {
            // Start playing so it initializes on next frame
            if (!optimizedTexture.isPlaying()) {
                optimizedTexture.play();
            }
            return null;
        }
        
        // Start playing if not already
        if (!optimizedTexture.isPlaying()) {
            optimizedTexture.play();
        }
        
        return location;
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
}
