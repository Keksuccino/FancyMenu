package de.keksuccino.fancymenu.menu.animation;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AnimationHandlerEvents {
	
	private boolean idle = false;
	private GuiScreen lastScreen;

	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (e.gui instanceof GuiMainMenu) {
			if (!AnimationHandler.isReady()) {
				AnimationHandler.setupAnimations();
			}
		}
	}
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		
		//Stopping audio and resetting to intro (if enabled) for all advanced animations when changing the screen
		if (MenuCustomization.isValidScreen(e.gui)) {
			if (AnimationHandler.isReady() && (this.lastScreen != e.gui) && !LayoutCreatorScreen.isActive) {
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
			}
			
			this.lastScreen = e.gui;
			this.idle = false;
		}
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
