package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.structures.StructureEventPacket;
import de.keksuccino.fancymenu.util.level.StructureUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer {

    @Unique
    private static final int STRUCTURE_LEAVE_GRACE_TICKS_FANCYMENU = 40;

    @Unique
    private final Map<String, Integer> structureMissingTickCounter_FancyMenu = new HashMap<>();

    /** @reason Track structure enter/leave events for FancyMenu listeners. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        this.updateStructureListeners_FancyMenu((ServerPlayer)(Object)this);
    }

    @Unique
    private void updateStructureListeners_FancyMenu(@NotNull ServerPlayer self) {
        if (!PacketHandler.isFancyMenuClient(self)) {
            this.structureMissingTickCounter_FancyMenu.clear();
            return;
        }

        Set<String> currentStructureKeys = this.detectStructureKeys_FancyMenu(self);

        // Reset counters for structures we currently detect and fire enters for newly discovered ones
        for (String currentKey : currentStructureKeys) {
            if (!this.structureMissingTickCounter_FancyMenu.containsKey(currentKey)) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.ENTER, currentKey);
            }
            this.structureMissingTickCounter_FancyMenu.put(currentKey, 0);
        }

        // Handle structures that have not been detected for a while
        Set<String> trackedKeys = new HashSet<>(this.structureMissingTickCounter_FancyMenu.keySet());
        for (String trackedKey : trackedKeys) {
            if (currentStructureKeys.contains(trackedKey)) {
                continue;
            }
            int newCounter = this.structureMissingTickCounter_FancyMenu.getOrDefault(trackedKey, 0) + 1;
            if (newCounter > STRUCTURE_LEAVE_GRACE_TICKS_FANCYMENU) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.LEAVE, trackedKey);
                this.structureMissingTickCounter_FancyMenu.remove(trackedKey);
            } else {
                this.structureMissingTickCounter_FancyMenu.put(trackedKey, newCounter);
            }
        }
    }

    @Unique
    private Set<String> detectStructureKeys_FancyMenu(@NotNull ServerPlayer self) {
        if (!(self.level() instanceof ServerLevel level)) {
            return Set.of();
        }

        BlockPos blockPos = self.blockPosition();
        if (!level.isLoaded(blockPos)) {
            return Set.of();
        }

        return new HashSet<>(StructureUtils.convertStructureKeysToStrings(StructureUtils.getAllStructuresAt(level, blockPos)));
    }

    @Unique
    private void sendStructureEvent_FancyMenu(@NotNull ServerPlayer self, @NotNull StructureEventPacket.StructureEventType type, @NotNull String structureKey) {
        StructureEventPacket packet = new StructureEventPacket();
        packet.event_type = type;
        packet.structure_identifier = structureKey;
        PacketHandler.sendToClient(self, packet);
    }
}
