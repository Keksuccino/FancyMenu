package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.authlib.GameProfile;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    /** @reason Fire FancyMenu listener whenever the client sends a chat message. */
    @Inject(method = "sendChat", at = @At("HEAD"))
    private void before_sendChat_FancyMenu(String message, CallbackInfo ci) {
        Listeners.ON_CHAT_MESSAGE_SENT.onChatMessageSent(Component.literal(message));
    }

    /** @reason Fire FancyMenu listener when another player joins the connected server. */
    @Inject(method = "handlePlayerInfoUpdate", at = @At("HEAD"))
    private void before_handlePlayerInfoUpdate_FancyMenu(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
            return;
        }

        UUID localProfileId = Minecraft.getInstance().getUser().getProfileId();

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile == null) {
                continue;
            }

            UUID profileId = profile.getId();
            if (profileId == null || profileId.equals(localProfileId)) {
                continue;
            }

            Listeners.ON_OTHER_PLAYER_JOINED_WORLD.onOtherPlayerJoined(profile.getName(), profileId);
        }
    }

    /** @reason Fire FancyMenu listener when another player leaves the connected server. */
    @Inject(method = "handlePlayerInfoRemove", at = @At("HEAD"))
    private void before_handlePlayerInfoRemove_FancyMenu(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        UUID localProfileId = Minecraft.getInstance().getUser().getProfileId();
        ClientPacketListener self = (ClientPacketListener)(Object)this;

        for (UUID profileId : packet.profileIds()) {
            if (profileId == null || profileId.equals(localProfileId)) {
                continue;
            }

            PlayerInfo playerInfo = self.getPlayerInfo(profileId);
            String playerName = playerInfo != null ? playerInfo.getProfile().getName() : null;
            Listeners.ON_OTHER_PLAYER_LEFT_WORLD.onOtherPlayerLeft(playerName, profileId);
        }
    }

}