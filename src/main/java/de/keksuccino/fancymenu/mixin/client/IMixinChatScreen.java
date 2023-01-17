
package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiChat.class)
public interface IMixinChatScreen {

    @Accessor("inputField") public GuiTextField getInputFancyMenu();

}
