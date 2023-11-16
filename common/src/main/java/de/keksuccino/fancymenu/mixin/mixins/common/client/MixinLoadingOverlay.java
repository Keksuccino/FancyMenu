package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(LoadingOverlay.class)
public abstract class MixinLoadingOverlay {

	@Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique private static boolean firstScreenInitFancyMenu = true;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onConstructFancyMenu(Minecraft mc, ReloadInstance reloadInstance, Consumer<?> consumer, boolean b, CallbackInfo info) {
		//Preload animation frames to avoid lagging when rendering them for the first time
		if (FancyMenu.getOptions().preLoadAnimations.getValue() && !AnimationHandler.preloadingCompleted()) {
			AnimationHandler.preloadAnimations(false);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
	private void beforeRenderScreenFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPre event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Pre(ScreenUtils.getScreen(), pose, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = At.Shift.AFTER))
	private void afterRenderScreenFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire RenderPost event for current screen in loading overlay
		if (ScreenUtils.getScreen() != null) {
			EventHandler.INSTANCE.postEvent(new RenderScreenEvent.Post(ScreenUtils.getScreen(), pose, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"))
	private void beforeInitScreenFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {

		boolean isPlayingIntro = false;

		if (!GameIntroHandler.introPlayed && GameIntroHandler.shouldPlayIntro()) {
			IAnimationRenderer animationRenderer = GameIntroHandler.getGameIntroAnimation();
			if (animationRenderer != null) {
				isPlayingIntro = true;
				Minecraft.getInstance().setScreen(new GameIntroScreen((Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen(), animationRenderer));
			}
		}
		GameIntroHandler.introPlayed = true;

		//Fire Pre Screen Init events, because they normally don't get fired in the loading overlay
		if (!isPlayingIntro) {
			RenderingUtils.resetGuiScale();
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(Minecraft.getInstance().screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen));
		}

	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void afterInitScreenFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
		//Fire Post Screen Init events, because they normally don't get fired in the loading overlay
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen));
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(Minecraft.getInstance().screen));
	}

//	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
//	private void afterInitScreenFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
//		if (Minecraft.getInstance().screen != null) {
//			//Update resource pack animation sizes after reloading textures and when starting the game
//			LOGGER_FANCYMENU.info("[FANCYMENU] Updating animation sizes..");
//			AnimationHandler.updateAnimationSizes();
//			//If it's the first time a screen gets initialized, soft-reload the screen's layer, so first-time stuff works when fading to the Title menu
//			ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getLayerOfScreen(Minecraft.getInstance().screen);
//			if ((layer != null) && firstScreenInitFancyMenu) layer.resetLayer();
//			firstScreenInitFancyMenu = false;
//			//Reset isNewMenu, so first-time stuff and on-load stuff works correctly, because the menu got initialized already (this is after screen init)
//			ScreenCustomization.setIsNewMenu(true);
//			//Re-init the screen to cover all customization init stages
//			ScreenCustomization.reInitCurrentScreen();
//		}
//	}

//	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
//	private void beforeClosingOverlayFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
//		if (Minecraft.getInstance().screen == null) {
//			//Update resource pack animation sizes after reloading textures if fading to no screen (while in-game)
//			LOGGER_FANCYMENU.info("[FANCYMENU] Updating animation sizes..");
//			AnimationHandler.updateAnimationSizes();
//		}
//	}

}

