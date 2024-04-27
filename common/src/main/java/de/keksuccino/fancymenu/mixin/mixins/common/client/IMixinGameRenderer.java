package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface IMixinGameRenderer {

    @Accessor("blurEffect") PostChain getBlurEffect_FancyMenu();

}
