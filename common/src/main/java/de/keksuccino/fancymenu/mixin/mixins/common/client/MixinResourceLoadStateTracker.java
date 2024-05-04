package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.MinecraftResourceReloadObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.server.packs.PackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(ResourceLoadStateTracker.class)
public class MixinResourceLoadStateTracker {

    @Unique
    private static boolean ScreenCustomization_init_called_FancyMenu = false;

    /**
     * @reason The first part exists because FancyMenu needs to initialize its customization engine as early as possible, but AFTER all other mods got initialized, so we run ScreenCustomization#init() in the GameNarrator constructor, which gets called before the loading screen overlay gets set for the first time.
     */
    @Inject(method = "startReload", at = @At("HEAD"))
    private void before_startReload_FancyMenu(ResourceLoadStateTracker.ReloadReason reason, List<PackResources> resources, CallbackInfo info) {
        if ((reason == ResourceLoadStateTracker.ReloadReason.INITIAL) && !ScreenCustomization_init_called_FancyMenu) {
            ScreenCustomization_init_called_FancyMenu = true;
            if (Services.PLATFORM.isOnClient()) {
                ScreenCustomization.init();
            }
        }
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
