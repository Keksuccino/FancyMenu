package de.keksuccino.fancymenu.mixin.mixins.common.server;

import de.keksuccino.fancymenu.util.level.raid.ServerRaidObserver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raid.class)
public abstract class MixinRaid {

    @Shadow @Final private ServerBossEvent raidEvent;
    @Shadow @Final private int numGroups;

    @Unique private final ServerRaidObserver raidObserver_FancyMenu = new ServerRaidObserver();

    @Shadow protected abstract boolean hasBonusWave();

    // Only new raids use this constructor; raids loaded from a save use the NBT constructor and were already going on before
    @Inject(method = "<init>(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
    private void after_init_FancyMenu(int id, ServerLevel level, BlockPos center, CallbackInfo info) {
        this.raidObserver_FancyMenu.markFreshlyCreated();
    }

    // The raid bar audience only changes here, so this is where players that are around when the raid starts get captured
    @Inject(method = "updatePlayers", at = @At("RETURN"))
    private void after_updatePlayers_FancyMenu(CallbackInfo info) {
        this.raidObserver_FancyMenu.onAudienceUpdated(this.raidEvent);
    }

    // RETURN instead of TAIL, because the tick method has many early returns
    @Inject(method = "tick", at = @At("RETURN"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        this.raidObserver_FancyMenu.onTick((Raid)(Object)this, this.raidEvent, this.getTotalWaves_FancyMenu());
    }

    @Inject(method = "stop", at = @At("RETURN"))
    private void after_stop_FancyMenu(CallbackInfo info) {
        this.raidObserver_FancyMenu.onStopped();
    }

    @Unique
    private int getTotalWaves_FancyMenu() {
        return this.numGroups + (this.hasBonusWave() ? 1 : 0);
    }

}
