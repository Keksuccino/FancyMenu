package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.authlib.GameProfile;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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

    /** @reason Fire FancyMenu listener when another player dies in the current world. */
    @Inject(method = "handlePlayerCombatKill", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V", shift = At.Shift.AFTER))
    private void after_handlePlayerCombatKillEnsureThread_FancyMenu(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(packet.playerId());
        if (!(entity instanceof Player player) || entity == minecraft.player) {
            return;
        }

        Vec3 deathPosition = player.position();
        String playerName = player.getGameProfile().getName();
        if ((playerName == null || playerName.isBlank()) && player.getName() != null) {
            playerName = player.getName().getString();
        }

        Listeners.ON_OTHER_PLAYER_DIED.onOtherPlayerDied(playerName, player.getUUID(), deathPosition);
    }

    /** @reason Fire FancyMenu listener after the death screen opened for the local player. */
    @Inject(method = "handlePlayerCombatKill", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", shift = At.Shift.AFTER))
    private void after_handlePlayerCombatKillSetScreen_FancyMenu(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Long daysSurvived = null;
        StatsCounter stats = minecraft.player.getStats();
        if (stats != null) {
            int ticksSinceDeath = stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
            if (ticksSinceDeath >= 0) {
                daysSurvived = (long)(ticksSinceDeath / 24000);
            }
        }

        Vec3 deathPosition = minecraft.player.position();
        Listeners.ON_DEATH.onDeath(packet.message(), daysSurvived, deathPosition.x, deathPosition.y, deathPosition.z);
    }
}