package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.DataResult;
import de.keksuccino.fancymenu.util.resource.PackResourcesRootEnumeration;
import net.minecraft.server.packs.VanillaPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(VanillaPackResources.class)
public class MixinVanillaPackResources {

    /** @reason MC 26.2 rejects the empty directory required to enumerate the vanilla pack's namespace root through the generic pack API. */
    @WrapOperation(method = "listResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FileUtil;decomposePath(Ljava/lang/String;)Lcom/mojang/serialization/DataResult;"))
    private DataResult<List<String>> wrap_listResourcesDirectory_FancyMenu(String directory, Operation<DataResult<List<String>>> original) {
        return PackResourcesRootEnumeration.decomposeDirectory(directory, original);
    }

}
