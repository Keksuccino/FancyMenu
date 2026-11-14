package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public interface IMixinBufferBuilder {

    @Accessor("buffer") ByteBuffer get_buffer_FancyMenu();

    @Accessor("format") VertexFormat get_format_FancyMenu();

    @Accessor("mode") VertexFormat.Mode get_mode_FancyMenu();

    @Accessor("indexOnly") boolean get_indexOnly_FancyMenu();

    @Accessor("renderedBufferPointer") int get_renderedBufferPointer_FancyMenu();

    @Accessor("nextElementByte") void set_nextElementByte_FancyMenu(int nextElementByte);

    @Accessor("vertices") int get_vertices_FancyMenu();

    @Accessor("vertices") void set_vertices_FancyMenu(int vertices);

    @Invoker("ensureCapacity")
    void invoke_ensureCapacity_FancyMenu(int increaseAmount);

}
