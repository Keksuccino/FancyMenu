package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphics.class)
public interface IMixinGuiGraphics {

    @Accessor("bufferSource") MultiBufferSource.BufferSource getBufferSource_FancyMenu();

    @Accessor("scratchItemStackRenderState") ItemStackRenderState get_scratchItemStackRenderState_FancyMenu();

}
