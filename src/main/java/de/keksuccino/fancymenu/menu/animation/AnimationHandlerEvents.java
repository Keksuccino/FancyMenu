package de.keksuccino.fancymenu.menu.animation;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class AnimationHandlerEvents {

	private boolean idle = false;
	private GuiScreen lastScreen;

	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent e) {
		if (e.getGui() instanceof GuiMainMenu) {

			AnimationHandler.preloadAnimations();

		}
	}

	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (MenuCustomization.isValidScreen(e.getGui())) {
			if (AnimationHandler.isReady() && (this.lastScreen != e.getGui()) && !LayoutEditorScreen.isActive) {
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							r.resetAnimation();
						}
					}
				}
			}

			this.lastScreen = e.getGui();
		}
		this.idle = false;
	}

	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		//Stopping audio for all advanced animations if no screen is being displayed
		if ((Minecraft.getMinecraft().currentScreen == null) && AnimationHandler.isReady() && !this.idle) {
			for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
				if (r instanceof AdvancedAnimation) {
					((AdvancedAnimation)r).stopAudio();
				}
			}
			this.idle = true;
		}
	}

}
