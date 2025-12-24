package de.keksuccino.fancymenu.customization.panorama;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class PanoramaCubeMapTexture extends AbstractTexture {
    
    private static final Logger LOGGER = LogManager.getLogger();
    // Vanilla uses this specific order for cube map faces
    private static final int[] FACE_ORDER = {1, 3, 5, 4, 0, 2};
    
    @NotNull
    private final List<ResourceSupplier<ITexture>> textureSuppliers;
    @NotNull
    private final String name;
    private volatile boolean loaded = false;
    private volatile boolean loadFailed = false;
    
    public PanoramaCubeMapTexture(@NotNull String name, @NotNull List<ResourceSupplier<ITexture>> textureSuppliers) {
        this.name = Objects.requireNonNull(name);
        this.textureSuppliers = Objects.requireNonNull(textureSuppliers);
        
        if (textureSuppliers.size() != 6) {
            throw new IllegalArgumentException("Cube map must have exactly 6 texture suppliers, got " + textureSuppliers.size());
        }
    }
    
    public void loadTextures() {
        if (this.loaded || this.loadFailed) {
            return;
        }
        
        try {
            // Wait for all textures to be ready
            List<ITexture> textures = new java.util.ArrayList<>(6);
            for (int i = 0; i < 6; i++) {
                ITexture texture = this.textureSuppliers.get(i).get();
                if (texture == null) {
                    throw new IOException("Texture supplier " + i + " returned null");
                }
                
                // Wait for texture to be ready
                texture.waitForReady(5000);
                if (!texture.isReady()) {
                    throw new IOException("Texture " + i + " failed to become ready in time");
                }
                
                textures.add(texture);
            }
            
            // Get dimensions from first texture
            int width = textures.get(0).getWidth();
            int height = textures.get(0).getHeight();
            
            // Verify all textures have same dimensions
            for (int i = 1; i < 6; i++) {
                if (textures.get(i).getWidth() != width || textures.get(i).getHeight() != height) {
                    throw new IOException("Image dimensions of cubemap '" + this.name + "' sides do not match: part 0 is " + 
                        width + "x" + height + ", but part " + i + " is " + 
                        textures.get(i).getWidth() + "x" + textures.get(i).getHeight());
                }
            }
            
            // Create combined image following vanilla's format
            NativeImage combinedImage = new NativeImage(width, height * 6, false);
            
            try {
                // Copy each face into the combined image in the correct order
                for (int i = 0; i < 6; i++) {
                    int faceIndex = FACE_ORDER[i];
                    ITexture texture = textures.get(faceIndex);
                    
                    // Get NativeImage from texture
                    NativeImage faceImage = null;
                    if (texture instanceof PngTexture pngTexture) {
                        faceImage = pngTexture.getNativeImage();
                    }
                    
                    if (faceImage == null) {
                        throw new IOException("Could not get NativeImage from texture " + faceIndex);
                    }
                    
                    // Copy face image to combined image (with Y flip like vanilla does)
                    faceImage.copyRect(combinedImage, 0, 0, 0, i * height, width, height, false, true);
                }
                
                // Load the combined image into GPU
                this.doLoad(combinedImage, false, true);
                
            } finally {
                combinedImage.close();
            }
            
            this.loaded = true;
            
        } catch (Exception ex) {
            this.loadFailed = true;
            LOGGER.error("[FANCYMENU] Failed to load custom cube map texture: " + this.name, ex);
        }
    }
    
    protected void doLoad(NativeImage image, boolean blur, boolean clamp) {
        GpuDevice device = RenderSystem.getDevice();
        int width = image.getWidth();
        int height = image.getHeight() / 6;
        
        this.texture = device.createTexture(
            () -> "CustomCubeMap_" + this.name,
            21, // Cube map target type
            TextureFormat.RGBA8,
            width,
            height,
            6, // 6 layers for cube map
            1  // 1 mip level
        );
        
        this.textureView = device.createTextureView(this.texture);
        AddressMode addressMode = clamp ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
        FilterMode filterMode = blur ? FilterMode.LINEAR : FilterMode.NEAREST;
        this.sampler = RenderSystem.getSamplerCache().getSampler(addressMode, addressMode, filterMode, filterMode, false);
        
        // Write each face to its layer
        for (int i = 0; i < 6; i++) {
            device.createCommandEncoder().writeToTexture(
                this.texture, 
                image,
                0, i, 0, 0,  // mip level, layer, x, y
                width, height,
                0, height * i // source x, source y
            );
        }
    }
    
    @Override
    @NotNull
    public GpuTextureView getTextureView() {
        if (!this.loaded && !this.loadFailed) {
            this.loadTextures();
        }
        return super.getTextureView();
    }
    
    @Override
    @NotNull
    public GpuTexture getTexture() {
        if (!this.loaded && !this.loadFailed) {
            this.loadTextures();
        }
        return super.getTexture();
    }
    
    public boolean isLoaded() {
        return this.loaded;
    }
    
    public boolean isLoadFailed() {
        return this.loadFailed;
    }

}
