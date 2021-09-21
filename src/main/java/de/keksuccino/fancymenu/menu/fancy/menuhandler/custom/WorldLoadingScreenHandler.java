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
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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
		
	}
	
	private static int getProgress(GuiScreen screen) {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(GuiScreenWorking.class, "progress", "");
			Field f = ObfuscationReflectionHelper.findField(GuiScreenWorking.class, "field_146590_g"); //progress
			return f.getInt(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private static boolean getDoneWorking(GuiScreen screen) {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(GuiScreenWorking.class, "doneWorking", "");
			Field f = ObfuscationReflectionHelper.findField(GuiScreenWorking.class, "field_146592_h"); //doneWorking
			return f.getBoolean(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private static String getTitle(GuiScreen screen) {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(GuiScreenWorking.class, "title", "");
			Field f = ObfuscationReflectionHelper.findField(GuiScreenWorking.class, "field_146591_a"); //title
			return (String) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private static String getStage(GuiScreen screen) {
		try {
			//TODO reflection
			//Field f = ReflectionHelper.findField(GuiScreenWorking.class, "stage", "");
			Field f = ObfuscationReflectionHelper.findField(GuiScreenWorking.class, "field_146589_f"); //stage
			return (String) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}
