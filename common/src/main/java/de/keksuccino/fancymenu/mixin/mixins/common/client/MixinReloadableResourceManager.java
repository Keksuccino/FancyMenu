package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;
import java.util.List;

@Mixin(ReloadableResourceManager.class)
public class MixinReloadableResourceManager {

    @Mutable
    @Shadow @Final private List<PreparableReloadListener> listeners;

    @WrapOperation(method = "registerReloadListener", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean wrap_add_in_registerReloadListener_FancyMenu(List<?> instance, Object e, Operation<Boolean> original) {
        try {
            return original.call(instance, e);
        } catch (UnsupportedOperationException ignore) {
            this.listeners = new ArrayList<>(this.listeners);
            return this.listeners.add((PreparableReloadListener) e);
        }
    }

}
