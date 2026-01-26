package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.time.Instant;
import java.util.UUID;

@Mixin(ChatListener.class)
public class MixinChatListener {

    /** @reason Capture chat messages shown to the local client. */
    @WrapOperation(
        method = "showMessageToPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"
        )
    )
    private void wrap_addMessage_showMessageToPlayer_FancyMenu(
        ChatComponent instance,
        Component component,
        MessageSignature messageSignature,
        GuiMessageTag guiMessageTag,
        Operation<Void> operation,
        ChatType.Bound boundChatType,
        PlayerChatMessage chatMessage,
        Component decoratedServerContent,
        GameProfile gameProfile,
        boolean onlyShowSecureChat,
        Instant timestamp
    ) {
        operation.call(instance, component, messageSignature, guiMessageTag);

        UUID senderUuid = gameProfile != null ? gameProfile.getId() : null;
        Component senderNameComponent = (gameProfile != null && gameProfile.getName() != null)
            ? Component.literal(gameProfile.getName())
            : null;

        Listeners.ON_CHAT_MESSAGE_RECEIVED.onChatMessageReceived(component, senderUuid, senderNameComponent);
    }

    /** @reason Capture command feedback/system messages shown to the local client. */
    @WrapOperation(
        method = "handleSystemMessage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;)V"
        )
    )
    private void wrap_addMessage_handleSystemMessage_FancyMenu(
        ChatComponent instance,
        Component component,
        Operation<Void> operation
    ) {
        operation.call(instance, component);
        Listeners.ON_SYSTEM_MESSAGE_RECEIVED_IN_CHAT.onSystemMessageReceivedInChat(component);
    }
}
