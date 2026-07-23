package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.DataResult;
import de.keksuccino.fancymenu.util.resource.PackResourcesRootEnumeration;
import net.minecraft.server.packs.PathPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PathPackResources.class)
public class MixinPathPackResources {

    /** @reason Minecraft rejects the empty directory required to enumerate a path pack's namespace root through the generic pack API. */
    @WrapOperation(method = "listResources(Ljava/nio/file/Path;Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/server/packs/PackResources$ResourceOutput;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FileUtil;decomposePath(Ljava/lang/String;)Lcom/mojang/serialization/DataResult;"))
    private static DataResult<List<String>> wrap_listResourcesDirectory_FancyMenu(String directory, Operation<DataResult<List<String>>> original) {
        return PackResourcesRootEnumeration.decomposeDirectory(directory, original);
    }

}
