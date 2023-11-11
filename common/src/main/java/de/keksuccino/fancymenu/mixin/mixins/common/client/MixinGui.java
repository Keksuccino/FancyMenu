package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

    @Unique private static final String DUMMY_RETURN_VALUE_FANCYMENU = "PREPARE RETURN VALUE";
    @Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

    @Unique private static boolean reloadListenerRegisteredFancyMenu = false;

    //This is a hacky way to get Minecraft to register FancyMenu's reload listener as early as possible in the Minecraft.class constructor
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructFancyMenu(Minecraft mc, ItemRenderer itemRenderer, CallbackInfo info) {
        if (!reloadListenerRegisteredFancyMenu) {
            LOGGER_FANCYMENU.info("[FANCYMENU] Registering resource reload listener..");
            reloadListenerRegisteredFancyMenu = true;
            if (mc.getResourceManager() instanceof ReloadableResourceManager r) {
                r.registerReloadListener(new SimplePreparableReloadListener<String>() {
                    @Override
                    protected @NotNull String prepare(@NotNull ResourceManager var1, @NotNull ProfilerFiller var2) {
                        return DUMMY_RETURN_VALUE_FANCYMENU;
                    }
                    @Override
                    protected void apply(@NotNull String prepareReturnValue, @NotNull ResourceManager var2, @NotNull ProfilerFiller var3) {
                        ResourceHandlers.reloadAll();
                    }
                });
            }
        }
    }

}
