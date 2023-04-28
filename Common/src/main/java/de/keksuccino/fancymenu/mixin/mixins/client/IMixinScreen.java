package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface IMixinScreen {

    @Accessor("itemRenderer") void setItemRendererFancyMenu(ItemRenderer itemRenderer);

    @Accessor("font") void setFontFancyMenu(Font font);

    @Accessor("renderables") List<Renderable> getRenderablesFancyMenu();

}
