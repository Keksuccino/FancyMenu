package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSelectionList.class)
public interface IMixinAbstractSelectionList {

    @Accessor("x0") int getX0FancyMenu();
    @Accessor("x0") void setX0FancyMenu(int x0);

    @Accessor("y0") int getY0FancyMenu();
    @Accessor("y0") void setY0FancyMenu(int y0);

    @Accessor("x1") int getX1FancyMenu();
    @Accessor("x1") void setX1FancyMenu(int x1);

    @Accessor("y1") int getY1FancyMenu();
    @Accessor("y1") void setY1FancyMenu(int y1);

    @Accessor("width") int getWidthFancyMenu();
    @Accessor("width") void setWidthFancyMenu(int width);

    @Accessor("height") int getHeightFancyMenu();
    @Accessor("height") void setHeightFancyMenu(int height);

}
