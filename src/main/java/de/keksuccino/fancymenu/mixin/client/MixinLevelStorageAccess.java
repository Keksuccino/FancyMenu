package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.world.LastWorldHandler;
import net.minecraft.world.storage.SaveFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(SaveFormat.LevelSave.class)
public class MixinLevelStorageAccess {

    private static final Logger MIXIN_LOGGER = LogManager.getLogger("fancymenu/mixin/LevelStorageAccess");

    @Shadow @Final private Path levelPath;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onInit(SaveFormat this$0, String saveName, CallbackInfo info) {
        LastWorldHandler.setLastWorld(this.levelPath.toFile().getPath().replace("\\", "/"), false);
    }

}
