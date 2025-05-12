package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow @Final private static Component COPYRIGHT_TEXT;
    @Shadow @Final private static ResourceLocation MINECRAFT_LOGO;
    @Shadow @Final private static ResourceLocation MINECRAFT_EDITION;

    @Shadow public boolean fading;
    @Shadow private String splash;

    //unused dummy constructor
    @SuppressWarnings("all")
    private MixinTitleScreen() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void after_init_FancyMenu(CallbackInfo info) {

        // Give the Copyright button and identifier
        this.children().forEach(guiEventListener -> {
            if (guiEventListener instanceof PlainTextButton b) {
                if (b.getMessage() == COPYRIGHT_TEXT) ((UniqueWidget)b).setWidgetIdentifierFancyMenu("title_screen_copyright_button");
            }
        });

        // Add widgets for all Title screen elements to make them editable

        MinecraftLogoRenderer logo = MinecraftLogoRenderer.DEFAULT_INSTANCE;
        this.addRenderableWidget(new RendererWidget((this.width / 2) - (logo.getTotalWidth() / 2), 30, logo.getTotalWidth(), logo.getTotalHeight(),
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            logo.render(GuiGraphics.currentGraphics(), x, y, renderer.getAlpha());
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_logo_widget")
                .setMessage(Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.logo"));

        MinecraftSplashRenderer splash = MinecraftSplashRenderer.getDefaultInstance();
        this.addRenderableWidget(new RendererWidget(splash.getDefaultPositionX(this.width) - 50, splash.getDefaultPositionY() - 20, 100, 40,
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            int splashColor = Mth.ceil(renderer.getAlpha() * 255.0F) << 24;
                            splash.renderAt(GuiGraphics.currentGraphics(), x + (width / 2), y + (height / 2), Minecraft.getInstance().font, splashColor);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_splash_widget")
                .setMessage(Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash"));

        BrandingRenderer branding = new BrandingRenderer(this.height);
        this.addRenderableWidget(new RendererWidget(branding.getDefaultPositionX(), branding.getDefaultPositionY(), branding.getTotalWidth(), branding.getTotalHeight(),
                        (pose, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            branding.setOpacity(renderer.getAlpha());
                            branding.render(GuiGraphics.currentGraphics(), x, y);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_branding_widget")
                .setMessage(Components.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.branding"));

    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(PoseStack graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
        //Disable fading if customizations enabled, so FancyMenu can properly handle widget alpha
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            this.fading = false;
        }
    }

    /**
     * @reason Manually fire FancyMenu's {@link RenderedScreenBackgroundEvent} in {@link TitleScreen}, because normal event doesn't work correctly here.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void wrap_renderPanorama_FancyMenu(PanoramaRenderer instance, float p_110004_, float p_110005_, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                GuiGraphics.currentGraphics().fill(0, 0, this.width, this.height, 0);
            } else {
                original.call(instance, p_110004_, p_110005_);
            }
        } else {
            original.call(instance, p_110004_, p_110005_);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, GuiGraphics.currentGraphics().pose()));
    }

    /**
     * @reason Cancel panorama overlay rendering when a custom background is active.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIFFIIII)V"))
    private boolean wrap_blit_in_render_FancyMenu(PoseStack pose, int i1, int i2, int i3, int i4, float v5, float v6, int i7, int i8, int i9, int i0) {
        if (i0 == 128) {
            ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
            if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
                if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
    private boolean cancel_VanillaRealmsNotificationRendering_FancyMenu(RealmsNotificationsScreen instance, PoseStack p_88837_, int p_88838_, int p_88839_, float p_88840_) {
        return false;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void cancel_VanillaLogoRendering_FancyMenu(int i, ResourceLocation texture, Operation<Void> original) {
        if (texture == MINECRAFT_LOGO) texture = RenderingUtils.FULLY_TRANSPARENT_TEXTURE;
        if (texture == MINECRAFT_EDITION) texture = RenderingUtils.FULLY_TRANSPARENT_TEXTURE;
        original.call(i, texture);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean cancel_VanillaSplashRendering_FancyMenu(PoseStack pose, Font font, String s, int i1, int i2, int i3) {
        if (s.equals(this.splash)) return false;
        return true;
    }

    /**
     * @reason This is to make the Title screen not constantly update the alpha of its widgets, so FancyMenu can properly handle it.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
    private boolean cancel_setAlpha_FancyMenu(AbstractWidget instance, float alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}