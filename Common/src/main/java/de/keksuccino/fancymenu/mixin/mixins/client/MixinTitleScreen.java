package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.events.screen.RenderedScreenBackgroundEvent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lcom/mojang/blaze3d/vertex/PoseStack;IF)V"))
    private void fireBackgroundRenderedEventAfterPanoramaOverlayFancyMenu(PoseStack pose, int $$1, int $$2, float $$3, CallbackInfo info) {
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), pose));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lcom/mojang/blaze3d/vertex/PoseStack;IF)V"))
    private boolean cancelVanillaLogoRenderingFancyMenu(LogoRenderer instance, PoseStack poseStack, int i, float f) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean cancelVanillaSplashRenderingFancyMenu(PoseStack poseStack, Font font, String s, int i1, int i2, int i3) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaRealmsNotificationIconRenderingFancyMenu(CallbackInfoReturnable<Boolean> info) {
        if (ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this))) {
            info.setReturnValue(false);
        }
    }

}
