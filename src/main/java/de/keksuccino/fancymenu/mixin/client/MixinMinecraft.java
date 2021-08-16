package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.animation.ResourcePackAnimationRenderer;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LoadingGui;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;

	//TODO übernehmen
	private static boolean animationsLoaded = false;
	
	@Inject(at = @At(value = "HEAD"), method = "getWindowTitle", cancellable = true)
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

	@Inject(at = @At(value = "HEAD"), method = "setLoadingGui", cancellable = false)
	private void onSetLoadingGui(LoadingGui loadingGuiIn, CallbackInfo info) {
		if (loadingGuiIn == null) {
			//TODO übernehmen
			preloadAnimations();
			//TODO übernehmen
			MixinCache.isSplashScreenRendering = false;
			MenuCustomization.isLoadingScreen = false;
			MenuCustomization.reloadCurrentMenu();
		} else {
			MenuCustomization.isLoadingScreen = true;
		}
	}

	private static void preloadAnimations() {

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
						for (ResourceLocation rl : r.getAnimationFrames()) {
							TextureManager t = Minecraft.getInstance().getTextureManager();
							Texture to = t.getTexture(rl);
							if (to == null) {
								to = new SimpleTexture(rl);
								t.loadTexture(rl, to);
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
