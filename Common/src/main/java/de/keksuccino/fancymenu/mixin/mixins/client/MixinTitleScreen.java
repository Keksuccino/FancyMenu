package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationDeepElement;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Unique boolean handleRealmsNotification = false;

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

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRenderFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo info) {
        this.handleRealmsNotification = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRenderFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo info) {
        this.handleRealmsNotification = false;
    }

    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaRealmsNotificationIconRenderingFancyMenu(CallbackInfoReturnable<Boolean> info) {
        if (this.handleRealmsNotification && ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this))) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen((Screen)((Object)this));
            if (layer != null) {
                AbstractElement e = layer.getElementByInstanceIdentifier("deep:" + DeepScreenCustomizationLayers.TITLE_SCREEN.realmsNotification.getIdentifier());
                if (e instanceof TitleScreenRealmsNotificationDeepElement d) {
                    if (d.isHidden()) {
                        //TODO remove debug
                        LogManager.getLogger().info("############# NOTIFICATIONS HIDDEN");
                        info.setReturnValue(false);
                    }
                }
            }
        }
    }

}
