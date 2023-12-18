package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationDeepElement;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {

    @Shadow public boolean fading;
    @Unique boolean handleRealmsNotificationFancyMenu = false;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private void fireBackgroundRenderedEventAfterPanoramaOverlayFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), graphics));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private boolean cancelVanillaLogoRenderingFancyMenu(LogoRenderer instance, GuiGraphics $$0, int $$1, float $$2) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private boolean cancelVanillaSplashRenderingFancyMenu(SplashRenderer instance, GuiGraphics $$0, int $$1, Font $$2, int $$3) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRenderFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        this.handleRealmsNotificationFancyMenu = true;
        //Disable fading if customizations enabled, so FancyMenu can properly handle widget alpha
        if (ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this))) {
            this.fading = false;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRenderFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        this.handleRealmsNotificationFancyMenu = false;
    }

    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaRealmsNotificationIconRenderingFancyMenu(CallbackInfoReturnable<Boolean> info) {
        if (this.handleRealmsNotificationFancyMenu && ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this))) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen((Screen)((Object)this));
            if (layer != null) {
                AbstractElement e = layer.getElementByInstanceIdentifier("deep:" + DeepScreenCustomizationLayers.TITLE_SCREEN.realmsNotification.getIdentifier());
                if (e instanceof TitleScreenRealmsNotificationDeepElement d) {
                    if (d.isHidden()) {
                        info.setReturnValue(false);
                    }
                }
            }
        }
    }

    /**
     * @reason This is to make the Title screen not constantly update the alpha of its widgets, so FancyMenu can properly handle it.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
    private boolean wrapRenderAlphaFancyMenu(AbstractWidget instance, float alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen((Screen)((Object)this));
    }

}
