package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.structures.StructureEventPacket;
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

import java.util.Objects;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer {

    @Unique
    private boolean structureStateInitialized_FancyMenu;

    @Unique
    private String lastKnownStructureKey_FancyMenu;

    /** @reason Track structure enter/leave events for FancyMenu listeners. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        this.updateStructureListeners_FancyMenu((ServerPlayer)(Object)this);
    }

    @Unique
    private void updateStructureListeners_FancyMenu(@NotNull ServerPlayer self) {
        if (!PacketHandler.isFancyMenuClient(self)) {
            this.structureStateInitialized_FancyMenu = false;
            this.lastKnownStructureKey_FancyMenu = null;
            return;
        }

        String currentStructureKey = this.detectStructureKey_FancyMenu(self);

        if (!this.structureStateInitialized_FancyMenu) {
            this.structureStateInitialized_FancyMenu = true;
            this.lastKnownStructureKey_FancyMenu = currentStructureKey;
            if (currentStructureKey != null) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.ENTER, currentStructureKey);
            }
            return;
        }

        if (!Objects.equals(this.lastKnownStructureKey_FancyMenu, currentStructureKey)) {
            String previousStructureKey = this.lastKnownStructureKey_FancyMenu;
            if (previousStructureKey != null) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.LEAVE, previousStructureKey);
            }
            this.lastKnownStructureKey_FancyMenu = currentStructureKey;
            if (currentStructureKey != null) {
                this.sendStructureEvent_FancyMenu(self, StructureEventPacket.StructureEventType.ENTER, currentStructureKey);
            }
        }
    }

    @Unique
    private String detectStructureKey_FancyMenu(@NotNull ServerPlayer self) {
        ServerLevel level = self.serverLevel();
        BlockPos blockPos = self.blockPosition();
        if (!level.hasChunkAt(blockPos)) {
            return null;
        }

        StructureManager structureManager = level.structureManager();
        StructureStart structureStart = structureManager.getStructureWithPieceAt(blockPos, holder -> true);
        if (structureStart == StructureStart.INVALID_START) {
            return null;
        }

        Structure structure = structureStart.getStructure();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation key = registry.getKey(structure);
        return (key != null) ? key.toString() : null;
    }

    @Unique
    private void sendStructureEvent_FancyMenu(@NotNull ServerPlayer self, @NotNull StructureEventPacket.StructureEventType type, @NotNull String structureKey) {
        StructureEventPacket packet = new StructureEventPacket();
        packet.event_type = type;
        packet.structure_identifier = structureKey;
        PacketHandler.sendToClient(self, packet);
    }
}
