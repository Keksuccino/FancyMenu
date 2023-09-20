package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.mixin.MixinCache;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At("RETURN"))
    private void resetCachedCurrentRenderScreenFancyMenu(float $$0, long $$1, boolean $$2, CallbackInfo info) {
        MixinCache.currentRenderScreen = null;
    }

}
