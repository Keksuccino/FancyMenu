package de.keksuccino.fancymenu.menu.animation;

import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.events.client.GuiOpenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class AnimationHandlerEvents {
	
	private boolean idle = false;
	private Screen lastScreen;

//	@SubscribeEvent
//	public void onGuiOpen(GuiOpenEvent e) {
//		if (e.getGui() instanceof TitleScreen) {
//			if (!AnimationHandler.isReady()) {
//				AnimationHandler.setupAnimations(e);
//			}
//		}
//	}
	
	@SubscribeEvent
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		//Stopping audio and resetting to intro (if enabled) for all advanced animations when changing the screen
		if (MenuCustomization.isValidScreen(e.getScreen())) {
			if (AnimationHandler.isReady() && (this.lastScreen != e.getScreen()) && !LayoutEditorScreen.isActive) {
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
			}

			this.lastScreen = e.getScreen();
		}
		this.idle = false;
	}
	
	@SuppressWarnings("resource")
	@SubscribeEvent
	public void onTick(ClientTickEvent.Pre e) {
		//Stopping audio for all advanced animations if no screen is being displayed
		if ((Minecraft.getInstance().screen == null) && AnimationHandler.isReady() && !this.idle) {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
				}
			}
			this.idle = true;
		}
	}

}
