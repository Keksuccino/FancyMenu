package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.SimpleLoadingScreen;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenRealmsProxy;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomizationHelper {
	
	private boolean showButtonInfo = false;
	private boolean showMenuInfo = false;
	private List<GuiButton> buttons = new ArrayList<GuiButton>();
	private GuiButton buttonInfoButton;
	private GuiButton menuInfoButton;
	
	private GuiScreen current;
	private int lastWidth;
	private int lastHeight;
	private List<AdvancedButton> helperbuttons = new ArrayList<AdvancedButton>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (!isValidScreen(e.getGui())) {
			return;
		}
		
		this.current = e.getGui();
		
		this.handleWidgetsUpdate(e.getButtonList());
		
		if ((this.lastWidth != e.getGui().width) || (this.lastHeight != e.getGui().height)) {
			this.helperbuttons.clear();
			
			String infoLabel = "Button Info";
			if (this.showButtonInfo) {
				infoLabel = "§aButton Info";
			}
			AdvancedButton iButton = new CustomizationButton(5, 5, 70, 20, infoLabel, true, (onPress) -> {
				this.onInfoButtonPress();
			}); 
			this.buttonInfoButton = iButton;
			
			String minfoLabel = "Menu Info";
			if (this.showMenuInfo) {
				minfoLabel = "§aMenu Info";
			}
			AdvancedButton miButton = new CustomizationButton(80, 5, 70, 20, minfoLabel, true, (onPress) -> {
				this.onMoreInfoButtonPress();
			});
			this.menuInfoButton = miButton;
			
			AdvancedButton rButton = new CustomizationButton(e.getGui().width - 55, 5, 50, 20, "Reload", true, (onPress) -> {
				onReloadButtonPress();
			});
			
			AdvancedButton layoutCreatorButton = new CustomizationButton(e.getGui().width - 150, 5, 90, 20, "Create Layout", true, (onPress) -> {
				Minecraft.getMinecraft().displayGuiScreen(new LayoutCreatorScreen(this.current));
				LayoutCreatorScreen.isActive = true;
				MenuCustomization.stopSounds();
				MenuCustomization.resetSounds();
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
			});
			
			if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
				this.helperbuttons.add(iButton);
				this.helperbuttons.add(miButton);
				this.helperbuttons.add(rButton);
				this.helperbuttons.add(layoutCreatorButton);
			}
		}
		
		this.lastWidth = e.getGui().width;
		this.lastHeight = e.getGui().height;
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		
		if (isValidScreen(e.getGui())) {
			for (AdvancedButton b : this.helperbuttons) {
				b.drawButton(Minecraft.getMinecraft(), e.getMouseX(), e.getMouseY(), e.getRenderPartialTicks());
			}
		}
		
		if (this.showMenuInfo  && !(e.getGui() instanceof LayoutCreatorScreen)) {
			GlStateManager.enableBlend();
			e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, "§f§lMenu Identifier:", 7, 30, 0);
			e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, "§f" + e.getGui().getClass().getName(), 7, 40, 0);
			GlStateManager.disableBlend();
		}
		
		if (this.showButtonInfo) {
			for (GuiButton w : this.buttons) {
				if (w.isMouseOver()) {
					int id = getButtonId(w);
					String idString = "<id not found>";
					if (id >= 0) {
						idString = String.valueOf(id);
					}
					String key = ButtonCache.getKeyForButton(w);
					if (key == null) {
						key = "<button has no key>";
					}
					
					String identifier = null;
					if (id >= 0) {
						identifier = ButtonCache.getButtonForId(id).getScreen().getClass().getName();
					}
					if (identifier == null) {
						identifier = "<identifier not found>";
					}
					
					List<String> info = new ArrayList<String>();
					int width = Minecraft.getMinecraft().fontRenderer.getStringWidth("Button Info") + 10;
					
					info.add("§fID: " + idString);
					info.add("§fKey: " + key);
					info.add("§fWidth: " + w.width);
					info.add("§fHeight: " + w.height);
					info.add("§fLabel Width: " + Minecraft.getMinecraft().fontRenderer.getStringWidth(w.displayString));
					
					//Getting the longest string from the list to render the background with the correct width
					for (String s : info) {
						int i = Minecraft.getMinecraft().fontRenderer.getStringWidth(s) + 10;
						if (i > width) {
							width = i;
						}
					}
					
					int x = e.getMouseX();
					if (e.getGui().width < x + width + 10) {
						x -= width + 10;
					}
					
					int y = e.getMouseY();
					if (e.getGui().height < y + 90) {
						y -= 90;
					}
					
					drawInfoBackground(x, y, width + 10, 90);
					
					GlStateManager.enableBlend();
					e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, "§f§lButton Info", x + 10, y + 10, 0);

					int i2 = 20;
					for (String s : info) {
						e.getGui().drawString(Minecraft.getMinecraft().fontRenderer, s, x + 10, y + 10 + i2, 0);
						i2 += 10;
					}
					GlStateManager.disableBlend();
					
					break;
				}
			}
		}
	}
	
	private static boolean isValidScreen(GuiScreen s) {
		//Prevents rendering in child(?)-screens like RealmsScreenProxy
		if (s != Minecraft.getMinecraft().currentScreen) {
			return false;
		}
		//Prevents rendering in realm screens (if it's the main screen)
		if (s instanceof GuiScreenRealmsProxy) {
			return false;
		}
		//Prevents rendering in FancyMenu screens
		if (s instanceof SimpleLoadingScreen) {
			return false;
		}
		if (s instanceof GameIntroScreen) {
			return false;
		}
		//Prevents rendering in layout creation screens
		if (s instanceof LayoutCreatorScreen) {
			return false;
		}
		return true;
	}
	
	private static void drawInfoBackground(int x, int y, int width, int height) {
		GuiScreen.drawRect(x, y, x + width, y + height, new Color(102, 0, 102, 200).getRGB());
	}
	
	private void onInfoButtonPress() {
		if (this.showButtonInfo) {
			this.showButtonInfo = false;
			this.buttonInfoButton.displayString = "Button Info";
			
		} else {
			this.showButtonInfo = true;
			this.buttonInfoButton.displayString = "§aButton Info";
		}
	}
	
	private void onMoreInfoButtonPress() {
		if (this.showMenuInfo) {
			this.showMenuInfo = false;
			this.menuInfoButton.displayString = "Menu Info";
			
		} else {
			this.showMenuInfo = true;
			this.menuInfoButton.displayString = "§aMenu Info";
		}
	}

	private void onReloadButtonPress() {
		FancyMenu.updateConfig();
		MenuCustomization.resetSounds();
		MenuCustomization.reload();
		if (!FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
			this.showButtonInfo = false;
			this.showMenuInfo = false;
		}
		try {
			Minecraft.getMinecraft().displayGuiScreen(Minecraft.getMinecraft().currentScreen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleWidgetsUpdate(List<GuiButton> l) {
		this.buttons.clear();
		for (GuiButton w : l) {
			if (!CustomizationButton.isCustomizationButton(w)) {
				this.buttons.add(w);
			}
		}
	}
	
	/**
	 * Returns the button id or -1 if the button was not found in the button list.
	 */
	private static int getButtonId(GuiButton w) {
		return ButtonCache.getIdForButton(w);
	}
}
