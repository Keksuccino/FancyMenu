package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface IMixinScreen {

    @Accessor("renderables") public List<Renderable> getRenderablesFancyMenu();

    @Accessor("itemRenderer") public void setItemRendererFancyMenu(ItemRenderer renderer);

    @Accessor("font") public void setFontFancyMenu(Font font);

}
