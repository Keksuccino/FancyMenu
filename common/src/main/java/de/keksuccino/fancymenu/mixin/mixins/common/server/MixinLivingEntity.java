package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.entities.EntityEventPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
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
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getServer() == null) {
            return;
        }
        this.broadcastEntityDeath_FancyMenu(serverLevel, self);
    }

    @Unique
    private void broadcastEntityDeath_FancyMenu(@NotNull ServerLevel level, @NotNull LivingEntity entity) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.event_type = EntityEventPacket.EntityEventType.DEATH;
        ResourceLocation entityKeyLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        packet.entity_key = (entityKeyLocation != null) ? entityKeyLocation.toString() : null;
        packet.entity_uuid = entity.getUUID().toString();
        packet.pos_x = entity.getX();
        packet.pos_y = entity.getY();
        packet.pos_z = entity.getZ();
        ResourceLocation levelLocation = level.dimension().location();
        packet.level_identifier = (levelLocation != null) ? levelLocation.toString() : null;
        PacketHandler.sendToAllFancyMenuClients(level.getServer(), packet);
    }
}
