package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "org.watermedia.bootstrap.NeoBootstrap")
public class MixinWatermediaNeoBootstrap {

    /**
     * @reason WaterMedia 3.0.0.15 still calls the pre-26.x static FMLLoader#getDist API from
     * its NeoForge bootstrap. Redirect it through the active loader instance so WaterMedia can
     * finish bootstrapping on Minecraft 26.1.1.
     */
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/FMLLoader;getDist()Lnet/neoforged/api/distmarker/Dist;", remap = false), remap = false, require = 0)
    private Dist wrap_getDist_FancyMenu(Operation<Dist> original) {
        return FMLLoader.getCurrent().getDist();
    }

}
