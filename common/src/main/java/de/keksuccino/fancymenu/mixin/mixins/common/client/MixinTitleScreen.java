package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationDeepElement;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow @Final private static Component COPYRIGHT_TEXT;
    @Shadow public boolean fading;

    @Unique boolean handleRealmsNotificationFancyMenu = false;

    //unused dummy constructor
    @SuppressWarnings("all")
    private MixinTitleScreen() {
        super(null);
    }

    /**
     * @reason Add an identifier to the Copyright button.
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void at_return_of_screen_init_FancyMenu(CallbackInfo info) {

        this.children().forEach(guiEventListener -> {
            if (guiEventListener instanceof PlainTextButton b) {
                if (b.getMessage() == COPYRIGHT_TEXT) ((UniqueWidget)b).setWidgetIdentifierFancyMenu("title_screen_copyright_button");
            }
        });

    }

    /**
     * @reason Manually fire FancyMenu's {@link RenderedScreenBackgroundEvent} in {@link TitleScreen}, because normal event doesn't work correctly here.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void wrap_renderPanorama_in_render_FancyMenu(TitleScreen instance, GuiGraphics graphics, float f, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            if (l.layoutBase.menuBackground != null) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                graphics.fill(RenderType.guiOverlay(), 0, 0, this.width, this.height, 0);
            } else {
                original.call(instance, graphics, f);
            }
        } else {
            original.call(instance, graphics, f);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, graphics));
    }

    /**
     * @reason Makes FancyMenu not fire its {@link RenderedScreenBackgroundEvent} in {@link TitleScreen} when calling {@link Screen#render(GuiGraphics, int, int, float)}, because it would get fired too late.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private boolean wrap_super_render_in_render_FancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        ((IMixinScreen)this).getRenderablesFancyMenu().forEach(renderable -> renderable.render(graphics, mouseX, mouseY, partial));
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private boolean cancelVanillaLogoRenderingFancyMenu(LogoRenderer instance, GuiGraphics $$0, int $$1, float $$2) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private boolean cancelVanillaSplashRenderingFancyMenu(SplashRenderer instance, GuiGraphics $$0, int $$1, Font $$2, int $$3) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRenderFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        this.handleRealmsNotificationFancyMenu = true;
        //Disable fading if customizations enabled, so FancyMenu can properly handle widget alpha
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            this.fading = false;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRenderFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        this.handleRealmsNotificationFancyMenu = false;
    }

    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaRealmsNotificationIconRenderingFancyMenu(CallbackInfoReturnable<Boolean> info) {
        if (this.handleRealmsNotificationFancyMenu && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
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
    @WrapWithCondition(method = "fadeWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
    private boolean wrapRenderAlphaFancyMenu(AbstractWidget instance, float alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
