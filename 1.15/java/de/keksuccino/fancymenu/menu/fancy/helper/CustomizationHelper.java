package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.gui.SimpleLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CustomizationHelper {
	
	private boolean showButtonInfo = false;
	private boolean showMenuInfo = false;
	private List<Widget> buttons = new ArrayList<Widget>();
	private Widget buttonInfoButton;
	private Widget menuInfoButton;
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		//Prevents rendering in child(?)-screens like RealmsScreenProxy
		if (e.getGui() != Minecraft.getInstance().currentScreen) {
			return;
		}
		
		//Prevents rendering in FancyMenu loadingscreens
		if (e.getGui() instanceof SimpleLoadingScreen) {
			return;
		}
		
		this.handleWidgetsUpdate(e.getWidgetList());
		
		String infoLabel = "Button Info";
		if (this.showButtonInfo) {
			infoLabel = "§aButton Info";
		}
		Widget iButton = new CustomizationButton(5, 5, 70, 20, infoLabel, (onPress) -> {
			this.onInfoButtonPress();
		});
		this.buttonInfoButton = iButton;
		
		String minfoLabel = "Menu Info";
		if (this.showMenuInfo) {
			minfoLabel = "§aMenu Info";
		}
		Widget miButton = new CustomizationButton(80, 5, 70, 20, minfoLabel, (onPress) -> {
			this.onMoreInfoButtonPress();
		});
		this.menuInfoButton = miButton;
		
		Widget rButton = new CustomizationButton(e.getGui().width - 55, 5, 50, 20, "Reload", (onPress) -> {
			onReloadButtonPress();
		});
		
		if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
			e.addWidget(iButton);
			e.addWidget(rButton);
			e.addWidget(miButton);
		}

	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (this.showMenuInfo) {
			RenderSystem.enableBlend();
			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f§lMenu Identifier:", 7, 30, 0);
			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f" + e.getGui().getClass().getName(), 7, 40, 0);
			RenderSystem.disableBlend();
		}
		
		if (this.showButtonInfo) {
			for (Widget w : this.buttons) {
				if (w.isHovered()) {
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
					int width = Minecraft.getInstance().fontRenderer.getStringWidth("Button Info") + 10;
					
					info.add("§fID: " + idString);
					info.add("§fKey: " + key);
					info.add("§fWidth: " + w.getWidth());
					info.add("§fHeight: " + w.getHeight());
					info.add("§fLabel Width: " + Minecraft.getInstance().fontRenderer.getStringWidth(w.getMessage()));
					
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
					e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f§lButton Info", x + 10, y + 10, 0);

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
	
	private static void drawInfoBackground(int x, int y, int width, int height) {
		Screen.fill(x, y, x + width, y + height, new Color(102, 0, 102, 200).getRGB());
	}
	
	private void onInfoButtonPress() {
		if (this.showButtonInfo) {
			this.showButtonInfo = false;
			this.buttonInfoButton.setMessage("Button Info");
			
		} else {
			this.showButtonInfo = true;
			this.buttonInfoButton.setMessage("§aButton Info");
		}
	}
	
	private void onMoreInfoButtonPress() {
		if (this.showMenuInfo) {
			this.showMenuInfo = false;
			this.menuInfoButton.setMessage("Menu Info");
			
		} else {
			this.showMenuInfo = true;
			this.menuInfoButton.setMessage("§aMenu Info");
		}
	}

	private static void onReloadButtonPress() {
		MenuCustomization.reload();
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
