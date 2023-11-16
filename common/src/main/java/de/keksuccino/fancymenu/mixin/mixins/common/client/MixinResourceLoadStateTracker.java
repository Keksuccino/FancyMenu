package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public class MixinResourceLoadStateTracker {

    @Inject(method = "startReload", at = @At("HEAD"))
    private void beforeStartReloadFancyMenu(CallbackInfo info) {
        if (((IMixinMinecraft)Minecraft.getInstance()).getReloadStateTrackerFancyMenu() == ((Object)this)) {
            MinecraftResourceReloadObserver.getReloadListeners().forEach(reloadActionConsumer -> reloadActionConsumer.accept(MinecraftResourceReloadObserver.ReloadAction.STARTING));
        }
    }

    @Inject(method = "finishReload", at = @At("RETURN"))
    private void afterFinishReloadFancyMenu(CallbackInfo info) {
        if (((IMixinMinecraft)Minecraft.getInstance()).getReloadStateTrackerFancyMenu() == ((Object)this)) {
            MinecraftResourceReloadObserver.getReloadListeners().forEach(reloadActionConsumer -> reloadActionConsumer.accept(MinecraftResourceReloadObserver.ReloadAction.FINISHED));
        }
    }

}
