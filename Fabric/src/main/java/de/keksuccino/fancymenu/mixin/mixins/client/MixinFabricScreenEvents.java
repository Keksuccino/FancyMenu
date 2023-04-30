package de.keksuccino.fancymenu.mixin.mixins.client;

import de.keksuccino.fancymenu.mixin.MixinCache;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ScreenEvents.class)
public class MixinFabricScreenEvents {

    //This is needed because I use a cancelable screen render event and canceling
    //the event caused afterRender to receive a nulled screen object
    @ModifyArg(method = "afterRender", at = @At(value = "INVOKE", target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"), index = 0)
    private static <T> T fixNullScreenInAfterRenderFancyMenu1(T obj) {
        if (obj == null) {
            return (T) ((MixinCache.currentRenderScreen != null) ? MixinCache.currentRenderScreen : Minecraft.getInstance().screen);
        }
        return obj;
    }

    //This is needed because I use a cancelable screen render event and canceling
    //the event caused afterRender to receive a nulled screen object
    @ModifyArg(method = "afterRender", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/client/screen/ScreenExtensions;getExtensions(Lnet/minecraft/client/gui/screens/Screen;)Lnet/fabricmc/fabric/impl/client/screen/ScreenExtensions;"), index = 0)
    private static Screen fixNullScreenInAfterRenderFancyMenu2(Screen screen) {
        if (screen == null) {
            return ((MixinCache.currentRenderScreen != null) ? MixinCache.currentRenderScreen : Minecraft.getInstance().screen);
        }
        return screen;
    }

}
