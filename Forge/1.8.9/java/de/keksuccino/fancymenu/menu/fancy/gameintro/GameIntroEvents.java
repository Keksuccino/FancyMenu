package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GameIntroEvents {
	
	@SubscribeEvent
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if ((e.gui instanceof GuiMainMenu) && AnimationHandler.isReady() && !GameIntroHandler.introDisplayed) {
			IAnimationRenderer intro = GameIntroHandler.getGameIntroAnimation();
			if (intro != null) {
				Minecraft.getMinecraft().displayGuiScreen(new GameIntroScreen(intro, (GuiMainMenu) e.gui));
			} else {
				GameIntroHandler.introDisplayed = true;
			}
		}
	}

}
