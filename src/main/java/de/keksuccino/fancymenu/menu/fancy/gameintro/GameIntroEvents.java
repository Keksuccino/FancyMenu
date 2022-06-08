package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.ClientTickEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public class GameIntroEvents {

	private static boolean titleScreenDisplayed = false;
	
	@SubscribeEvent
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof TitleScreen) {
			titleScreenDisplayed = true;
		} else if (titleScreenDisplayed && MenuCustomization.isValidScreen(e.getGui())) {
			GameIntroHandler.introDisplayed = true;
		}
		if ((e.getGui() instanceof TitleScreen) && AnimationHandler.isReady() && !GameIntroHandler.introDisplayed) {
			IAnimationRenderer intro = GameIntroHandler.getGameIntroAnimation();
			if (intro != null) {
				Minecraft.getInstance().setScreen(new GameIntroScreen(intro, (TitleScreen) e.getGui()));
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
