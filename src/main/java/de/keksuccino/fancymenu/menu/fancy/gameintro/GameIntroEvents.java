package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GameIntroEvents {

	private static boolean titleScreenDisplayed = false;
	
	@SubscribeEvent
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof MainMenuScreen) {
			titleScreenDisplayed = true;
		} else if (titleScreenDisplayed && MenuCustomization.isValidScreen(e.getGui())) {
			GameIntroHandler.introDisplayed = true;
		}
		if ((e.getGui() instanceof MainMenuScreen) && AnimationHandler.isReady() && !GameIntroHandler.introDisplayed) {
			IAnimationRenderer intro = GameIntroHandler.getGameIntroAnimation();
			if (intro != null) {
				Minecraft.getInstance().displayGuiScreen(new GameIntroScreen(intro, (MainMenuScreen) e.getGui()));
			} else {
				GameIntroHandler.introDisplayed = true;
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent e) {
		if (!GameIntroHandler.introDisplayed && (Minecraft.getInstance().world != null)) {
			GameIntroHandler.introDisplayed = true;
		}
	}

}
