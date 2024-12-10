package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.stb.STBImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

@Mixin(NativeImage.class)
public interface IMixinNativeImage {

    @Invoker("writeToChannel") boolean invoke_writeToChannel_FancyMenu(WritableByteChannel channel);

}
