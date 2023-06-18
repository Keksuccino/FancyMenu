package de.keksuccino.fancymenu.customization.animation;

import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class AnimationHandlerEvents {
	
	private boolean idle = false;
	private Screen lastScreen;

	@EventListener
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		//Stopping audio and resetting to intro (if enabled) for all advanced animations when changing the screen
		if (this.lastScreen != e.getScreen()) {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation a) {
					a.stopAudio();
					if (a.replayIntro()) {
						r.resetAnimation();
					}
				}
			}
		}
		this.lastScreen = e.getScreen();
		this.idle = false;
	}
	
	@EventListener
	public void onTick(ClientTickEvent.Pre e) {
		//Stopping audio for all advanced animations if no screen is being displayed
		if ((Minecraft.getInstance().screen == null) && !this.idle) {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
				}
			}
			this.idle = true;
		}
	}

}
