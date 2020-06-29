package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.SimpleLoadingScreen;
import de.keksuccino.core.gui.screens.popup.NotificationPopup;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.properties.PropertiesSet;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.PreloadedLayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.realms.RealmsScreen;
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
	
	public Screen current;
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
		
		if ((this.lastWidth != e.getGui().field_230708_k_) || (this.lastHeight != e.getGui().field_230709_l_)) {
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
			
			AdvancedButton rButton = new CustomizationButton(e.getGui().field_230708_k_ - 55, 5, 50, 20, Locals.localize("helper.button.reload"), true, (onPress) -> {
				onReloadButtonPress();
			});
			
			AdvancedButton layoutCreatorButton = new CustomizationButton(e.getGui().field_230708_k_ - 150, 5, 90, 20, Locals.localize("helper.button.createlayout"), true, (onPress) -> {
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

			AdvancedButton editLayoutButton = new CustomizationButton(e.getGui().field_230708_k_ - 245, 5, 90, 20, Locals.localize("helper.creator.editlayout"), true, (onPress) -> {
				List<PropertiesSet> l = MenuCustomizationProperties.getPropertiesWithIdentifier(this.current.getClass().getName());
				if (l.isEmpty()) {
					PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.creator.editlayout.nolayouts.msg")));
				}
				if (l.size() == 1) {
					if (!MenuCustomization.containsCalculations(l.get(0))) {
						Minecraft.getInstance().displayGuiScreen(new PreloadedLayoutCreatorScreen(this.current, l));
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
					} else {
						PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.creator.editlayout.unsupportedvalues")));
					}
				}
				if (l.size() > 1) {
					PopupHandler.displayPopup(new EditLayoutPopup(l));
				}
			});
			
			if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
				this.helperbuttons.add(iButton);
				this.helperbuttons.add(miButton);
				this.helperbuttons.add(rButton);
				this.helperbuttons.add(layoutCreatorButton);
				this.helperbuttons.add(editLayoutButton);
			}
		}

		this.lastWidth = e.getGui().field_230708_k_;
		this.lastHeight = e.getGui().field_230709_l_;
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (PopupHandler.isPopupActive()) {
			return;
		}
		if (!isValidScreen(e.getGui())) {
			return;
		}
		
		for (AdvancedButton b : this.helperbuttons) {
			b.render(e.getMatrixStack(), e.getMouseX(), e.getMouseY(), e.getRenderPartialTicks());
		}
		
		FontRenderer font = Minecraft.getInstance().fontRenderer;
		
		if (this.showMenuInfo && !(e.getGui() instanceof LayoutCreatorScreen)) {
			RenderSystem.enableBlend();
			font.func_238405_a_(e.getMatrixStack(), "§f§l" + Locals.localize("helper.menuinfo.identifier") + ":", 7, 30, 0);
			font.func_238405_a_(e.getMatrixStack(), "§f" + e.getGui().getClass().getName(), 7, 40, 0);
			RenderSystem.disableBlend();
		}

		if (this.showButtonInfo) {
			for (Widget w : this.buttons) {
				if (w.func_230449_g_()) {
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
					info.add("§f" + Locals.localize("general.width") + ": " + w.func_230998_h_());
					info.add("§f" + Locals.localize("general.height") + ": " + w.getHeight());
					info.add("§f" + Locals.localize("helper.buttoninfo.labelwidth") + ": " + Minecraft.getInstance().fontRenderer.getStringWidth(w.func_230458_i_().getString()));
					
					//Getting the longest string from the list to render the background with the correct width
					for (String s : info) {
						int i = Minecraft.getInstance().fontRenderer.getStringWidth(s) + 10;
						if (i > width) {
							width = i;
						}
					}
					
					int x = e.getMouseX();
					if (e.getGui().field_230708_k_ < x + width + 10) {
						x -= width + 10;
					}
					
					int y = e.getMouseY();
					if (e.getGui().field_230709_l_ < y + 90) {
						y -= 90;
					}
					
					drawInfoBackground(e.getMatrixStack(), x, y, width + 10, 90);
					
					RenderSystem.enableBlend();
					font.func_238405_a_(e.getMatrixStack(), "§f§l" + Locals.localize("helper.button.buttoninfo"), x + 10, y + 10, 0);

					int i2 = 20;
					for (String s : info) {
						font.func_238405_a_(e.getMatrixStack(), s, x + 10, y + 10 + i2, 0);
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
		//TODO RealmsScreenProxy
		if (s instanceof RealmsScreen) {
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
	
	private static void drawInfoBackground(MatrixStack matrix, int x, int y, int width, int height) {
		IngameGui.func_238467_a_(matrix, x, y, x + width, y + height, new Color(102, 0, 102, 200).getRGB());
	}
	
	public void updateCustomizationButtons() {
		this.lastHeight = -1;
		Screen current = Minecraft.getInstance().currentScreen;
		if (current != null) {
			Minecraft.getInstance().displayGuiScreen(current);
		}
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
