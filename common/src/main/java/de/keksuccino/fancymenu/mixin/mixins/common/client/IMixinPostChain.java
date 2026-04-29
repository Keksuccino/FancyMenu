package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(PostChain.class)
public interface IMixinPostChain {

    @Accessor("passes")
    List<PostPass> getPasses_FancyMenu();

    @Accessor("screenTarget")
    RenderTarget getScreenTarget_FancyMenu();

    @Accessor("customRenderTargets")
    Map<String, RenderTarget> getCustomRenderTargets_FancyMenu();

}
