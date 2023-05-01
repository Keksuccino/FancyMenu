package de.keksuccino.fancymenu.customization.backend.menuhandler.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.backend.MenuCustomization;
import de.keksuccino.fancymenu.customization.backend.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinLevelLoadingScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;

public class WorldLoadingScreenHandler extends MenuHandlerBase {

	private long lastNarratorUpdateTime = -1L;
	 
	public WorldLoadingScreenHandler() {
		super(LevelLoadingScreen.class.getName());
	}
	
	@EventListener
	public void onRender(RenderScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getScreen())) {
			if (MenuCustomization.isMenuCustomizable(e.getScreen())) {
				e.setCanceled(true);
				e.getScreen().renderBackground(e.getPoseStack());
				this.renderMenu(e.getPoseStack(), e.getScreen());
			}
		}
	}
	
	private void renderMenu(PoseStack matrix, Screen screen) {
		
		StoringChunkProgressListener tracker = ((IMixinLevelLoadingScreen)screen).getProgressListenerFancyMenu();
		Font font = Minecraft.getInstance().font;
		int j = screen.width / 2;
		int k = screen.height / 2;
		String s = "";
		
		if (tracker != null) {
			s = Mth.clamp(((IMixinLevelLoadingScreen)screen).getProgressListenerFancyMenu().getProgress(), 0, 100) + "%";
			long i = Util.getMillis();
			if (i - this.lastNarratorUpdateTime > 2000L) {
				this.lastNarratorUpdateTime = i;
			}
			
			if (FancyMenu.getConfig().getOrDefault("showloadingscreenanimation", true)) {
				LevelLoadingScreen.renderChunks(matrix, ((IMixinLevelLoadingScreen)screen).getProgressListenerFancyMenu(), j, k + 30, 2, 0);
			}
		}
		
		if (FancyMenu.getConfig().getOrDefault("showloadingscreenpercent", true)) {
			GuiComponent.drawCenteredString(matrix, font, s, j, k - 9 / 2 - 30, 16777215);
		}
		
	}

}
