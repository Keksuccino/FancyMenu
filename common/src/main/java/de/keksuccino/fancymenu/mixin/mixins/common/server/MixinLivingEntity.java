package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.entities.EntityEventPacket;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    /** @reason Broadcast FancyMenu entity death events to connected clients. */
    @Inject(method = "die", at = @At("TAIL"))
    private void after_die_FancyMenu(@NotNull DamageSource damageSource, CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self.level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getServer() == null) {
            return;
        }
        this.broadcastEntityDeath_FancyMenu(serverLevel, self, damageSource);
    }

    @Unique
    private void broadcastEntityDeath_FancyMenu(@NotNull ServerLevel level, @NotNull LivingEntity entity, @NotNull DamageSource damageSource) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.event_type = EntityEventPacket.EntityEventType.DEATH;
        ResourceLocation entityKeyLocation = Registry.ENTITY_TYPE.getKey(entity.getType());
        packet.entity_key = (entityKeyLocation != null) ? entityKeyLocation.toString() : null;
        packet.entity_uuid = entity.getUUID().toString();
        packet.pos_x = entity.getX();
        packet.pos_y = entity.getY();
        packet.pos_z = entity.getZ();
        ResourceLocation levelLocation = level.dimension().location();
        packet.level_identifier = (levelLocation != null) ? levelLocation.toString() : null;
        packet.damage_type = this.resolveDamageTypeKey_FancyMenu(damageSource);

        Entity killer = this.resolveKillerEntity_FancyMenu(entity, damageSource);
        if (killer != null) {
            packet.killer_name = killer.getDisplayName().getString();
            packet.killer_uuid = killer.getUUID().toString();
            ResourceLocation killerKeyLocation = Registry.ENTITY_TYPE.getKey(killer.getType());
            packet.killer_key = (killerKeyLocation != null) ? killerKeyLocation.toString() : null;
            if (packet.killer_key == null && killer instanceof net.minecraft.world.entity.player.Player) {
                packet.killer_key = "minecraft:player";
            }
        } else {
            packet.killer_name = null;
            packet.killer_uuid = null;
            packet.killer_key = null;
        }

        PacketHandler.sendToAllFancyMenuClients(level.getServer(), packet);
    }

    @Unique
    @Nullable
    private Entity resolveKillerEntity_FancyMenu(@NotNull LivingEntity victim, @Nullable DamageSource damageSource) {
        if (damageSource != null) {
            Entity killer = damageSource.getEntity();
            if (killer != null) {
                return killer;
            }
            Entity directEntity = damageSource.getDirectEntity();
            if (directEntity != null) {
                return directEntity;
            }
        }
        LivingEntity killCredit = victim.getKillCredit();
        return killCredit != null ? killCredit : null;
    }

    @Unique
    @NotNull
    private String resolveDamageTypeKey_FancyMenu(@Nullable DamageSource damageSource) {
        if (damageSource == null) {
            return "unknown";
        }
        String messageId = damageSource.getMsgId();
        if (messageId == null || messageId.isEmpty()) {
            return "unknown";
        }
        String namespacedId = messageId.contains(":") ? messageId : ("minecraft:" + messageId);
        ResourceLocation location = ResourceLocation.tryParse(namespacedId);
        return location != null ? location.toString() : messageId;
    }

}
