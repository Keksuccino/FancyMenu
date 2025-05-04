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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow @Final private static Component COPYRIGHT_TEXT;
    @Shadow public boolean fading;
    @Shadow private @Nullable RealmsNotificationsScreen realmsNotificationsScreen;

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
                .setMessage(Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.logo"));

        MinecraftSplashRenderer splash = MinecraftSplashRenderer.DEFAULT_INSTANCE;
        this.addRenderableWidget(new RendererWidget(splash.getDefaultPositionX(this.width) - 50, splash.getDefaultPositionY() - 20, 100, 40,
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            int splashColor = Mth.ceil(renderer.getAlpha() * 255.0F) << 24;
                            splash.renderAt(graphics, x + (width / 2), y + (height / 2), Minecraft.getInstance().font, splashColor);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_splash_widget")
                .setMessage(Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash"));

        if (this.realmsNotificationsScreen != null) {
            RealmsNotificationRenderer notifications = new RealmsNotificationRenderer(this.realmsNotificationsScreen, this.width, this.height);
            int totalWidth = notifications.getTotalWidth();
            if (totalWidth == 0) totalWidth = 50;
            this.addRenderableWidget(new RendererWidget(notifications.getDefaultPositionX(), notifications.getDefaultPositionY(), totalWidth, notifications.getTotalHeight(),
                            (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                                notifications.renderIcons(graphics, x, y, DrawableColor.WHITE.getColorIntWithAlpha(renderer.getAlpha()));
                            }))
                    .setWidgetIdentifierFancyMenu("minecraft_realms_notification_icons_widget")
                    .setMessage(Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.realmsnotification"));
        }

        BrandingRenderer branding = new BrandingRenderer(this.height);
        this.addRenderableWidget(new RendererWidget(branding.getDefaultPositionX(), branding.getDefaultPositionY() + 1, branding.getTotalWidth(), branding.getTotalHeight(),
                        (graphics, mouseX, mouseY, partial, x, y, width, height, renderer) -> {
                            branding.setOpacity(renderer.getAlpha());
                            branding.render(graphics, x, y);
                        }))
                .setWidgetIdentifierFancyMenu("minecraft_branding_widget")
                .setMessage(Component.translatable("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.branding"));

    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        //Disable fading if customizations enabled, so FancyMenu can properly handle widget alpha
        if (ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            this.fading = false;
        }
    }

    /**
     * @reason Manually fire FancyMenu's {@link RenderedScreenBackgroundEvent} in {@link TitleScreen}, because normal event doesn't work correctly here.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void wrap_renderPanorama_FancyMenu(TitleScreen instance, GuiGraphics graphics, float f, Operation<Void> original) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this)) {
            if (!l.layoutBase.menuBackgrounds.isEmpty()) {
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
    @WrapWithCondition(method = "fadeWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
    private boolean cancel_setAlpha_FancyMenu(AbstractWidget instance, float alpha) {
        return !ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
