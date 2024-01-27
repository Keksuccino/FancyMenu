package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.resources.IAsyncReloader;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.ResourceLoadProgressGui;

import java.util.function.Consumer;

@Mixin(value = ResourceLoadProgressGui.class)
public abstract class MixinResourceLoadProgressGui extends AbstractGui {

	private static final Logger LOGGER = LogManager.getLogger();

	private static boolean animationsLoaded = false;
	private static boolean firstScreenInit = true;
	private MenuHandlerBase menuHandler = null;

	@Inject(method = "<init>", at = @At(value = "RETURN"))
	private void onConstructFancyMenu(Minecraft mc, IAsyncReloader reloadInstance, Consumer consumer, boolean b, CallbackInfo info) {
		if (!animationsLoaded) {
			FancyMenu.initConfig();
			animationsLoaded = true;
			LOGGER.info("[FANCYMENU] Pre-loading animations if enabled in config..");
			AnimationHandler.preloadAnimations();
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V"))
	private void beforeRenderScreenFancyMenu(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if ((Minecraft.getInstance().screen != null) && (this.menuHandler != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {
			//Manually call onRenderPre of the screen's menu handler, because it doesn't get called automatically in the loading screen
			this.menuHandler.onRenderPre(new GuiScreenEvent.DrawScreenEvent.Pre(Minecraft.getInstance().screen, matrix, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V", shift = At.Shift.AFTER))
	private void afterRenderScreenFancyMenu(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if ((Minecraft.getInstance().screen != null) && (this.menuHandler != null) && MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {
			//This is to correctly render the title menu
			if (this.menuHandler instanceof MainMenuHandler) {
				Minecraft.getInstance().screen.renderBackground(matrix);
			}
			//Manually call onRenderPost of the screen's menu handler, because it doesn't get called automatically in the loading screen
			this.menuHandler.onRenderPost(new GuiScreenEvent.DrawScreenEvent.Post(Minecraft.getInstance().screen, matrix, mouseX, mouseY, partial));
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void afterInitScreenFancyMenu(MatrixStack matrix, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if (Minecraft.getInstance().screen != null) {
			//Enable animation engine and customization engine before screen init to not block the customization engine
			AnimationHandler.setReady(true);
			MenuCustomization.allowScreenCustomization = true;
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
			Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
		}
	}

}

