package de.keksuccino.fancymenu.customization.panorama;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom texture implementation that takes ResourceSuppliers, retrieves their
 * loaded NativeImage data, and uploads them to the GPU as a single, seamless cubemap texture.
 */
public class PanoramaCubeMapTexture extends AbstractTexture {

    private static final Logger LOGGER = LogManager.getLogger();
    private final List<ResourceSupplier<ITexture>> suppliers;

    public PanoramaCubeMapTexture(List<ResourceSupplier<ITexture>> suppliers) {
        if (suppliers.size() != 6) {
            throw new IllegalArgumentException("Must provide exactly 6 ResourceSuppliers for a cubemap texture.");
        }
        this.suppliers = suppliers;
    }

    /**
     * Loads the 6 images by getting them from the suppliers, validates them, and uploads them to the GPU.
     * This version is simplified thanks to PngTexture::getNativeImage.
     *
     * @throws IOException If image data cannot be retrieved or dimensions are mismatched.
     */
    public void load() throws IOException {

        RenderSystem.assertOnRenderThread();
        this.close();

        List<NativeImage> imagesToUpload = new ArrayList<>();
        try {
            for (ResourceSupplier<ITexture> supplier : this.suppliers) {
                ITexture iTexture = supplier.get();
                if (iTexture == null) {
                    throw new IOException("ResourceSupplier returned a null ITexture for source: " + supplier.getSourceWithPrefix());
                }

                // Wait for the asynchronous loading to complete or fail.
                iTexture.waitForLoadingCompletedOrFailed(5000L); // 5 second timeout
                if (iTexture.isLoadingFailed()) {
                    throw new IOException("A texture resource failed to load: " + supplier.getSourceWithPrefix());
                }
                if (!iTexture.isLoadingCompleted()) {
                    throw new IOException("A texture resource timed out while loading: " + supplier.getSourceWithPrefix());
                }

                if (!(iTexture instanceof PngTexture pngTexture)) {
                    throw new IOException("Supplied texture is not a PngTexture instance for source: " + supplier.getSourceWithPrefix());
                }

                // ### THE SIMPLIFICATION ###
                // Directly get the NativeImage instead of re-reading from an InputStream.
                NativeImage nativeImage = pngTexture.getNativeImage();
                if (nativeImage == null) {
                    throw new IOException("PngTexture provided a null NativeImage for source: " + supplier.getSourceWithPrefix());
                }
                imagesToUpload.add(nativeImage);
            }

            if (imagesToUpload.size() < 6) {
                throw new IOException("Failed to retrieve all 6 cubemap images from suppliers.");
            }

            // Validate dimensions.
            int width = imagesToUpload.get(0).getWidth();
            int height = imagesToUpload.get(0).getHeight();
            if (width != height) {
                throw new IOException("Cubemap images must be square! First image is " + width + "x" + height);
            }
            for (int i = 1; i < imagesToUpload.size(); i++) {
                NativeImage image = imagesToUpload.get(i);
                if (image.getWidth() != width || image.getHeight() != height) {
                    throw new IOException("Cubemap image dimensions do not match. Face 0: " + width + "x" + height + ", Face " + i + ": " + image.getWidth() + "x" + image.getHeight());
                }
            }

            // Create and upload the texture to the GPU.
            GpuDevice device = RenderSystem.getDevice();
            // The magic number 21 is the internal flag for a cubemap texture (GL_TEXTURE_CUBE_MAP).
            this.texture = device.createTexture(() -> "fancymenu_custom_cubemap", 21, TextureFormat.RGBA8, width, height, 6, 1);
            this.textureView = device.createTextureView(this.texture);

            this.setFilter(false, false);
            this.setClamp(true);

            // Upload each NativeImage to its corresponding face on the GPU cubemap.
            for (int i = 0; i < imagesToUpload.size(); i++) {
                device.createCommandEncoder().writeToTexture(this.texture, imagesToUpload.get(i), 0, i, 0, 0, width, height, 0, 0);
            }

        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Failed to load custom cubemap texture", e);
            this.close(); // Ensure cleanup on failure
            throw new IOException("Failed to load custom cubemap texture", e);
        }

        // NOTE: We do NOT close the NativeImages here, because they are owned by the PngTexture instances.
        // We are just borrowing them for the upload.

    }

}