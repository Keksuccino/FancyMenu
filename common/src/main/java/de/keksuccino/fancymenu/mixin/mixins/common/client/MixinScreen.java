package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	@Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
	@Unique private boolean nextFocusPath_called_FancyMenu = false;

	@Shadow @Final private List<GuiEventListener> children;

    @WrapOperation(method = "renderWithTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void wrap_render_in_renderWithTooltip_FancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(instance, graphics, mouseX, mouseY, partial));
        original.call(instance, graphics, mouseX, mouseY, partial);
        EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(instance, graphics, mouseX, mouseY, partial));
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void return_renderWithTooltip_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
        RenderingUtils.executeAndClearDeferredScreenRenderingTasks(graphics, mouseX, mouseY, partial);
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void head_renderBlurredBackground_FancyMenu(float f, CallbackInfo info) {
        if (RenderingUtils.isMenuBlurringBlocked()) info.cancel();
    }

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void before_renderPanorama_FancyMenu(GuiGraphics graphics, float partial, CallbackInfo info) {
        LocalTexturePanoramaRenderer panorama = GlobalCustomizationHandler.getCustomBackgroundPanorama();
        if (panorama != null) {
            float previousOpacity = panorama.opacity;
            panorama.opacity = 1.0F;
            panorama.render(graphics, 0, 0, partial);
            panorama.opacity = previousOpacity;
            info.cancel();
        }
    }

    @Inject(method = "renderMenuBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private static void before_renderMenuBackgroundTexture_FancyMenu(GuiGraphics graphics, ResourceLocation location, int x, int y, float uOffset, float vOffset, int width, int height, CallbackInfo info) {
        RenderableResource customBackground = GlobalCustomizationHandler.getCustomMenuBackgroundTexture();
        if (customBackground == null) return;
        ResourceLocation customLocation = customBackground.getResourceLocation();
        if (customLocation == null) return;
        int textureWidth = customBackground.getWidth();
        int textureHeight = customBackground.getHeight();
        if (textureWidth <= 0 || textureHeight <= 0) return;
        RenderSystem.enableBlend();
        RenderingUtils.blitRepeat(graphics, customLocation, x, y, width, height, textureWidth, textureHeight);
        RenderingUtils.resetShaderColor(graphics);
        RenderSystem.disableBlend();
        info.cancel();
    }

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void wrap_renderBackground_in_render_FancyMenu(Screen instance, GuiGraphics graphics, int mouseX, int mouseY, float partial, Operation<Void> original) {
		//Don't fire the event in the TitleScreen, because it gets handled differently there
		if (instance instanceof TitleScreen) {
			original.call(instance, graphics, mouseX, mouseY, partial);
			return;
		}
		ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(instance);
		if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(instance)) {
			if (!l.layoutBase.menuBackgrounds.isEmpty()) {
				RenderSystem.enableBlend();
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
	private void beforeNextFocusPathInKeyPressedFancyMenu(int $$0, int $$1, int $$2, CallbackInfoReturnable<Boolean> info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;", shift = At.Shift.AFTER))
	private void afterNextFocusPathInKeyPressedFancyMenu(int $$0, int $$1, int $$2, CallbackInfoReturnable<Boolean> info) {
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

	@Unique
	@Override
	public @NotNull List<GuiEventListener> removeOnInitChildrenFancyMenu() {
		return this.removeOnInitChildrenFancyMenu;
	}

}
