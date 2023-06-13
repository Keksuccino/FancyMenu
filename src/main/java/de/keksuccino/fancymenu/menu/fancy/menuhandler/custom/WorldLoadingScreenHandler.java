package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import de.keksuccino.fancymenu.events.*;
import de.keksuccino.fancymenu.mixin.client.IMixinLevelLoadingScreen;
import de.keksuccino.konkrete.events.EventPriority;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent.Post;

public class WorldLoadingScreenHandler extends MenuHandlerBase {

	private long lastNarratorUpdateTime = -1L;
	 
	public WorldLoadingScreenHandler() {
		super(LevelLoadingScreen.class.getName());
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				e.setCanceled(true);
				
				e.getGui().renderBackground(e.getGuiGraphics());
				
				this.renderMenu(e.getGuiGraphics(), e.getGui());
			}
		}
	}
	
	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	@Override
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		super.onInitPre(e);
	}

	@SubscribeEvent
	@Override
	public void onSoftReload(SoftMenuReloadEvent e) {
		super.onSoftReload(e);
	}
	
	@SubscribeEvent
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(Post e) {
		super.onRenderPost(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
		super.onButtonClickSound(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
		super.onButtonRenderBackground(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
		super.onRenderListBackground(e);
	}
	
	private void renderMenu(GuiGraphics graphics, Screen screen) {
		
		StoringChunkProgressListener tracker = getTracker(screen);
		Font font = Minecraft.getInstance().font;
		int j = screen.width / 2;
		int k = screen.height / 2;
		String s = "";
		
		if (tracker != null) {
			s = Mth.clamp(tracker.getProgress(), 0, 100) + "%";
			long i = Util.getMillis();
			if (i - this.lastNarratorUpdateTime > 2000L) {
				this.lastNarratorUpdateTime = i;
//				NarratorChatListener.INSTANCE.sayNow(Component.translatable("narrator.loading", s));
			}
			
			if (FancyMenu.config.getOrDefault("showloadingscreenanimation", true)) {
				LevelLoadingScreen.renderChunks(graphics, getTracker(screen), j, k + 30, 2, 0);
			}
		}
		
		if (FancyMenu.config.getOrDefault("showloadingscreenpercent", true)) {
			graphics.drawCenteredString(font, s, j, k - 9 / 2 - 30, 16777215);
		}
		
	}
	
	private static StoringChunkProgressListener getTracker(Screen screen) {
		return ((IMixinLevelLoadingScreen)screen).getProgressListenerFancyMenu();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		super.onRenderPre(e);
	}

}
