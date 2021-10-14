package de.keksuccino.fancymenu.menu.animation;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

public class AnimationHandlerEvents {

	private boolean idle = false;
	private GuiScreen lastScreen;

	private boolean animationsLoaded = false;

	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent e) {
		if (e.getGui() instanceof GuiMainMenu) {

			if (!AnimationHandler.isReady()) {
				AnimationHandler.setupAnimations(e);
			}

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
							for (ResourceLocation rl : r.getAnimationFrames()) {
								TextureManager t = Minecraft.getMinecraft().getTextureManager();
								ITextureObject to = t.getTexture(rl);
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

	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (MenuCustomization.isValidScreen(e.getGui())) {
			if (AnimationHandler.isReady() && (this.lastScreen != e.getGui()) && !LayoutEditorScreen.isActive) {
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
			}

			this.lastScreen = e.getGui();
		}
		this.idle = false;
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		//Stopping audio for all advanced animations if no screen is being displayed
		if ((Minecraft.getMinecraft().currentScreen == null) && AnimationHandler.isReady() && !this.idle) {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
				}
			}
			this.idle = true;
		}
	}

}
