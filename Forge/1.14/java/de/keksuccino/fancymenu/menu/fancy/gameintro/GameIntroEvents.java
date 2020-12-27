package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GameIntroEvents {
	
	@SubscribeEvent
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if ((e.getGui() instanceof MainMenuScreen) && AnimationHandler.isReady() && !GameIntroHandler.introDisplayed) {
			GameIntroHandler.introDisplayed = true;
			IAnimationRenderer intro = GameIntroHandler.getGameIntroAnimation();
			if (intro != null) {
				Minecraft.getInstance().displayGuiScreen(new GameIntroScreen(intro, (MainMenuScreen) e.getGui()));
			}
		}
	}

}
