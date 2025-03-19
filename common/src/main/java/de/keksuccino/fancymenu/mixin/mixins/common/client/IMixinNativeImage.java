package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface IMixinNativeImage {

    @Accessor("pixels") long get_pixels_FancyMenu();

    @Invoker("<init>")
    static NativeImage invoke_class_constructor_FancyMenu(NativeImage.Format format, int width, int height, boolean useStbFree, long pixels) {
        throw new AssertionError("Invoker not implemented");
    }

    @Invoker("writeToChannel") boolean invoke_writeToChannel_FancyMenu(WritableByteChannel channel);

}
