package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
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

	@Unique
	private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique
	private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();
	@Unique
	private boolean nextFocusPath_called_FancyMenu = false;

	@Shadow @Final private List<GuiEventListener> children;

	@WrapOperation(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V"))
	private void wrap_fillGradient_in_renderBackground_FancyMenu(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, Operation<Void> original) {
		ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen((Screen)((Object)this));
		if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this.getScreen_FancyMenu())) {
			if (l.layoutBase.menuBackground != null) {
				RenderSystem.enableBlend();
				//Render a black background before the custom background gets rendered
				graphics.fill(0, 0, this.getScreen_FancyMenu().width, this.getScreen_FancyMenu().height, 0);
				RenderingUtils.resetShaderColor(graphics);
			} else {
				original.call(graphics, x1, y1, x2, y2, colorFrom, colorTo);
			}
		} else {
			original.call(graphics, x1, y1, x2, y2, colorFrom, colorTo);
		}
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this.getScreen_FancyMenu(), graphics));
	}

	@WrapOperation(method = "renderDirtBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V"))
	private void wrap_blit_in_renderDirtBackground_FancyMenu(GuiGraphics instance, ResourceLocation atlasLocation, int x, int y, int blitOffset, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, Operation<Void> original) {
		ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen((Screen)((Object)this));
		if ((l != null) && ScreenCustomization.isCustomizationEnabledForScreen(this.getScreen_FancyMenu())) {
			if (l.layoutBase.menuBackground != null) {
				RenderSystem.enableBlend();
				//Render a black background before the custom background gets rendered
				instance.fill(0, 0, this.getScreen_FancyMenu().width, this.getScreen_FancyMenu().height, 0);
				RenderingUtils.resetShaderColor(instance);
			} else {
				original.call(instance, atlasLocation, x, y, blitOffset, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
			}
		} else {
			original.call(instance, atlasLocation, x, y, blitOffset, uOffset, vOffset, uWidth, vHeight, textureWidth, textureHeight);
		}
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this.getScreen_FancyMenu(), instance));
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;"))
	private void beforeNextFocusPathInKeyPressedFancyMenu(int $$0, int $$1, int $$2, CallbackInfoReturnable<Boolean> info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;", shift = At.Shift.AFTER))
	private void afterNextFocusPathInKeyPressedFancyMenu(int $$0, int $$1, int $$2, CallbackInfoReturnable<Boolean> info) {
		this.nextFocusPath_called_FancyMenu = false;
	}

	@Inject(method = "setInitialFocus", at = @At("HEAD"))
	private void beforeSetInitialFocusFancyMenu(GuiEventListener $$0, CallbackInfo info) {
		this.nextFocusPath_called_FancyMenu = true;
	}

	@Inject(method = "setInitialFocus", at = @At("RETURN"))
	private void afterSetInitialFocusFancyMenu(GuiEventListener $$0, CallbackInfo info) {
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

	@Unique
	private Screen getScreen_FancyMenu() {
		return (Screen)((Object)this);
	}

}
