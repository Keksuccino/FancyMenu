package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.widget.BrandingRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.MinecraftLogoRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.MinecraftSplashRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.RendererWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow @Final public static Component COPYRIGHT_TEXT;
    @Shadow @Final private static ResourceLocation MINECRAFT_LOGO;
    @Shadow @Final private static ResourceLocation MINECRAFT_EDITION;
    @Shadow @Nullable private RealmsNotificationsScreen realmsNotificationsScreen;
    @Shadow @Nullable private String splash;
    @Shadow public boolean fading;

    @Unique private int cached_mouseX_FancyMenu = -1;
    @Unique private int cached_mouseY_FancyMenu = -1;
    @Unique private float cached_partial_FancyMenu = -1f;

    @SuppressWarnings("all")
    private MixinTitleScreen() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void after_init_FancyMenu(CallbackInfo info) {
        this.children().forEach(guiEventListener -> {
            if (guiEventListener instanceof PlainTextButton button && button.getMessage() == COPYRIGHT_TEXT) {
                ((UniqueWidget)button).setWidgetIdentifierFancyMenu("title_screen_copyright_button");
            }
        });

        MinecraftLogoRenderer logo = MinecraftLogoRenderer.DEFAULT_INSTANCE;
        this.addRenderableWidget(new RendererWidget((this.width / 2) - (logo.getTotalWidth() / 2), 30, logo.getTotalWidth(), logo.getTotalHeight(),
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> logo.render(graphics, x, y, renderer.getAlpha())))
                .setWidgetIdentifierFancyMenu("minecraft_logo_widget")
                .setMessage(Component.translatable("fancymenu.widgetified_screens.title_screen.logo"));

        MinecraftSplashRenderer splash = MinecraftSplashRenderer.getDefaultInstance();
        this.addRenderableWidget(new RendererWidget(splash.getDefaultPositionX(this.width) - 50, splash.getDefaultPositionY() - 20, 100, 40,
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            int splashColor = Mth.ceil(renderer.getAlpha() * 255.0F) << 24;
                            splash.renderAt(graphics, x + (width / 2), y + (height / 2), Minecraft.getInstance().font, splashColor);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_splash_widget")
                .setMessage(Component.translatable("fancymenu.widgetified_screens.title_screen.splash"));

        BrandingRenderer branding = new BrandingRenderer(this.height);
        this.addRenderableWidget(new RendererWidget(branding.getDefaultPositionX(), branding.getDefaultPositionY(), branding.getTotalWidth(), branding.getTotalHeight(),
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            branding.setOpacity(renderer.getAlpha());
                            branding.render(graphics, x, y);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_branding_widget")
                .setMessage(Component.translatable("fancymenu.widgetified_screens.title_screen.branding"));

        this.realmsNotificationsScreen = null;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partial, CallbackInfo info) {
        this.cached_mouseX_FancyMenu = mouseX;
        this.cached_mouseY_FancyMenu = mouseY;
        this.cached_partial_FancyMenu = partial;
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            this.fading = false;
        }
    }

    /**
     * @reason Manually fire FancyMenu's {@link RenderedScreenBackgroundEvent} in {@link TitleScreen}, because normal event doesn't work correctly here.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void wrap_renderPanorama_FancyMenu(PanoramaRenderer instance, float deltaT, float alpha, Operation<Void> original) {
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((layer != null) && ScreenCustomization.isCustomizationEnabledForScreen(this) && !layer.layoutBase.menuBackgrounds.isEmpty()) {
            RenderSystem.enableBlend();
            graphics.fill(0, 0, this.width, this.height, 0);
            RenderingUtils.resetShaderColor(graphics);
        } else {
            this.renderCustomOrVanillaPanorama_FancyMenu(graphics, deltaT, alpha, instance, original);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, graphics, this.cached_mouseX_FancyMenu, this.cached_mouseY_FancyMenu, this.cached_partial_FancyMenu));
    }

    @Unique
    private void renderCustomOrVanillaPanorama_FancyMenu(GuiGraphics graphics, float partialTick, float alpha, PanoramaRenderer instance, Operation<Void> original) {
        LocalTexturePanoramaRenderer panorama = GlobalCustomizationHandler.getCustomBackgroundPanorama();
        if (panorama != null) {
            float previousOpacity = panorama.opacity;
            panorama.opacity = alpha;
            panorama.render(graphics, 0, 0, partialTick);
            panorama.opacity = previousOpacity;
            return;
        }
        original.call(instance, partialTick, alpha);
    }

    /**
     * @reason Cancel panorama overlay rendering when a custom background is active.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIFFIIII)V"))
    private boolean wrap_blit_in_render_FancyMenu(PoseStack pose, int i1, int i2, int i3, int i4, float v5, float v6, int i7, int i8, int i9, int textureHeight) {
        if (textureHeight == 128) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
            if (GlobalCustomizationHandler.getCustomBackgroundPanorama() != null) return false;
            if ((layer != null) && ScreenCustomization.isCustomizationEnabledForScreen(this) && !layer.layoutBase.menuBackgrounds.isEmpty()) return false;
        }
        return true;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
    private boolean cancel_VanillaRealmsNotificationRendering_FancyMenu(RealmsNotificationsScreen instance, PoseStack pose, int mouseX, int mouseY, float partial) {
        return false;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void cancel_VanillaLogoRendering_FancyMenu(int slot, ResourceLocation texture, Operation<Void> original) {
        if (texture == MINECRAFT_LOGO) texture = RenderingUtils.FULLY_TRANSPARENT_TEXTURE;
        if (texture == MINECRAFT_EDITION) texture = RenderingUtils.FULLY_TRANSPARENT_TEXTURE;
        original.call(slot, texture);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean cancel_VanillaSplashRendering_FancyMenu(PoseStack pose, Font font, String text, int x, int y, int color) {
        return !text.equals(this.splash);
    }

    /**
     * @reason This is to make the Title screen not constantly update the alpha of its widgets, so FancyMenu can properly handle it.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
    private boolean cancel_setAlpha_FancyMenu(AbstractWidget instance, float alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
