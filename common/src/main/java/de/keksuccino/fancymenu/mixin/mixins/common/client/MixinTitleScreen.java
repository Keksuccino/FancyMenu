package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow @Final public static Component COPYRIGHT_TEXT;
    @Shadow @Final private static ResourceLocation PANORAMA_OVERLAY;
    @Shadow @Nullable private RealmsNotificationsScreen realmsNotificationsScreen;
    @Shadow public boolean fading;

    @Unique private GuiGraphics cached_graphics_FancyMenu = null;
    @Unique private int cached_mouseX_FancyMenu = -1;
    @Unique private int cached_mouseY_FancyMenu = -1;
    @Unique private float cached_partial_FancyMenu = -1f;

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
        this.addRenderableWidget(new RendererWidget((this.width / 2) - (logo.getWidth() / 2), 30, logo.getWidth(), logo.getHeight(),
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            logo.renderLogoAtPosition(graphics, x, y, renderer.getAlpha());
                        }))
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

        if (this.realmsNotificationsScreen != null) {
            RealmsNotificationRenderer notifications = new RealmsNotificationRenderer(this.realmsNotificationsScreen, this.width, this.height);
            int totalWidth = notifications.getTotalWidth();
            if (totalWidth == 0) totalWidth = 50;
            this.addRenderableWidget(new RendererWidget(notifications.getDefaultPositionX(), notifications.getDefaultPositionY(), totalWidth, notifications.getTotalHeight(),
                            (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                                notifications.renderIcons(graphics, x, y, DrawableColor.WHITE.getColorIntWithAlpha(renderer.getAlpha()));
                            }))
                    .setWidgetIdentifierFancyMenu("minecraft_realms_notification_icons_widget")
                    .setMessage(Component.translatable("fancymenu.widgetified_screens.title_screen.realmsnotification"));
        }

        BrandingRenderer branding = new BrandingRenderer(this.height);
        this.addRenderableWidget(new RendererWidget(branding.getDefaultPositionX(), branding.getDefaultPositionY() + 1, branding.getTotalWidth(), branding.getTotalHeight(),
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            branding.setOpacity(renderer.getAlpha());
                            branding.render(graphics, x, y);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_branding_widget")
                .setMessage(Component.translatable("fancymenu.widgetified_screens.title_screen.branding"));

    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        this.cached_mouseX_FancyMenu = mouseX;
        this.cached_mouseY_FancyMenu = mouseY;
        this.cached_partial_FancyMenu = partial;
        //Disable fading if customizations enabled, so FancyMenu can properly handle widget alpha
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            this.fading = false;
        }
        this.cached_graphics_FancyMenu = graphics;
    }

    /**
     * @reason Manually fire FancyMenu's {@link RenderedScreenBackgroundEvent} in {@link TitleScreen}, because normal event doesn't work correctly here.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    private void wrap_renderPanorama_FancyMenu(PanoramaRenderer instance, float deltaT, float alpha, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                this.cached_graphics_FancyMenu.fill(RenderType.guiOverlay(), 0, 0, this.width, this.height, 0);
            } else {
                original.call(instance, deltaT, alpha);
            }
        } else {
            original.call(instance, deltaT, alpha);
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, this.cached_graphics_FancyMenu, this.cached_mouseX_FancyMenu, this.cached_mouseY_FancyMenu, this.cached_partial_FancyMenu));
    }

    /**
     * @reason Cancel panorama overlay rendering when a custom background is active.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private boolean wrap_blit_in_render_FancyMenu(GuiGraphics instance, ResourceLocation location, int p_283671_, int p_282377_, int p_282058_, int p_281939_, float p_282285_, float p_283199_, int p_282186_, int p_282322_, int p_282481_, int p_281887_) {
        if (location == PANORAMA_OVERLAY) {
            ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
            if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
                if (!l.layoutBase.menuBackgrounds.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private boolean cancel_VanillaRealmsNotificationRendering_FancyMenu(RealmsNotificationsScreen instance, GuiGraphics $$0, int $$1, int $$2, float $$3) {
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private boolean cancel_VanillaLogoRendering_FancyMenu(LogoRenderer instance, GuiGraphics $$0, int $$1, float $$2) {
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private boolean cancel_VanillaSplashRendering_FancyMenu(SplashRenderer instance, GuiGraphics $$0, int $$1, Font $$2, int $$3) {
        return false;
    }

    /**
     * @reason This is to make the Title screen not constantly update the alpha of its widgets, so FancyMenu can properly handle it.
     */
    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
    private boolean cancel_setAlpha_FancyMenu(AbstractWidget instance, float alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
