package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.structure.clientstructures.StructureEventPacket;
import de.keksuccino.fancymenu.util.level.StructureUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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
import java.util.Objects;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer {

    @Unique
    private static final int STRUCTURE_LEAVE_GRACE_TICKS_FANCYMENU = 40;

    @Unique
    private final Map<String, Integer> structureMissingTickCounter_FancyMenu = new HashMap<>();

    @Unique
    private boolean structureHighPrecisionInitialized_FancyMenu;

    @Unique
    private String lastHighPrecisionStructureKey_FancyMenu;

    @Unique
    private static final int STRUCTURE_HIGH_PRECISION_LEAVE_GRACE_TICKS_FANCYMENU = 20;

    @Unique
    private int highPrecisionMissingTickCounter_FancyMenu;

    /** @reason Track structure enter/leave events for FancyMenu listeners. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        this.updateStructureListeners_FancyMenu((ServerPlayer)(Object)this);
    }

    @Unique
    private void updateStructureListeners_FancyMenu(@NotNull ServerPlayer self) {
        if (!PacketHandler.isFancyMenuClient(self)) {
            this.structureMissingTickCounter_FancyMenu.clear();
            this.structureHighPrecisionInitialized_FancyMenu = false;
            this.lastHighPrecisionStructureKey_FancyMenu = null;
            this.highPrecisionMissingTickCounter_FancyMenu = 0;
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

        this.updateHighPrecisionStructureListeners_FancyMenu(self);
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
    private void updateHighPrecisionStructureListeners_FancyMenu(@NotNull ServerPlayer self) {
        String currentStructureKey = this.detectHighPrecisionStructureKey_FancyMenu(self);

        if (!this.structureHighPrecisionInitialized_FancyMenu) {
            this.structureHighPrecisionInitialized_FancyMenu = true;
            this.lastHighPrecisionStructureKey_FancyMenu = currentStructureKey;
            this.highPrecisionMissingTickCounter_FancyMenu = 0;
            if (currentStructureKey != null) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.ENTER_HIGH_PRECISION, currentStructureKey);
            }
            return;
        }

        if (Objects.equals(this.lastHighPrecisionStructureKey_FancyMenu, currentStructureKey)) {
            if (currentStructureKey != null) {
                this.highPrecisionMissingTickCounter_FancyMenu = 0;
            }
            return;
        }

        if (currentStructureKey != null) {
            if (this.lastHighPrecisionStructureKey_FancyMenu != null) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.LEAVE_HIGH_PRECISION, this.lastHighPrecisionStructureKey_FancyMenu);
            }
            this.lastHighPrecisionStructureKey_FancyMenu = currentStructureKey;
            this.highPrecisionMissingTickCounter_FancyMenu = 0;
            this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.ENTER_HIGH_PRECISION, currentStructureKey);
            return;
        }

        if (this.lastHighPrecisionStructureKey_FancyMenu != null) {
            this.highPrecisionMissingTickCounter_FancyMenu++;
            if (this.highPrecisionMissingTickCounter_FancyMenu > STRUCTURE_HIGH_PRECISION_LEAVE_GRACE_TICKS_FANCYMENU) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.LEAVE_HIGH_PRECISION, this.lastHighPrecisionStructureKey_FancyMenu);
                this.lastHighPrecisionStructureKey_FancyMenu = null;
                this.highPrecisionMissingTickCounter_FancyMenu = 0;
            }
        }
    }

    @Unique
    private String detectHighPrecisionStructureKey_FancyMenu(@NotNull ServerPlayer self) {
        if (!(self.level() instanceof ServerLevel level)) {
            return null;
        }

        BlockPos blockPos = self.blockPosition();
        if (!level.isLoaded(blockPos)) {
            return null;
        }

        StructureManager structureManager = level.structureManager();
        StructureStart structureStart = structureManager.getStructureWithPieceAt(blockPos, holder -> true);
        if (structureStart == StructureStart.INVALID_START) {
            return null;
        }

        Structure structure = structureStart.getStructure();
        if (structure == null) {
            return null;
        }

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation location = registry.getKey(structure);
        return (location != null) ? location.toString() : null;
    }

    @Unique
    private void sendStructureEvent_FancyMenu(@NotNull ServerPlayer self, @NotNull StructureEventPacket.StructureEventType type, @NotNull String structureKey) {
        StructureEventPacket packet = new StructureEventPacket();
        packet.event_type = type;
        packet.structure_identifier = structureKey;
        PacketHandler.sendToClient(self, packet);
    }
}
