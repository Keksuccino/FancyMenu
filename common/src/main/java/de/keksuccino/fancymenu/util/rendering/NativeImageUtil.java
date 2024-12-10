package de.keksuccino.fancymenu.util.rendering;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinNativeImage;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.stb.STBImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class NativeImageUtil {

    public static byte[] asByteArray(@NotNull NativeImage image) throws IOException {
        byte[] var3;
        try (
                ByteArrayOutputStream $$0 = new ByteArrayOutputStream();
                WritableByteChannel $$1 = Channels.newChannel($$0);
        ) {
            if (!((IMixinNativeImage)((Object)image)).invoke_writeToChannel_FancyMenu($$1)) {
                throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
            }
            var3 = $$0.toByteArray();
        }
        return var3;
    }

}
