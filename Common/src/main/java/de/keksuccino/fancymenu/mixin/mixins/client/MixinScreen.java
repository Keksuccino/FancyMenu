package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.Compat;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {

	@Unique private static final Logger LOGGER = LogManager.getLogger();

	@Inject(method = "renderBackground", at = @At(value = "RETURN"))
	private void afterRenderScreenBackgroundFancyMenu(PoseStack matrix, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent((Screen)((Object)this), matrix));
	}

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
	private void afterInitFancyMenu(Minecraft $$0, int $$1, int $$2, CallbackInfo ci) {
		Overlay overlay = Minecraft.getInstance().getOverlay();
		if (Compat.isRRLSLoaded() && (overlay != null) && Compat.isRRLSOverlay(overlay)) {
			LOGGER.info("[FANCYMENU] Re-initializing screen after init in overlay to fix incompatibility with RemoveReloadingScreen..");
			ScreenCustomization.reInitCurrentScreen();
		}
	}

}
