package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface IMixinWindow {

    @Accessor("guiScaledWidth") void set_guiScaledWidth_FancyMenu(int width);

    @Accessor("guiScaledHeight") void set_guiScaledHeight_FancyMenu(int height);

}
