package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface IMixinGuiGraphics {

    @Accessor("guiRenderState") GuiRenderState fancymenu$getGuiRenderState();

    @Accessor("scissorStack") IMixinScissorStack fancymenu$getScissorStack();

}
