package de.keksuccino.fancymenu.mixin.mixins.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface IMixinChatScreen {

    @Accessor("input") EditBox getInputFancyMenu();

}
