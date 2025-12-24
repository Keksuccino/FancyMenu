package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface IMixinNativeImage {

    @Invoker("writeToChannel") boolean invoke_writeToChannel_FancyMenu(WritableByteChannel channel);

}
