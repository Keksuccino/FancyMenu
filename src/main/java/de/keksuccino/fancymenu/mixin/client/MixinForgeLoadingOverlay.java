package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ForgeLoadingOverlay.class)
public abstract class MixinForgeLoadingOverlay {

	private static final Logger LOGGER = LogManager.getLogger();

	private static boolean firstScreenInit = true;
	private MenuHandlerBase menuHandler = null;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onConstructFancyMenu(Minecraft mc, ReloadInstance reloadInstance, Consumer<?> consumer, DisplayWindow displayWindow, CallbackInfo info) {
		//Preload animation frames to avoid lagging when rendering them for the first time
		if (FancyMenu.getConfig().getOrDefault("preloadanimations", true) && !AnimationHandler.preloadingCompleted()) {
			AnimationHandler.preloadAnimations(false);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void beforeRenderScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if ((this.menuHandler != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {
			//Manually call onRenderPre of the screen's menu handler, because it doesn't get called automatically in the loading screen
			this.menuHandler.onRenderPre(new ScreenEvent.Render.Pre(Minecraft.getInstance().screen, graphics, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
	private void afterRenderScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if ((this.menuHandler != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {
			//This is to correctly render the title menu
			if (this.menuHandler instanceof MainMenuHandler) {
				Minecraft.getInstance().screen.renderBackground(graphics);
			}
			//Manually call onRenderPost of the screen's menu handler, because it doesn't get called automatically in the loading screen
			this.menuHandler.onRenderPost(new ScreenEvent.Render.Post(Minecraft.getInstance().screen, graphics, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void afterInitScreenFancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if (Minecraft.getInstance().screen != null) {
			//Update resource pack animation sizes after reloading textures and when starting the game
			LOGGER.info("[FANCYMENU] Updating animation sizes..");
			AnimationHandler.updateAnimationSizes();
			//Cache the menu handler of the screen to be able to call some of its render events
			this.menuHandler = MenuHandlerRegistry.getHandlerFor(Minecraft.getInstance().screen);
			//If it's the first time a screen gets initialized, soft-reload the screen's handler, so first-time stuff works when fading to the Title menu
			if ((this.menuHandler != null) && firstScreenInit) {
				this.menuHandler.onSoftReload(new SoftMenuReloadEvent(Minecraft.getInstance().screen));
			}
			firstScreenInit = false;
			//Reset isNewMenu, so first-time stuff and on-load stuff works correctly, because the menu got initialized already (this is after screen init)
			MenuCustomization.setIsNewMenu(true);
			//Set the screen again to cover all customization init stages
			MenuCustomization.reInitCurrentScreen();
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
	private void beforeClosingOverlayFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
		if (Minecraft.getInstance().screen == null) {
			//Update resource pack animation sizes after reloading textures if fading to no screen (while in-game)
			LOGGER.info("[FANCYMENU] Updating animation sizes..");
			AnimationHandler.updateAnimationSizes();
		}
	}

}

