package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    /** @reason Fire FancyMenu listener whenever the client sends a chat message. */
    @Inject(method = "sendChat", at = @At("HEAD"))
    private void before_sendChat_FancyMenu(String message, CallbackInfo ci) {
        Listeners.ON_CHAT_MESSAGE_SENT.onChatMessageSent(Component.literal(message));
    }
}