package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ByteBufferBuilder.class)
public interface IMixinByteBufferBuilder {

    @Accessor("pointer") long get_pointer_FancyMenu();

    @Accessor("writeOffset") int get_writeOffset_FancyMenu();

    @Accessor("writeOffset") void set_writeOffset_FancyMenu(int writeOffset);

}
