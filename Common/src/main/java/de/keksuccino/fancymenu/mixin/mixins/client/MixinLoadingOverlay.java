package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.ScreenReloadEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

	private static final Logger LOGGER = LogManager.getLogger();

	private static boolean firstScreenInit = true;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onConstructFancyMenu(Minecraft mc, ReloadInstance reloadInstance, Consumer<?> consumer, boolean b, CallbackInfo info) {
		//Preload animation frames to avoid lagging when rendering them for the first time
		if (FancyMenu.getOptions().preLoadAnimations.getValue() && !AnimationHandler.preloadingCompleted()) {
			AnimationHandler.preloadAnimations(false);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void beforeRenderScreenFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPre event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(ScreenUtils.getScreen(), matrix, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = At.Shift.AFTER))
	private void afterRenderScreenFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPost event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(ScreenUtils.getScreen(), matrix, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void afterInitScreenFancyMenu(PoseStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if (Minecraft.getInstance().screen != null) {
			//Update resource pack animation sizes after reloading textures and when starting the game
			LOGGER.info("[FANCYMENU] Updating animation sizes..");
			AnimationHandler.updateAnimationSizes();
			//If it's the first time a screen gets initialized, soft-reload the screen's layer, so first-time stuff works when fading to the Title menu
			ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
			if ((layer != null) && firstScreenInit) {
				layer.resetLayer();
			}
			firstScreenInit = false;
			//Reset isNewMenu, so first-time stuff and on-load stuff works correctly, because the menu got initialized already (this is after screen init)
			ScreenCustomization.setIsNewMenu(true);
			//Re-init the screen to cover all customization init stages
			ScreenCustomization.reInitCurrentScreen();
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
	private void beforeClosingOverlayFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
		if (Minecraft.getInstance().screen == null) {
			//Update resource pack animation sizes after reloading textures if fading to no screen (while in-game)
			LOGGER.info("[FANCYMENU] Updating animation sizes..");
			AnimationHandler.updateAnimationSizes();
		}
	}

}

