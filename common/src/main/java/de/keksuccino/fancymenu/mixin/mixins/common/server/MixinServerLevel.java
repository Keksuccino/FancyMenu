package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.entities.EntityEventPacket;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class MixinServerLevel {

    /** @reason Broadcast FancyMenu entity spawn events to connected clients. */
    @Inject(method = "addEntity", at = @At("RETURN"))
    private void after_addEntity_FancyMenu(@NotNull Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (entity.tickCount > 0) {
            return;
        }
        this.broadcastEntitySpawn_FancyMenu((ServerLevel)(Object)this, entity);
    }

    @Unique
    private void broadcastEntitySpawn_FancyMenu(@NotNull ServerLevel level, @NotNull Entity entity) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.event_type = EntityEventPacket.EntityEventType.SPAWN;
        ResourceLocation entityKeyLocation = Registry.ENTITY_TYPE.getKey(entity.getType());
        packet.entity_key = (entityKeyLocation != null) ? entityKeyLocation.toString() : null;
        packet.entity_uuid = entity.getUUID().toString();
        packet.pos_x = entity.getX();
        packet.pos_y = entity.getY();
        packet.pos_z = entity.getZ();
        ResourceLocation levelLocation = level.dimension().location();
        packet.level_identifier = (levelLocation != null) ? levelLocation.toString() : null;
        if (level.getServer() == null) {
            return;
        }
        PacketHandler.sendToAllFancyMenuClients(level.getServer(), packet);
    }
}
