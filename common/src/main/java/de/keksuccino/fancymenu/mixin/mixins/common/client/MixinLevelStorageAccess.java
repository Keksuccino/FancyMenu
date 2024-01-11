package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.file.Path;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public class MixinLevelStorageAccess {

    @Shadow @Final LevelStorageSource.LevelDirectory levelDirectory;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructFancyMenu(LevelStorageSource $$0, String $$1, Path $$2, CallbackInfo ci) {
        LastWorldHandler.setLastWorld(this.levelDirectory.path().toFile().getPath().replace("\\", "/"), false);
    }

}
