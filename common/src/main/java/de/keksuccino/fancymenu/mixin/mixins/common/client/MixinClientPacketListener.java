package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    /** @reason Fire FancyMenu listener whenever the client sends a chat message. */
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void before_sendChat_FancyMenu(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ServerboundChatPacket chatPacket)) {
            return;
        }
        Listeners.ON_CHAT_MESSAGE_SENT.onChatMessageSent(Component.literal(chatPacket.message()));
    }

    /** @reason Fire FancyMenu listener when another player joins the connected server. */
    @Inject(method = "handlePlayerInfo", at = @At("HEAD"))
    private void before_handlePlayerInfoUpdate_FancyMenu(ClientboundPlayerInfoPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ClientboundPlayerInfoPacket.Action.ADD_PLAYER) {
            return;
        }

        UUID localProfileId = Minecraft.getInstance().getUser().getProfileId();

        for (ClientboundPlayerInfoPacket.PlayerUpdate entry : packet.getEntries()) {
            GameProfile profile = entry.getProfile();
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
    @Inject(method = "handlePlayerInfo", at = @At("HEAD"))
    private void before_handlePlayerInfoRemove_FancyMenu(ClientboundPlayerInfoPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER) {
            return;
        }

        UUID localProfileId = Minecraft.getInstance().getUser().getProfileId();
        ClientPacketListener self = (ClientPacketListener)(Object)this;

        for (ClientboundPlayerInfoPacket.PlayerUpdate entry : packet.getEntries()) {
            GameProfile profile = entry.getProfile();
            if (profile == null) {
                continue;
            }

            UUID profileId = profile.getId();
            if (profileId == null || profileId.equals(localProfileId)) {
                continue;
            }

            PlayerInfo playerInfo = self.getPlayerInfo(profileId);
            String playerName = profile.getName();
            if ((playerName == null || playerName.isBlank()) && playerInfo != null) {
                playerName = playerInfo.getProfile().getName();
            }
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

        Entity entity = minecraft.level.getEntity(packet.getPlayerId());
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
        Listeners.ON_DEATH.onDeath(packet.getMessage(), daysSurvived, deathPosition.x, deathPosition.y, deathPosition.z);
    }

    /** @reason Fire FancyMenu listener when the local player picks up an item entity. */
    @WrapOperation(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void wrap_shrinkItem_FancyMenu(ItemStack stack, int amount, Operation<Void> operation, ClientboundTakeItemEntityPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            operation.call(stack, amount);
            return;
        }

        ClientLevel level = minecraft.level;
        Entity potentialCollector = level != null ? level.getEntity(packet.getPlayerId()) : null;
        boolean isLocalCollector = potentialCollector == localPlayer;
        if (!isLocalCollector && potentialCollector == null) {
            isLocalCollector = packet.getPlayerId() == localPlayer.getId();
        }

        String itemKey = null;
        if (!stack.isEmpty()) {
            ResourceLocation itemLocation = Registry.ITEM.getKey(stack.getItem());
            if (itemLocation != null) {
                itemKey = itemLocation.toString();
            }
        }

        operation.call(stack, amount);

        if (!isLocalCollector || itemKey == null) {
            return;
        }

        Listeners.ON_ITEM_PICKED_UP.onItemPickedUp(itemKey);
    }

}
