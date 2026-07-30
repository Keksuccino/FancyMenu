package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.resource.PackResourcesRootEnumeration;
import net.minecraft.server.packs.FolderPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FolderPackResources.class)
public class MixinFolderPackResources {

    /** @reason MC 1.19.2 creates a leading-slash resource path when a folder pack is enumerated from its namespace root. */
    @ModifyArg(method = "getResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/FolderPackResources;listResources(Ljava/io/File;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/util/function/Predicate;)V"), index = 3)
    private String modify_getResourcesPrefix_FancyMenu(String prefix) {
        return PackResourcesRootEnumeration.normalizeFolderPrefix(prefix);
    }

}
