package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//TODO übernehmen
@Mixin(Gui.class)
public interface IMixinGui {

    @Accessor("title") Component get_title_FancyMenu();

    @Accessor("subtitle") Component get_subtitle_FancyMenu();

}
