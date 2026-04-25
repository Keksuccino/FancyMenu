package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface IMixinGameRenderer {

    @Accessor("guiRenderer")
    GuiRenderer get_guiRenderer_FancyMenu();

    @Accessor("fogRenderer")
    FogRenderer get_fogRenderer_FancyMenu();

}
