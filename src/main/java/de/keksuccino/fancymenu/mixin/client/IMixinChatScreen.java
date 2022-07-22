//TODO übernehmen
package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatScreen.class)
public interface IMixinChatScreen {

    @Accessor("input") public TextFieldWidget getInputFancyMenu();

}
