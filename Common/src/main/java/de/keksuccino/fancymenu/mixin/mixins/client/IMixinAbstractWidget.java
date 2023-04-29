package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(AbstractWidget.class)
public interface IMixinAbstractWidget {

    @Accessor("alpha") float getAlphaFancyMenu();

    @Accessor("height") int getHeightFancyMenu();

    @Accessor("height") void setHeightFancyMenu(int height);

}
