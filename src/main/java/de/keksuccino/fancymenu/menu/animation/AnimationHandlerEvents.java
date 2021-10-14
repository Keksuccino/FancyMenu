package de.keksuccino.fancymenu.menu.animation;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.events.client.GuiOpenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

public class AnimationHandlerEvents {
	
	private boolean idle = false;
	private Screen lastScreen;
	
	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent e) {
		if (e.getGui() instanceof TitleScreen) {
			if (!AnimationHandler.isReady()) {
				AnimationHandler.setupAnimations(e);
			}
		}
	}
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Stopping audio and resetting to intro (if enabled) for all advanced animations when changing the screen
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
	
	@SuppressWarnings("resource")
	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		//Stopping audio for all advanced animations if no screen is being displayed
		if ((MinecraftClient.getInstance().currentScreen == null) && AnimationHandler.isReady() && !this.idle) {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
				}
			}
			this.idle = true;
		}
	}

}
