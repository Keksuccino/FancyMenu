package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpectatorGui.class)
public interface IMixinSpectatorGui {

    @Accessor("menu") SpectatorMenu get_menu_FancyMenu();

    @Invoker("getHotbarAlpha") float invoke_getHotbarAlpha_FancyMenu();

}
