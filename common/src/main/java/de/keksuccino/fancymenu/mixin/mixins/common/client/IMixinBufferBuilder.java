package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferBuilder.class)
public interface IMixinBufferBuilder {

    @Accessor("buffer") ByteBufferBuilder get_buffer_FancyMenu();

    @Accessor("format") VertexFormat get_format_FancyMenu();

    @Accessor("mode") VertexFormat.Mode get_mode_FancyMenu();

    @Accessor("vertexSize") int get_vertexSize_FancyMenu();

    @Accessor("vertices") int get_vertices_FancyMenu();

    @Accessor("vertices") void set_vertices_FancyMenu(int vertices);

    @Accessor("vertexPointer") void set_vertexPointer_FancyMenu(long vertexPointer);

    @Accessor("elementsToFill") void set_elementsToFill_FancyMenu(int elementsToFill);

    @Invoker("beginElement")
    long invoke_beginElement_FancyMenu(VertexFormatElement element);

}
