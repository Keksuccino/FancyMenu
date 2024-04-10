package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSelectionList.class)
public interface IMixinAbstractSelectionList {

    @Accessor("x0") int getX0FancyMenu();

    @Accessor("y0") int getY0FancyMenu();

    @Accessor("x1") int getX1FancyMenu();

    @Accessor("y1") int getY1FancyMenu();

    @Accessor("width") int getWidthFancyMenu();

    @Accessor("height") int getHeightFancyMenu();

}
