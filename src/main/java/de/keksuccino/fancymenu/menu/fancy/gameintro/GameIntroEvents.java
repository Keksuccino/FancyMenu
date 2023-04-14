package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
	public void onClientTick(TickEvent.ClientTickEvent e) {
		if (!GameIntroHandler.introDisplayed && (Minecraft.getInstance().level != null)) {
			GameIntroHandler.introDisplayed = true;
		}
	}

}
