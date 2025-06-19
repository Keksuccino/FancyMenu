package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface IMixinRenderPipelines {

    @Accessor("MATRICES_PROJECTION_SNIPPET") static RenderPipeline.Snippet get_MATRICES_PROJECTION_SNIPPET_FancyMenu() { return null; }

    @Invoker("register") static RenderPipeline invoke_register_FancyMenu(RenderPipeline $$0) { return null; }

}
