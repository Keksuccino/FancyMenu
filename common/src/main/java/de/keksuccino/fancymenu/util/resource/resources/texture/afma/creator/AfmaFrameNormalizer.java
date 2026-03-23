package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.util.resource.resources.texture.afma.AfmaNativeImageHelper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class AfmaFrameNormalizer {

    @NotNull
    public NativeImage loadFrame(@NotNull File file) throws IOException {
        Objects.requireNonNull(file);
        try (InputStream in = new FileInputStream(file)) {
            return NativeImage.read(in);
        }
    }

    @NotNull
    public NativeImage extractPatch(@NotNull NativeImage image, int x, int y, int width, int height) {
        return AfmaNativeImageHelper.crop(image, x, y, width, height);
    }

}
