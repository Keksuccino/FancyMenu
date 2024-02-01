package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.Compat;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
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

	//TODO übernehmen
	@Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V", shift = At.Shift.AFTER))
	private void afterFillGradientInRenderScreenBackgroundFancyMenu(GuiGraphics graphics, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), graphics));
	}

	//TODO übernehmen
	@Inject(method = "renderDirtBackground", at = @At(value = "RETURN"))
	private void afterRenderDirtBackgroundFancyMenu(GuiGraphics graphics, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), graphics));
	}

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
	private void afterInitFancyMenu(Minecraft $$0, int $$1, int $$2, CallbackInfo info) {
		Overlay overlay = Minecraft.getInstance().getOverlay();
		if (Compat.isRRLSLoaded() && (overlay != null) && Compat.isRRLSOverlay(overlay)) {
			LOGGER_FANCYMENU.info("[FANCYMENU] Re-initializing screen after init in overlay to fix incompatibility with RemoveReloadingScreen..");
			ScreenCustomization.reInitCurrentScreen();
		}
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

}
