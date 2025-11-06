package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface IMixinMouseHandler {

    @Accessor("lastClickTime") long get_lastClickTime_FancyMenu();

    @Accessor("lastClickTime") void set_lastClickTime_FancyMenu(long time);

    @Accessor("lastClickButton") int get_lastClickButton_FancyMenu();

    @Accessor("lastClickButton") void set_lastClickButton_FancyMenu(int button);

}
