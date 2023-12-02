package de.keksuccino.fancymenu.util.file.type.types;

import de.keksuccino.fancymenu.util.file.type.FileCodec;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImageFileType extends FileType<ITexture> {

    protected boolean animated = false;

    public ImageFileType(@NotNull FileCodec<ITexture> codec, @Nullable String mimeType, @NotNull String... extensions) {
        super(codec, mimeType, FileMediaType.IMAGE, extensions);
    }

    public boolean isAnimated() {
        return this.animated;
    }

    public ImageFileType setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }

    @Override
    public ImageFileType addExtension(@NotNull String extension) {
        return (ImageFileType) super.addExtension(extension);
    }

    @Override
    public ImageFileType removeExtension(@NotNull String extension) {
        return (ImageFileType) super.removeExtension(extension);
    }

    @Override
    public ImageFileType setCodec(@NotNull FileCodec<ITexture> codec) {
        return (ImageFileType) super.setCodec(codec);
    }

    @Override
    public ImageFileType setLocationAllowed(boolean allowLocation) {
        return (ImageFileType) super.setLocationAllowed(allowLocation);
    }

    @Override
    public ImageFileType setLocalAllowed(boolean allowLocal) {
        return (ImageFileType) super.setLocalAllowed(allowLocal);
    }

    @Override
    public ImageFileType setWebAllowed(boolean allowWeb) {
        return (ImageFileType) super.setWebAllowed(allowWeb);
    }

    @Override
    public ImageFileType setCustomDisplayName(@Nullable Component name) {
        return (ImageFileType) super.setCustomDisplayName(name);
    }

}
