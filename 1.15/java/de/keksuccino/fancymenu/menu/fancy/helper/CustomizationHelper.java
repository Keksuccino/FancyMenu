package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.SimpleLoadingScreen;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.realms.RealmsScreenProxy;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CustomizationHelper {
	
	private static CustomizationHelper instance;
	
	private boolean showButtonInfo = false;
	private boolean showMenuInfo = false;
	private List<Widget> buttons = new ArrayList<Widget>();
	private AdvancedButton buttonInfoButton;
	private AdvancedButton menuInfoButton;
	
	private Screen current;
	private int lastWidth;
	private int lastHeight;
	private List<AdvancedButton> helperbuttons = new ArrayList<AdvancedButton>();
	
	public static void init() {
		instance = new CustomizationHelper();
		MinecraftForge.EVENT_BUS.register(instance);
	}
	
	public static CustomizationHelper getInstance() {
		return instance;
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (!isValidScreen(e.getGui())) {
			return;
		}
		
		this.current = e.getGui();
		
		this.handleWidgetsUpdate(e.getWidgetList());
		
		if ((this.lastWidth != e.getGui().width) || (this.lastHeight != e.getGui().height)) {
			this.helperbuttons.clear();
			
			String infoLabel = Locals.localize("helper.button.buttoninfo");
			if (this.showButtonInfo) {
				infoLabel = "§a" + Locals.localize("helper.button.buttoninfo");
			}
			AdvancedButton iButton = new CustomizationButton(5, 5, 70, 20, infoLabel, true, (onPress) -> {
				this.onInfoButtonPress();
			}); 
			this.buttonInfoButton = iButton;
			
			String minfoLabel = Locals.localize("helper.button.menuinfo");
			if (this.showMenuInfo) {
				minfoLabel = "§a" + Locals.localize("helper.button.menuninfo");
			}
			AdvancedButton miButton = new CustomizationButton(80, 5, 70, 20, minfoLabel, true, (onPress) -> {
				this.onMoreInfoButtonPress();
			});
			this.menuInfoButton = miButton;
			
			AdvancedButton rButton = new CustomizationButton(e.getGui().width - 55, 5, 50, 20, Locals.localize("helper.button.reload"), true, (onPress) -> {
				onReloadButtonPress();
			});
			
			AdvancedButton layoutCreatorButton = new CustomizationButton(e.getGui().width - 150, 5, 90, 20, Locals.localize("helper.button.createlayout"), true, (onPress) -> {
				Minecraft.getInstance().displayGuiScreen(new LayoutCreatorScreen(this.current));
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
				b.render(e.getMouseX(), e.getMouseY(), e.getRenderPartialTicks());
			}
		}
		
		if (this.showMenuInfo && !(e.getGui() instanceof LayoutCreatorScreen)) {
			RenderSystem.enableBlend();
			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f§l" + Locals.localize("helper.menuinfo.identifier") + ":", 7, 30, 0);
			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f" + e.getGui().getClass().getName(), 7, 40, 0);
			RenderSystem.disableBlend();
		}

		if (this.showButtonInfo) {
			for (Widget w : this.buttons) {
				if (w.isHovered()) {
					int id = getButtonId(w);
					String idString = Locals.localize("helper.buttoninfo.idnotfound");
					if (id >= 0) {
						idString = String.valueOf(id);
					}
					String key = ButtonCache.getKeyForButton(w);
					if (key == null) {
						key = Locals.localize("helper.buttoninfo.keynotfound");
					}
					
					List<String> info = new ArrayList<String>();
					int width = Minecraft.getInstance().fontRenderer.getStringWidth(Locals.localize("helper.button.buttoninfo")) + 10;
					
					info.add("§f" + Locals.localize("helper.buttoninfo.id") + ": " + idString);
					info.add("§f" + Locals.localize("helper.buttoninfo.key") + ": " + key);
					info.add("§f" + Locals.localize("general.width") + ": " + w.getWidth());
					info.add("§f" + Locals.localize("general.height") + ": " + w.getHeight());
					info.add("§f" + Locals.localize("helper.buttoninfo.labelwidth") + ": " + Minecraft.getInstance().fontRenderer.getStringWidth(w.getMessage()));
					
					//Getting the longest string from the list to render the background with the correct width
					for (String s : info) {
						int i = Minecraft.getInstance().fontRenderer.getStringWidth(s) + 10;
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
					
					RenderSystem.enableBlend();
					e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f§l" + Locals.localize("helper.button.buttoninfo"), x + 10, y + 10, 0);

					int i2 = 20;
					for (String s : info) {
						e.getGui().drawString(Minecraft.getInstance().fontRenderer, s, x + 10, y + 10 + i2, 0);
						i2 += 10;
					}
					RenderSystem.disableBlend();
					
					break;
				}
			}
		}
	}
	
	private static boolean isValidScreen(Screen s) {
		//Prevents rendering in child(?)-screens like RealmsScreenProxy
		if (s != Minecraft.getInstance().currentScreen) {
			return false;
		}
		//Prevents rendering in realm screens (if it's the main screen)
		if (s instanceof RealmsScreenProxy) {
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
		IngameGui.fill(x, y, x + width, y + height, new Color(102, 0, 102, 200).getRGB());
	}
	
	public void onInfoButtonPress() {
		if (this.showButtonInfo) {
			this.showButtonInfo = false;
			this.buttonInfoButton.setMessage(Locals.localize("helper.button.buttoninfo"));;
			
		} else {
			this.showButtonInfo = true;
			this.buttonInfoButton.setMessage("§a" + Locals.localize("helper.button.buttoninfo"));;
		}
	}
	
	public void onMoreInfoButtonPress() {
		if (this.showMenuInfo) {
			this.showMenuInfo = false;
			this.menuInfoButton.setMessage(Locals.localize("helper.button.menuinfo"));;
			
		} else {
			this.showMenuInfo = true;
			this.menuInfoButton.setMessage("§a" + Locals.localize("helper.button.menuinfo"));;
		}
	}

	public void onReloadButtonPress() {
		FancyMenu.updateConfig();
		MenuCustomization.resetSounds();
		MenuCustomization.reload();
		if (!FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
			this.showButtonInfo = false;
			this.showMenuInfo = false;
		}
		try {
			Minecraft.getInstance().displayGuiScreen(Minecraft.getInstance().currentScreen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleWidgetsUpdate(List<Widget> l) {
		this.buttons.clear();
		for (Widget w : l) {
			if (!CustomizationButton.isCustomizationButton(w)) {
				this.buttons.add(w);
			}
		}
	}
	
	/**
	 * Returns the button id or -1 if the button was not found in the button list.
	 */
	private static int getButtonId(Widget w) {
		return ButtonCache.getIdForButton(w);
	}
}
