package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.Compat;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class MixinScreen implements CustomizableScreen {

	@Unique
	private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique
	private final List<GuiEventListener> removeOnInitChildrenFancyMenu = new ArrayList<>();

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
	private void afterInitFancyMenu(Minecraft $$0, int $$1, int $$2, CallbackInfo info) {
		Overlay overlay = Minecraft.getInstance().getOverlay();
		if (Compat.isRRLSLoaded() && (overlay != null) && Compat.isRRLSOverlay(overlay)) {
			LOGGER_FANCYMENU.info("[FANCYMENU] Re-initializing screen after init in overlay to fix incompatibility with RemoveReloadingScreen..");
			ScreenCustomization.reInitCurrentScreen();
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
	private void afterRenderBackgroundFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), graphics));
	}

	@Unique
	@Override
	public @NotNull List<GuiEventListener> removeOnInitChildrenFancyMenu() {
		return this.removeOnInitChildrenFancyMenu;
	}

}
