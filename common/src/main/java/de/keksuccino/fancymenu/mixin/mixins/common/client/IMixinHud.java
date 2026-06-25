package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface IMixinHud {

    @Accessor("title") Component get_title_FancyMenu();

    @Accessor("subtitle") Component get_subtitle_FancyMenu();

    @Accessor("overlayMessageString") Component get_overlayMessageString_FancyMenu();

    @Accessor("overlayMessageTime") int get_overlayMessageTime_FancyMenu();

    @Accessor("toolHighlightTimer") int get_toolHighlightTimer_FancyMenu();

}
