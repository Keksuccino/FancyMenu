package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Widget.class)
public interface IMixinWidget {

    @Accessor("alpha") public float getAlphaFancyMenu();

    @Accessor("alpha") public void setAlphaFancyMenu(float f);

}
