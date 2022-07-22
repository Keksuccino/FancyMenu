package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface IMixinScreen {

    @Accessor("buttons") public List<Widget> getButtonsFancyMenu();

    @Accessor("buttons") public void setButtonsFancyMenu(List<Widget> buttons);

    @Accessor("itemRenderer") public ItemRenderer getItemRendererFancyMenu();

    @Accessor("itemRenderer") public void setItemRendererFancyMenu(ItemRenderer r);

    @Accessor("font") public FontRenderer getFontFancyMenu();

    @Accessor("font") public void setFontFancyMenu(FontRenderer f);

}
