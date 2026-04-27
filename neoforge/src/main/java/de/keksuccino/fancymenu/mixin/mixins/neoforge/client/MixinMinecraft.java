package de.keksuccino.fancymenu.mixin.mixins.neoforge.client;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    /**
     * @reason NeoForge initializes mods before the client resource infrastructure exists, so FancyMenu
     * initializes once Minecraft is ready enough but before GameLoadCookie starts the customization engine.
     */
    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;initClientHooks(Lnet/minecraft/client/Minecraft;Lnet/minecraft/server/packs/resources/ReloadableResourceManager;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void after_initClientHooks_NeoForge_FancyMenu(GameConfig gameConfig, CallbackInfo info) {

        FancyMenu.init();

    }

}
