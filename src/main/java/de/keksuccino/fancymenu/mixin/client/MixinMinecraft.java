package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import net.minecraft.client.Minecraft;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;

	//TODO übernehmen
//	private static boolean animationsLoaded = false;
	
	@Inject(at = @At(value = "HEAD"), method = "createTitle", cancellable = true)
	private void onGetWindowTitle(CallbackInfoReturnable<String> info) {

		if (FancyMenu.config != null) {
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

	@Inject(at = @At(value = "HEAD"), method = "setOverlay")
	private void onSetLoadingGui(Overlay loadingGuiIn, CallbackInfo info) {
		if (FancyMenu.config == null) {
			return;
		}
		if (loadingGuiIn == null) {
			//TODO übernehmen
			AnimationHandler.preloadAnimations();
			//----------
			MixinCache.isSplashScreenRendering = false;
			MenuCustomization.isLoadingScreen = false;
			MenuCustomization.reloadCurrentMenu();
		} else {
			MenuCustomization.isLoadingScreen = true;
		}
	}

	//TODO übernehmen
//	private static void preloadAnimations() {
//
//		System.out.println("[FANCYMENU] Updating animation sizes..");
//		AnimationHandler.setupAnimationSizes();
//
//		//Pre-load animation frames to prevent them from lagging when rendered for the first time
//		if (FancyMenu.config.getOrDefault("preloadanimations", true)) {
//			if (!animationsLoaded) {
//				System.out.println("[FANCYMENU] LOADING ANIMATION TEXTURES! THIS CAUSES THE LOADING SCREEN TO FREEZE FOR A WHILE!");
//				try {
//					List<ResourcePackAnimationRenderer> l = new ArrayList<ResourcePackAnimationRenderer>();
//					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
//						if (r instanceof AdvancedAnimation) {
//							IAnimationRenderer main = ((AdvancedAnimation) r).getMainAnimationRenderer();
//							IAnimationRenderer intro = ((AdvancedAnimation) r).getIntroAnimationRenderer();
//							if ((main != null) && (main instanceof ResourcePackAnimationRenderer)) {
//								l.add((ResourcePackAnimationRenderer) main);
//							}
//							if ((intro != null) && (intro instanceof  ResourcePackAnimationRenderer)) {
//								l.add((ResourcePackAnimationRenderer) intro);
//							}
//						} else if (r instanceof ResourcePackAnimationRenderer) {
//							l.add((ResourcePackAnimationRenderer) r);
//						}
//					}
//					for (ResourcePackAnimationRenderer r : l) {
//						for (ResourceLocation rl : r.getAnimationFrames()) {
//							TextureManager t = Minecraft.getInstance().getTextureManager();
//							AbstractTexture to = t.getTexture(rl);
//							if (to == null) {
//								to = new SimpleTexture(rl);
//								t.register(rl, to);
//							}
//						}
//					}
//				} catch (Exception ex) {
//					ex.printStackTrace();
//				}
//				System.out.println("[FANCYMENU] FINISHED LOADING ANIMATION TEXTURES!");
//				animationsLoaded = true;
//			}
//		} else {
//			animationsLoaded = true;
//		}
//
//	}
	
}
