package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphics.class)
public interface IMixinGuiGraphics {

    @Accessor("guiRenderState") GuiRenderState get_guiRenderState_FancyMenu();

    @Accessor("scissorStack") GuiGraphics.ScissorStack get_scissorStack_FancyMenu();

}
