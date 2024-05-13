package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.deep.layers.DeepScreenCustomizationLayers;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.realmsnotification.TitleScreenRealmsNotificationDeepElement;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.BiConsumer;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow public boolean fading;

    @Unique boolean handleRealmsNotificationFancyMenu = false;
    @Unique PoseStack cachedGraphics_FancyMenu = null;
    @Unique boolean shouldRenderVanillaBackground_FancyMenu = true;

    //unused dummy constructor
    private MixinTitleScreen() {
        super(Components.empty());
    }

    /**
     * @reason Cache {@link PoseStack} instance for later use.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void head_render_FancyMenu(PoseStack pose, int $$1, int $$2, float $$3, CallbackInfo info) {
        this.shouldRenderVanillaBackground_FancyMenu = true;
        this.cachedGraphics_FancyMenu = pose;
    }

    /**
     * @reason Manually fire FancyMenu's {@link RenderedScreenBackgroundEvent} in {@link TitleScreen}, because normal event doesn't work correctly here.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void wrap_PanoramaRenderer_render_in_render_FancyMenu(PanoramaRenderer instance, float deltaT, float alpha, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this) && (this.cachedGraphics_FancyMenu != null)) {
            if (l.layoutBase.menuBackground != null) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                this.fill(this.cachedGraphics_FancyMenu, 0, 0, this.width, this.height, 0);
                RenderingUtils.resetShaderColor();
                this.shouldRenderVanillaBackground_FancyMenu = false;
            } else {
                original.call(instance, deltaT, alpha);
            }
        } else {
            original.call(instance, deltaT, alpha);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, this.cachedGraphics_FancyMenu));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIFFIIII)V"))
    private boolean wrap_blit_in_render_FancyMenu(PoseStack poseStack, int i1, int i2, int i3, int i4, float v5, float v6, int i7, int i8, int i9, int i0) {
        return this.shouldRenderVanillaBackground_FancyMenu;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;blitOutlineBlack(IILjava/util/function/BiConsumer;)V"))
    private boolean cancelVanillaLogoRenderingFancyMenu(TitleScreen instance, int i1, int i2, BiConsumer biConsumer) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIFFIIII)V"))
    private boolean cancelVanillaLogoEditionRenderingFancyMenu(PoseStack poseStack, int i1, int i2, float v3, float v4, int i5, int i6, int i7, int i8) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean cancelVanillaSplashRenderingFancyMenu(PoseStack poseStack, Font font, String s, int i1, int i2, int i3) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRenderFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo info) {
        this.handleRealmsNotificationFancyMenu = true;
        //Disable fading if customizations enabled, so FancyMenu can properly handle widget alpha
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            this.fading = false;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void afterRenderFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo info) {
        this.handleRealmsNotificationFancyMenu = false;
    }

    @Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaRealmsNotificationIconRenderingFancyMenu(CallbackInfoReturnable<Boolean> info) {
        if (this.handleRealmsNotificationFancyMenu && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
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
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}