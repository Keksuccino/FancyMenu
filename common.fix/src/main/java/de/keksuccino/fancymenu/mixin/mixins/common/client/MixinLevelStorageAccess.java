package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class MixinLevelStorageAccess {

    private static final Logger LOGGER = LogManager.getLogger();

    @Shadow @Final LevelStorageSource.LevelDirectory levelDirectory;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructFancyMenu(LevelStorageSource this$0, String p_78276_, CallbackInfo info) {
        LastWorldHandler.setLastWorld(this.levelDirectory.path().toFile().getPath().replace("\\", "/"), false);
    }

}
