package de.keksuccino.fancymenu.util.resources.texture;

import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

@SuppressWarnings("unused")
public class WrappedWebTexture implements ITexture {

    @Nullable
    protected ITexture texture;
    protected final AspectRatio placeholderAspectRatio = new AspectRatio(10, 10);

    /**
     * Supports PNG, JPEG and GIF.<br>
     * In case of PNG or JPEG, a new {@link SimpleWebTexture} will get wrapped into the {@link WrappedWebTexture}.<br>
     * In case of GIF, a new {@link GifTexture} will get wrapped into the {@link WrappedWebTexture}.
     *
     * @param imageUrl The image URL.
     * @param autoLoadPngAndJpeg If PNG and JPEG images should automatically (asynchronously) load. GIF images will always load automatically.
     */
    @NotNull
    public static WrappedWebTexture of(@NotNull String imageUrl, boolean autoLoadPngAndJpeg) {

        Objects.requireNonNull(imageUrl);

        WrappedWebTexture texture = WrappedWebTexture.empty();

        new Thread(() -> {
            try {
                if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(imageUrl)) return;
                if (FileTypes.GIF_IMAGE.isFileTypeWeb(imageUrl)) {
                    texture.setWrappedTexture(GifTexture.web(imageUrl));
                } else {
                    texture.setWrappedTexture(SimpleWebTexture.of(imageUrl, autoLoadPngAndJpeg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();

        return texture;

    }

    @NotNull
    public static WrappedWebTexture empty() {
        return new WrappedWebTexture();
    }

    protected WrappedWebTexture() {
    }

    @Override
    public @Nullable ResourceLocation getResourceLocation() {
        return (this.texture != null) ? this.texture.getResourceLocation() : null;
    }

    @Override
    public int getWidth() {
        return (this.texture != null) ? this.texture.getWidth() : 10;
    }

    @Override
    public int getHeight() {
        return (this.texture != null) ? this.texture.getHeight() : 10;
    }

    @Override
    public @NotNull AspectRatio getAspectRatio() {
        return (this.texture != null) ? this.texture.getAspectRatio() : this.placeholderAspectRatio;
    }

    @Override
    public boolean isReady() {
        return (this.texture != null) && this.texture.isReady();
    }

    @Override
    public void reset() {
        if (this.texture != null) this.texture.reset();
    }

    public void setWrappedTexture(@Nullable ITexture texture) {
        this.texture = texture;
    }

    @Nullable
    public ITexture getWrappedTexture() {
        return this.texture;
    }

}
