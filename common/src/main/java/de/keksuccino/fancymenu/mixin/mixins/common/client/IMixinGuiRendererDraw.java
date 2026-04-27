package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import javax.annotation.Nullable;

@Mixin(targets = "net.minecraft.client.gui.render.GuiRenderer$Draw")
public interface IMixinGuiRendererDraw {

    @Accessor("vertexBuffer") GpuBuffer get_vertexBuffer_FancyMenu();

    @Accessor("baseVertex") int get_baseVertex_FancyMenu();

    @Accessor("indexCount") int get_indexCount_FancyMenu();

    @Accessor("pipeline") RenderPipeline get_pipeline_FancyMenu();

    @Accessor("textureSetup") TextureSetup get_textureSetup_FancyMenu();

    @Accessor("scissorArea") @Nullable ScreenRectangle get_scissorArea_FancyMenu();

}
