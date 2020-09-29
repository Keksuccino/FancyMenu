package de.keksuccino.fancymenu.menu.fancy;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.music.AdvancedMusicTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

//Yes, I'm using too much "XYEvents" classes, but seperating events for every part of the mod helps me to find stuff more easily.
public class MenuCustomizationEvents {
	
	private boolean idle = false;


//	//TODO remove debug
//	@SubscribeEvent
//	public void onMenuOpen(GuiOpenEvent e) {
//		
//		e.setCanceled(true);
//		this.openScaledGui(e.getGui(), 1);
//
//	}
//	
//	//TODO remove debug
//	private void openScaledGui(GuiScreen screen, int scale) {
//		Minecraft mc = Minecraft.getMinecraft();
//		GuiScreen old = mc.currentScreen;
//		
//        if (old != null && screen != old) {
//            old.onGuiClosed();
//        }
//
//        if (screen instanceof GuiMainMenu || screen instanceof GuiMultiplayer) {
//            mc.gameSettings.showDebugInfo = false;
//            mc.ingameGUI.getChatGUI().clearChatMessages(true);
//        }
//
//        mc.currentScreen = screen;
//
//        if (screen != null) {
//            mc.setIngameNotInFocus();
//            KeyBinding.unPressAllKeys();
//
//            while (Mouse.next()) {
//                ;
//            }
//
//            while (Keyboard.next()) {
//                ;
//            }
//
//            AdvancedScaledResolution res = new AdvancedScaledResolution(scale);
//            int i = res.getScaledWidth();
//            int j = res.getScaledHeight();
//            screen.setWorldAndResolution(mc, i, j);
//            mc.skipRenderWorld = false;
//        } else {
//            mc.getSoundHandler().resumeSounds();
//            mc.setIngameFocus();
//        }
//	}
	
	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		//Stopping audio for all menu handlers when changing the screen
		if (MenuCustomization.isValidScreen(e.getGui()) && !LayoutCreatorScreen.isActive) {
			this.idle = false;
		}
		
		if (!MenuCustomization.isValidScreen(Minecraft.getMinecraft().currentScreen)) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
		}
		
		if (!FancyMenu.config.getOrDefault("playmenumusic", true)) {
			MusicTicker m = Minecraft.getMinecraft().getMusicTicker();
			if (m instanceof AdvancedMusicTicker) {
				((AdvancedMusicTicker)m).stop();
			}
		}
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		//Stopping audio for all menu handlers if no screen is being displayed
		if ((Minecraft.getMinecraft().currentScreen == null) && !this.idle) {
			MenuCustomization.stopSounds();
			MenuCustomization.resetSounds();
			this.idle = true;
		}
	}
	
	@SubscribeEvent
	public void onMenuReload(MenuReloadedEvent e) {
		MenuCustomization.reloadExcludedMenus();
	}

}
