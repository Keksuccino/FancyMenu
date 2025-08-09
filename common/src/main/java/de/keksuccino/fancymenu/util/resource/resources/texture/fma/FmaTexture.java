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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Streaming FMA texture that loads frames on-demand during playback
 */
public class FmaTexture implements ITexture, PlayableResource {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // The streaming texture backend
    private final FmaTextureBackend backend;
    
    // The decoder that streams frames from the FMA file
    private FmaDecoder decoder;
    
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
    
    // Loading future for async operations
    private CompletableFuture<Void> loadingFuture;
    
    public FmaTexture() {
        this.backend = new FmaTextureBackend();
    }
    
    /**
     * Create from ResourceLocation (compatible with FmaTexture.location())
     */
    @NotNull
    public static FmaTexture location(@NotNull ResourceLocation location) {
        FmaTexture texture = new FmaTexture();
        texture.sourceLocation = location;
        
        // Load asynchronously
        texture.loadingFuture = CompletableFuture.runAsync(() -> {
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(location)) {
                texture.loadFromStream(in, location.toString());
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read streaming FMA image from ResourceLocation: " + location, ex);
            }
        }, runnable -> new Thread(runnable, "FMA-StreamLoader-" + location).start());
        
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
            LOGGER.error("[FANCYMENU] Failed to read streaming FMA image from file! File not found: " + fmaFile.getPath());
            return texture;
        }
        
        // Load asynchronously
        texture.loadingFuture = CompletableFuture.runAsync(() -> {
            try {
                texture.loadFromFile(fmaFile);
            } catch (Exception ex) {
                texture.loadingFailed.set(true);
                LOGGER.error("[FANCYMENU] Failed to read streaming FMA image from file: " + fmaFile.getPath(), ex);
            }
        }, runnable -> new Thread(runnable, "FMA-StreamLoader-" + fmaFile.getName()).start());
        
        return texture;
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl) {
        return web(fmaUrl, null);
    }

    @NotNull
    public static FmaTexture web(@NotNull String fmaUrl, @Nullable FmaTexture writeTo) {
        // Web loading not implemented yet in streaming version
        FmaTexture texture = writeTo != null ? writeTo : new FmaTexture();
        texture.sourceURL = fmaUrl;
        texture.loadingFailed.set(true);
        LOGGER.warn("[FANCYMENU] Web loading not yet implemented for streaming FMA textures");
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
            LOGGER.error("[FANCYMENU] Failed to read streaming FMA image from stream", ex);
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
     * Load FMA data from file using the streaming decoder
     */
    private void loadFromFile(File file) throws IOException {
        // Create decoder but keep it open for streaming
        decoder = new FmaDecoder();
        decoder.openFile(file);
        processStreamingDecoder(file.getPath());
    }
    
    /**
     * Load FMA data from stream using the streaming decoder
     */
    private void loadFromStream(InputStream in, String sourceName) throws IOException {
        // Create decoder but keep it open for streaming
        decoder = new FmaDecoder();
        decoder.openStream(in);
        processStreamingDecoder(sourceName);
    }
    
    /**
     * Process the streaming decoder and set up the texture backend
     */
    private void processStreamingDecoder(String sourceName) {
        try {
            // Wait for decoder to be ready
            if (!decoder.isReady()) {
                LOGGER.error("[FANCYMENU] Decoder not ready for: {}", sourceName);
                LOGGER.error("[FANCYMENU] Frame count: {}, Intro count: {}, Dimensions: {}x{}", 
                    decoder.getFrameCount(), decoder.getIntroFrameCount(), 
                    decoder.getFrameWidth(), decoder.getFrameHeight());
                throw new IOException("Decoder failed to initialize");
            }
            
            // Get metadata
            FmaDecoder.FmaMetadata metadata = decoder.getMetadata();
            if (metadata == null) {
                throw new IOException("No metadata found in FMA file");
            }
            
            // Set dimensions
            this.width = decoder.getFrameWidth();
            this.height = decoder.getFrameHeight();
            this.aspectRatio = new AspectRatio(width, height);
            this.numPlays.set(metadata.getLoopCount());
            
            // Initialize the streaming backend with the decoder
            backend.initializeWithDecoder(decoder);
            
            decoded.set(true);
            loadingCompleted.set(true);
            
            LOGGER.debug("[FANCYMENU] Streaming FMA texture initialized: {} ({}x{}, {} frames + {} intro)", 
                sourceName, width, height, decoder.getFrameCount(), decoder.getIntroFrameCount());
            
        } catch (Exception ex) {
            loadingFailed.set(true);
            LOGGER.error("[FANCYMENU] Failed to process streaming FMA image: " + sourceName, ex);
            
            // Clean up decoder on failure
            if (decoder != null) {
                try {
                    decoder.close();
                } catch (IOException e) {
                    // Ignore
                }
                decoder = null;
            }
        }
    }
    
    @Nullable
    @Override
    public ResourceLocation getResourceLocation() {
        if (closed.get() || !decoded.get()) return null;
        
        // Get the texture location from the backend
        ResourceLocation location = backend.getTextureLocation();
        
        // If the texture isn't ready yet, return null
        if (location == null) {
            // Start playing so it initializes on next frame
            if (!backend.isPlaying()) {
                backend.play();
            }
            return null;
        }
        
        // Start playing if not already
        if (!backend.isPlaying()) {
            backend.play();
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
        return decoded.get() && backend.isReady();
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
        if (decoded.get()) {
            backend.play();
        }
    }
    
    @Override
    public boolean isPlaying() {
        return backend.isPlaying();
    }
    
    @Override
    public void pause() {
        backend.pause();
    }
    
    @Override
    public boolean isPaused() {
        return !backend.isPlaying();
    }
    
    @Override
    public void stop() {
        backend.stop();
        cycles.set(0);
    }
    
    /**
     * Reset the animation to the beginning.
     * Compatible with FmaTexture.reset()
     */
    public void reset() {
        // Reset the streaming texture
        backend.reset();
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
        
        // Cancel loading if still in progress
        if (loadingFuture != null && !loadingFuture.isDone()) {
            loadingFuture.cancel(true);
        }
        
        // Close backend
        backend.close();
        
        // Close decoder if we still have one
        // (backend.close() should already close it, but just in case)
        if (decoder != null) {
            try {
                decoder.close();
            } catch (IOException e) {
                // Ignore
            }
            decoder = null;
        }
        
        sourceLocation = null;
        sourceFile = null;
        sourceURL = null;
    }
}
