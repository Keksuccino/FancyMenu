package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenWorking;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WorldLoadingScreenHandler extends MenuHandlerBase {
	 
	public WorldLoadingScreenHandler() {
		super(GuiScreenWorking.class.getName());
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				e.setCanceled(true);
				
				e.getGui().drawDefaultBackground();
				
				this.renderMenu(e.getGui());
			}
		}
	}
	
	private void renderMenu(GuiScreen screen) {
		
		Minecraft mc = Minecraft.getMinecraft();
		FontRenderer font = mc.fontRenderer;
		
		if (getDoneWorking(screen)) {
			
            if (!mc.isConnectedToRealms()) {
                mc.displayGuiScreen((GuiScreen)null);
            }
            
        } else {

        	if (FancyMenu.config.getOrDefault("showloadingscreenpercent", true)) {
        		
        		screen.drawCenteredString(font, getTitle(screen), screen.width / 2, 70, 16777215);
        		screen.drawCenteredString(font, getStage(screen) + " " + getProgress(screen) + "%", screen.width / 2, 90, 16777215);
        		
        	}
            
        }
		
//		TrackingChunkStatusListener tracker = getTracker(screen);
//		FontRenderer font = Minecraft.getInstance().fontRenderer;
//		int j = screen.width / 2;
//		int k = screen.height / 2;
//		String s = "";
//		
//		if (tracker != null) {
//			s = MathHelper.clamp(getTracker(screen).getPercentDone(), 0, 100) + "%";
//			long i = Util.milliTime();
//			if (i - this.lastNarratorUpdateTime > 2000L) {
//				this.lastNarratorUpdateTime = i;
//				NarratorChatListener.INSTANCE.say((new TranslationTextComponent("narrator.loading", s)).getString());
//			}
//			
//			if (FancyMenu.config.getOrDefault("showloadingscreenanimation", true)) {
//				WorldLoadProgressScreen.func_238625_a_(matrix, getTracker(screen), j, k + 30, 2, 0);
//			}
//		}
//		
//		if (FancyMenu.config.getOrDefault("showloadingscreenpercent", true)) {
//			AbstractGui.drawCenteredString(matrix, font, s, j, k - 9 / 2 - 30, 16777215);
//		}
		
	}
	
	private static int getProgress(GuiScreen screen) {
		try {
			Field f = ReflectionHelper.findField(GuiScreenWorking.class, "progress", "");
			return f.getInt(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private static boolean getDoneWorking(GuiScreen screen) {
		try {
			Field f = ReflectionHelper.findField(GuiScreenWorking.class, "doneWorking", "");
			return f.getBoolean(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private static String getTitle(GuiScreen screen) {
		try {
			Field f = ReflectionHelper.findField(GuiScreenWorking.class, "title", "");
			return (String) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private static String getStage(GuiScreen screen) {
		try {
			Field f = ReflectionHelper.findField(GuiScreenWorking.class, "stage", "");
			return (String) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}
