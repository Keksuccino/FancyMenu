package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphics.class)
public interface IMixinGuiGraphics {

    @Accessor("guiRenderState") GuiRenderState get_guiRenderState_FancyMenu();

    /**
     * Gets the internal ScissorStack instance from a GuiGraphics object.
     * <p>
     * The return type is {@link Object} because the actual type,
     * {@code GuiGraphics.ScissorStack}, is a private inner class and cannot be
     * referenced directly from outside code.
     *
     * @return The ScissorStack instance as an {@link Object}.
     */
    @Accessor("scissorStack") Object get_scissorStack_FancyMenu(); // shows as error in IDE, but should work

}
