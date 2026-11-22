package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.keksuccino.fancymenu.util.level.HeldDirectoryLocks;
import net.minecraft.util.DirectoryLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.nio.file.Path;

@Mixin(DirectoryLock.class)
public class MixinDirectoryLock {

    @Inject(method = "create", at = @At("RETURN"))
    private static void after_create_FancyMenu(Path path, CallbackInfoReturnable<DirectoryLock> info) {
        HeldDirectoryLocks.add(path, info.getReturnValue());
    }

    /**
     * @reason Probing a lock that this game holds itself fails and releases that lock for other processes, like when world placeholders
     * read the world list while a singleplayer world is open, so folders this game holds a lock for get reported as locked right away.
     */
    @WrapMethod(method = "isLocked")
    private static boolean wrap_isLocked_FancyMenu(Path path, Operation<Boolean> original) {
        if (HeldDirectoryLocks.isHeld(path)) return true;
        return original.call(path);
    }

}
