package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.event.acara.SubscribeEvent;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.event.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public class GameIntroEvents {

	private static boolean titleScreenDisplayed = false;

	
	@SubscribeEvent
	public void onScreenInitPre(InitOrResizeScreenEvent.Pre e) {
		if (e.getScreen() instanceof TitleScreen) {
			titleScreenDisplayed = true;
		} else if (titleScreenDisplayed && MenuCustomization.isValidScreen(e.getScreen())) {
			GameIntroHandler.introDisplayed = true;
		}
		if ((e.getScreen() instanceof TitleScreen) && AnimationHandler.isReady() && !GameIntroHandler.introDisplayed) {
			IAnimationRenderer intro = GameIntroHandler.getGameIntroAnimation();
			if (intro != null) {
				Minecraft.getInstance().setScreen(new GameIntroScreen(intro, (TitleScreen) e.getScreen()));
			} else {
				GameIntroHandler.introDisplayed = true;
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(ClientTickEvent.Pre e) {
		if (!GameIntroHandler.introDisplayed && (Minecraft.getInstance().level != null)) {
			GameIntroHandler.introDisplayed = true;
		}
	}

}
