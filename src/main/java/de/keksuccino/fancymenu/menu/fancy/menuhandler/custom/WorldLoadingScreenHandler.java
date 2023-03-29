package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldLoadingScreenHandler extends MenuHandlerBase {

	private long lastNarratorUpdateTime = -1L;
	 
	public WorldLoadingScreenHandler() {
		super(LevelLoadingScreen.class.getName());
	}
	
	@SubscribeEvent
	public void onRender(ScreenEvent.Render.Pre e) {
		if (this.shouldCustomize(e.getScreen())) {
			if (MenuCustomization.isMenuCustomizable(e.getScreen())) {
				e.setCanceled(true);
				
				e.getScreen().renderBackground(e.getPoseStack());
				
				this.renderMenu(e.getPoseStack(), e.getScreen());
			}
		}
	}
	
	private void renderMenu(PoseStack matrix, Screen screen) {
		
		StoringChunkProgressListener tracker = getTracker(screen);
		Font font = Minecraft.getInstance().font;
		int j = screen.width / 2;
		int k = screen.height / 2;
		String s = "";
		
		if (tracker != null) {
			s = Mth.clamp(getTracker(screen).getProgress(), 0, 100) + "%";
			long i = Util.getMillis();
			if (i - this.lastNarratorUpdateTime > 2000L) {
				this.lastNarratorUpdateTime = i;
//				NarratorChatListener.INSTANCE.sayNow((Component.translatable("narrator.loading", s)).getString());
			}
			
			if (FancyMenu.config.getOrDefault("showloadingscreenanimation", true)) {
				LevelLoadingScreen.renderChunks(matrix, getTracker(screen), j, k + 30, 2, 0);
			}
		}
		
		if (FancyMenu.config.getOrDefault("showloadingscreenpercent", true)) {
			GuiComponent.drawCenteredString(matrix, font, s, j, k - 9 / 2 - 30, 16777215);
		}
		
	}
	
	private static StoringChunkProgressListener getTracker(Screen screen) {
		try {
			Field f = ReflectionHelper.findField(LevelLoadingScreen.class, "f_96138_"); //progressListener
			return (StoringChunkProgressListener) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
