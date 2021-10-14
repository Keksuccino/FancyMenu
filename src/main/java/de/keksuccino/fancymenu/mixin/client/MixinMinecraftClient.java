package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.animation.ResourcePackAnimationRenderer;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MinecraftClient.class)
public class MixinMinecraftClient {
	
	private static boolean customWindowInit = false;

	private static boolean animationsLoaded = false;

	@Inject(at = @At(value = "HEAD"), method = "getWindowTitle", cancellable = true)
	public void onGetWindowTitle(CallbackInfoReturnable<String> info) {
		
		if ((FancyMenu.config != null) && (MinecraftClient.getInstance().getWindow() != null)) {
			if (!customWindowInit) {
				MainWindowHandler.init();
				MainWindowHandler.updateWindowIcon();
				MainWindowHandler.updateWindowTitle();
				customWindowInit = true;
			}
		}
		
		String title = MainWindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "setOverlay")
	private void onSetOverlay(Overlay overlay, CallbackInfo info) {
		if (FancyMenu.config == null) {
			return;
		}
		if (overlay == null) {
			preloadAnimations();
			MixinCache.isSplashScreenRendering = false;
			MenuCustomization.isLoadingScreen = false;
			MenuCustomization.reloadCurrentMenu();
		} else {
			MenuCustomization.isLoadingScreen = true;
		}
	}

	private static void preloadAnimations() {

		System.out.println("[FANCYMENU] Updating animation sizes..");
		AnimationHandler.setupAnimationSizes();

		//Pre-load animation frames to prevent them from lagging when rendered for the first time
		if (FancyMenu.config.getOrDefault("preloadanimations", true)) {
			if (!animationsLoaded) {
				System.out.println("[FANCYMENU] LOADING ANIMATION TEXTURES! THIS CAUSES THE LOADING SCREEN TO FREEZE FOR A WHILE!");
				try {
					List<ResourcePackAnimationRenderer> l = new ArrayList<ResourcePackAnimationRenderer>();
					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
						if (r instanceof AdvancedAnimation) {
							IAnimationRenderer main = ((AdvancedAnimation) r).getMainAnimationRenderer();
							IAnimationRenderer intro = ((AdvancedAnimation) r).getIntroAnimationRenderer();
							if ((main != null) && (main instanceof ResourcePackAnimationRenderer)) {
								l.add((ResourcePackAnimationRenderer) main);
							}
							if ((intro != null) && (intro instanceof  ResourcePackAnimationRenderer)) {
								l.add((ResourcePackAnimationRenderer) intro);
							}
						} else if (r instanceof ResourcePackAnimationRenderer) {
							l.add((ResourcePackAnimationRenderer) r);
						}
					}
					for (ResourcePackAnimationRenderer r : l) {
						for (Identifier rl : r.getAnimationFrames()) {
							TextureManager t = MinecraftClient.getInstance().getTextureManager();
							AbstractTexture to = t.getTexture(rl);
							if (to == null) {
								to = new ResourceTexture(rl);
								t.registerTexture(rl, to);
							}
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				System.out.println("[FANCYMENU] FINISHED LOADING ANIMATION TEXTURES!");
				animationsLoaded = true;
			}
		} else {
			animationsLoaded = true;
		}

	}
	
}
