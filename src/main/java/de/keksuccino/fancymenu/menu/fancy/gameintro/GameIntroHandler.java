package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraftforge.common.MinecraftForge;

public class GameIntroHandler {

	public static boolean introDisplayed = false;

	public static void init() {
		MinecraftForge.EVENT_BUS.register(new GameIntroEvents());
	}
	
	/**
	 * Returns the game intro or null if no animation was set or the animation was not found.
	 */
	public static IAnimationRenderer getGameIntroAnimation() {
		if (!FancyMenu.config.valueExists("gameintroanimation")) {
			return null;
		}
		String name = FancyMenu.config.getOrDefault("gameintroanimation", "");
		if (AnimationHandler.animationExists(name)) {
			return AnimationHandler.getAnimation(name);
		}
		return null;
	}
	
}
