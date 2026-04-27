package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlStateManager;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class MixinScreen implements CustomizableScreen {

    @Unique private static final Identifier DIRT_TEXTURE_FANCYMENU = Identifier.withDefaultNamespace("textures/block/dirt.png");

	@Unique private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
	@Unique private boolean nextFocusPath_called_FancyMenu = false;

	@Shadow @Final private List<GuiEventListener> children;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void wrap_extractRenderState_in_extractRenderStateWithTooltipAndSubtitles_FancyMenu(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(instance, graphics, mouseX, mouseY, partial));
        original.call(instance, graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(instance, graphics, mouseX, mouseY, partial));
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void return_extractRenderStateWithTooltipAndSubtitles_FancyMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        RenderingUtils.executeAndClearDeferredScreenRenderingTasks(graphics, mouseX, mouseY, partial);
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void head_extractBlurredBackground_FancyMenu(GuiGraphicsExtractor graphics, CallbackInfo info) {
        if (RenderingUtils.isVanillaMenuBlurringBlocked()) info.cancel();
    }

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void before_extractPanorama_FancyMenu(GuiGraphicsExtractor graphics, float partial, CallbackInfo info) {
        LocalTexturePanoramaRenderer panorama = GlobalCustomizationHandler.getCustomBackgroundPanorama();
        if (panorama != null) {
            float previousOpacity = panorama.opacity;
            panorama.opacity = 1.0F;
            panorama.extractRenderState(graphics, 0, 0, partial);
            panorama.opacity = previousOpacity;
            info.cancel();
        }
    }

    @WrapOperation(method = "extractPanorama", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Panorama;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZ)V"))
    private void wrap_panorama_rendering_in_extractPanorama_FancyMenu(Panorama instance, GuiGraphicsExtractor graphics, int width, int height, boolean spin, Operation<Void> original) {
        if (PiPWindowHandler.INSTANCE.isScreenRenderActive()) {
            // This forces a normal background texture for PiP window screens
            RenderingUtils.setShaderColor(graphics, 0.5F, 0.5F, 0.5F, 1.0F);
            GlStateManager._enableBlend();
            Screen.extractMenuBackgroundTexture(graphics, DIRT_TEXTURE_FANCYMENU, 0, 0, 0.0F, 0.0F, this.width, this.height);
            GlStateManager._disableBlend();
            RenderingUtils.resetShaderColor(graphics);
        } else {
            original.call(instance, graphics, width, height, spin);
        }
    }

    @Inject(method = "extractMenuBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private static void before_extractMenuBackgroundTexture_FancyMenu(GuiGraphicsExtractor graphics, Identifier location, int x, int y, float uOffset, float vOffset, int width, int height, CallbackInfo info) {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (SeamlessWorldLoadingHandler.renderLoadingBackgroundIfActive(graphics, x, y, width, height, currentScreen)) {
            info.cancel();
            return;
        }
        RenderableResource customBackground = GlobalCustomizationHandler.getCustomMenuBackgroundTexture();
        if (customBackground == null) return;
        Identifier customLocation = customBackground.getResourceLocation();
        if (customLocation == null) return;
        int textureWidth = customBackground.getWidth();
        int textureHeight = customBackground.getHeight();
        if (textureWidth <= 0 || textureHeight <= 0) return;
        GlStateManager._enableBlend();
        RenderingUtils.blitRepeat(graphics, customLocation, x, y, width, height, textureWidth, textureHeight);
        RenderingUtils.resetShaderColor(graphics);
        GlStateManager._disableBlend();
        info.cancel();
    }

		@WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
		private void wrap_extractBackground_in_extractRenderStateWithTooltipAndSubtitles_FancyMenu(Screen instance, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
		//Don't fire the event in the TitleScreen, because it gets handled differently there
		if (instance instanceof TitleScreen) {
			original.call(instance, graphics, mouseX, mouseY, partial);
			return;
		}
		ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
		if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance)) {
			if (!l.layoutBase.menuBackgrounds.isEmpty()) {
					GlStateManager._enableBlend();
				//Render a black background before the custom background gets rendered
				graphics.fill(0, 0, instance.width, instance.height, 0);
				RenderingUtils.resetShaderColor(graphics);
			} else {
				original.call(instance, graphics, mouseX, mouseY, partial);
			}
		} else {
			original.call(instance, graphics, mouseX, mouseY, partial);
		}
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(instance, graphics, mouseX, mouseY, partial));
	}

		@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;"))
		private void beforeNextFocusPathInKeyPressedFancyMenu(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
			this.nextFocusPath_called_FancyMenu = true;
		}

		@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;", shift = At.Shift.AFTER))
		private void afterNextFocusPathInKeyPressedFancyMenu(KeyEvent event, CallbackInfoReturnable<Boolean> info) {
			this.nextFocusPath_called_FancyMenu = false;
		}

	@Inject(method = "setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("HEAD"))
	private void beforeSetInitialFocusFancyMenu(GuiEventListener $$0, CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "setInitialFocus(Lnet/minecraft/client/gui/components/events/GuiEventListener;)V", at = @At("RETURN"))
	private void afterSetInitialFocusFancyMenu(GuiEventListener $$0, CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = false;
	}

	@Inject(method = "setInitialFocus()V", at = @At("HEAD"))
	private void beforeSetInitialFocus_2_FancyMenu(CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "setInitialFocus()V", at = @At("RETURN"))
	private void afterSetInitialFocus_2_FancyMenu(CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = false;
	}

	@Inject(method = "children", at = @At("RETURN"), cancellable = true)
	private void atReturnChildrenFancyMenu(CallbackInfoReturnable<List<? extends GuiEventListener>> info) {
		if (this.nextFocusPath_called_FancyMenu) {
			List<GuiEventListener> filtered = new ArrayList<>(this.children);
			filtered.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && (!n.isFocusable() || !n.isNavigatable()));
			info.setReturnValue(filtered);
		}
	}

	/**
	 * @reason This is to make the Title screen not constantly update the alpha of its widgets, so FancyMenu can properly handle it.
	 */
	@WrapWithCondition(method = "fadeWidgets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;setAlpha(F)V"))
	private boolean cancel_setAlpha_FancyMenu(AbstractWidget instance, float alpha) {
		if (((Object)this) instanceof TitleScreen s) {
			return !ScreenCustomization.isCustomizationEnabledForScreen(s);
		}
		return true;
	}

	@Unique
	@Override
	public @NotNull List<GuiEventListener> removeOnInitChildrenFancyMenu() {
		return this.removeOnInitChildrenFancyMenu;
	}

}
