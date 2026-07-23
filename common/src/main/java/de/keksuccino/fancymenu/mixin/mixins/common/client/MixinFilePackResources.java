package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.resource.PackResourcesRootEnumeration;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(FilePackResources.class)
public class MixinFilePackResources {

    /** @reason MC 1.19.2 creates a double-slash prefix when the generic pack API enumerates an archive namespace root, which otherwise hides every root and nested resource in that namespace. */
    @WrapOperation(method = "getResources", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z"))
    private boolean wrap_getResourcesPrefix_FancyMenu(String resourcePath, String prefix, Operation<Boolean> original, PackType type, String namespace, String directory, Predicate<?> filter) {
        // MC 1.19.2 appends a slash to an already slash-terminated namespace root when listing from "".
        return original.call(resourcePath, PackResourcesRootEnumeration.normalizeArchivePrefix(prefix, directory));
    }

}
