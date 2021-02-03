package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent.Post;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent.Pre;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

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
				
				e.getGui().renderBackground(e.getMatrixStack());
				
				this.renderMenu(e.getMatrixStack(), e.getGui());
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
	public void onInitPre(Pre e) {
		super.onInitPre(e);
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
	
	private void renderMenu(MatrixStack matrix, Screen screen) {
		
		WorldGenerationProgressTracker tracker = getTracker(screen);
		TextRenderer font = MinecraftClient.getInstance().textRenderer;
		int j = screen.width / 2;
		int k = screen.height / 2;
		String s = "";
		
		if (tracker != null) {
			s = MathHelper.clamp(tracker.getProgressPercentage(), 0, 100) + "%";
			long i = Util.getMeasuringTimeMs();
			if (i - this.lastNarratorUpdateTime > 2000L) {
				this.lastNarratorUpdateTime = i;
				NarratorManager.INSTANCE.narrate((new TranslatableText("narrator.loading", s)).getString());
			}
			
			if (FancyMenu.config.getOrDefault("showloadingscreenanimation", true)) {
				LevelLoadingScreen.drawChunkMap(matrix, getTracker(screen), j, k + 30, 2, 0);
			}
		}
		
		if (FancyMenu.config.getOrDefault("showloadingscreenpercent", true)) {
			drawCenteredString(matrix, font, s, j, k - 9 / 2 - 30, 16777215);
		}
		
	}
	
	private static WorldGenerationProgressTracker getTracker(Screen screen) {
		try {
			Field f = ReflectionHelper.findField(LevelLoadingScreen.class, "progressProvider", "field_17406");
			return (WorldGenerationProgressTracker) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
