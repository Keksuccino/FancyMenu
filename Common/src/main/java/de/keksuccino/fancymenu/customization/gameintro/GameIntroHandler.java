package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import org.jetbrains.annotations.Nullable;

public class GameIntroHandler {

	public static boolean introPlayed = false;

	public static boolean shouldPlayIntro() {
		return !FancyMenu.getOptions().gameIntroAnimation.getValue().isEmpty() && AnimationHandler.animationExists(FancyMenu.getOptions().gameIntroAnimation.getValue());
	}

	@Nullable
	public static IAnimationRenderer getGameIntroAnimation() {
		String name = FancyMenu.getOptions().gameIntroAnimation.getValue();
		if (name.isEmpty()) return null;
		if (AnimationHandler.animationExists(name)) {
			return AnimationHandler.getAnimation(name);
		}
		return null;
	}
	
}
