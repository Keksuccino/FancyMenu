package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public class MixinWorldOpenFlows {

    @Shadow @Final private LevelStorageSource levelSource;

    @Inject(method = "openWorld(Ljava/lang/String;Ljava/lang/Runnable;)V", at = @At("HEAD"))
    private void before_openWorld_FancyMenu(String levelName, Runnable onCancel, CallbackInfo info) {
        try {
            SeamlessWorldLoadingHandler.preWarmWorldLoad(this.levelSource.getLevelPath(levelName).toAbsolutePath().toString());
        } catch (Exception ex) {
            SeamlessWorldLoadingHandler.preWarmWorldLoad(levelName);
        }
    }

}
