package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GameIntroEvents {

	private static boolean titleScreenDisplayed = false;
	
	@SubscribeEvent
	public void onScreenInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (e.getGui() instanceof GuiMainMenu) {
			titleScreenDisplayed = true;
		} else if (titleScreenDisplayed && MenuCustomization.isValidScreen(e.getGui())) {
			GameIntroHandler.introDisplayed = true;
		}
		if ((e.getGui() instanceof GuiMainMenu) && AnimationHandler.isReady() && !GameIntroHandler.introDisplayed) {
			IAnimationRenderer intro = GameIntroHandler.getGameIntroAnimation();
			if (intro != null) {
				Minecraft.getMinecraft().displayGuiScreen(new GameIntroScreen(intro, (GuiMainMenu) e.getGui()));
			} else {
				GameIntroHandler.introDisplayed = true;
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent e) {
		if (!GameIntroHandler.introDisplayed && (Minecraft.getMinecraft().world != null)) {
			GameIntroHandler.introDisplayed = true;
		}
	}

}
