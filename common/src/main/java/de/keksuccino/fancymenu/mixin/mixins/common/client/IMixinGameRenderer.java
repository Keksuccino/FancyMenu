package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface IMixinGameRenderer {

    @Accessor("resourcePool")
    CrossFrameResourcePool get_resourcePool_FancyMenu();

}
