package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroScreen;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.CreateCustomGuiPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar.ElementAlignment;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.screens.ConfigScreen;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

public class CustomizationHelperUI extends UIBase {
	
	public static MenuBar bar;
	
	public static boolean showButtonInfo = false;
	public static boolean showMenuInfo = false;
	protected static List<ButtonData> buttons = new ArrayList<ButtonData>();
	protected static int tick = 0;
	
	protected static final Identifier CLOSE_BUTTON_TEXTURE = new Identifier("keksuccino", "close_btn.png");
	protected static final Identifier RELOAD_BUTTON_TEXTURE = new Identifier("keksuccino", "/filechooser/back_icon.png");
	
	public static void init() {
		
		Konkrete.getEventHandler().registerEventsFrom(new CustomizationHelperUI());
		
	}
	
	public static void updateUI() {
		try {
			
			boolean extended = true;
			if (bar != null) {
				extended = bar.isExtended();
			}
			
			bar = new MenuBar();
			
			/** CURRENT MENU TAB START **/
			FMContextMenu currentMenu = new FMContextMenu();
			currentMenu.setAutoclose(true);
			bar.addChild(currentMenu, "fm.ui.tab.current", ElementAlignment.LEFT);
			
			String toggleLabel = Locals.localize("helper.popup.togglecustomization.enable");
			if (MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {
				toggleLabel = Locals.localize("helper.popup.togglecustomization.disable");
			}
			CustomizationButton toggleCustomizationButton = new CustomizationButton(0, 0, 0, 0, toggleLabel, true, (press) -> {
				if (MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {
					press.setMessage(new LiteralText(Locals.localize("helper.popup.togglecustomization.enable")));
					MenuCustomization.disableCustomizationForMenu(MinecraftClient.getInstance().currentScreen);
					CustomizationHelper.reloadSystemAndMenu();
				} else {
					press.setMessage(new LiteralText(Locals.localize("helper.popup.togglecustomization.disable")));
					MenuCustomization.enableCustomizationForMenu(MinecraftClient.getInstance().currentScreen);
					CustomizationHelper.reloadSystemAndMenu();
				}
			});
			toggleCustomizationButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.onoff.btndesc"), "%n%"));
			currentMenu.addContent(toggleCustomizationButton);
			
			FMContextMenu layoutsMenu = new FMContextMenu();
			layoutsMenu.setAutoclose(true);
			currentMenu.addChild(layoutsMenu);
			
			CustomizationButton newLayoutButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.new"), true, (press) -> {
				LayoutEditorScreen.isActive = true;
				Screen s = MinecraftClient.getInstance().currentScreen;
				MinecraftClient.getInstance().openScreen(new LayoutEditorScreen(s));
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
			newLayoutButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.new.desc"), "%n%"));
			layoutsMenu.addContent(newLayoutButton);
			
			ManageLayoutsContextMenu manageLayoutsMenu = new ManageLayoutsContextMenu();
			manageLayoutsMenu.setAutoclose(true);
			layoutsMenu.addChild(manageLayoutsMenu);
			
			CustomizationButton manageLayoutsButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.manage"), true, (press) -> {
				manageLayoutsMenu.setParentButton((AdvancedButton) press);
				manageLayoutsMenu.openMenuAt(press);
			});
			manageLayoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.manage.desc"), "%n%"));
			layoutsMenu.addContent(manageLayoutsButton);
			
			CustomizationButton layoutsButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts"), true, (press) -> {
				layoutsMenu.setParentButton((AdvancedButton) press);
				layoutsMenu.openMenuAt(0, press.y);
			});
			if (!MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {
				layoutsButton.active = false;
			}
			layoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.desc"), "%n%"));
			currentMenu.addContent(layoutsButton);
			
			FMContextMenu advancedMenu = new FMContextMenu();
			advancedMenu.setAutoclose(true);
			currentMenu.addChild(advancedMenu);
			
			OverrideMenuContextMenu overrideMenu = new OverrideMenuContextMenu();
			overrideMenu.setAutoclose(true);
			advancedMenu.addChild(overrideMenu);
			
			String overrLabel = Locals.localize("helper.buttons.tools.overridemenu");
			if (CustomizationHelper.isScreenOverridden(MinecraftClient.getInstance().currentScreen)) {
				overrLabel = Locals.localize("helper.buttons.tools.resetoverride");
			}
			CustomizationButton overrideButton = new CustomizationButton(0, 0, 0, 0, overrLabel, true, (press) -> {
				
				if (!CustomizationHelper.isScreenOverridden(MinecraftClient.getInstance().currentScreen)) {
					
					overrideMenu.setParentButton((AdvancedButton) press);
					overrideMenu.openMenuAt(0, press.y);
					
				} else {

					for (String s : FileUtils.getFiles(FancyMenu.getCustomizationPath().getPath())) {
						PropertiesSet props = PropertiesSerializer.getProperties(s);
						if (props == null) {
							continue;
						}
						PropertiesSet props2 = new PropertiesSet(props.getPropertiesType());
						List<PropertiesSection> l = props.getProperties();
						List<PropertiesSection> l2 = new ArrayList<PropertiesSection>();
						boolean b = false;

						List<PropertiesSection> metas = props.getPropertiesOfType("customization-meta");
						if ((metas == null) || metas.isEmpty()) {
							metas = props.getPropertiesOfType("type-meta");
						}
						if (metas != null) {
							if (metas.isEmpty()) {
								continue;
							}
							String identifier = metas.get(0).getEntryValue("identifier");
							Screen overridden = ((CustomGuiBase)MinecraftClient.getInstance().currentScreen).getOverriddenScreen();
							if ((identifier == null) || !identifier.equalsIgnoreCase(overridden.getClass().getName())) {
								continue;
							}

						} else {
							continue;
						}

						for (PropertiesSection sec : l) {
							String action = sec.getEntryValue("action");
							if (sec.getSectionType().equalsIgnoreCase("customization-meta") || sec.getSectionType().equalsIgnoreCase("type-meta")) {
								l2.add(sec);
								continue;
							}
							if ((action != null) && !action.equalsIgnoreCase("overridemenu")) {
								l2.add(sec);
							}
							if ((action != null) && action.equalsIgnoreCase("overridemenu")) {
								b = true;
							}
						}

						if (b) {
							File f = new File(s);
							if (f.exists() && f.isFile()) {
								f.delete();
							}

							if (l2.size() > 1) {
								for (PropertiesSection sec : l2) {
									props2.addProperties(sec);
								}

								PropertiesSerializer.writeProperties(props2, s);
							}
						}
					}

					CustomizationHelper.reloadSystemAndMenu();
					if (MinecraftClient.getInstance().currentScreen instanceof CustomGuiBase) {
						MinecraftClient.getInstance().openScreen(((CustomGuiBase) MinecraftClient.getInstance().currentScreen).getOverriddenScreen());
					}
				}
			});
			overrideButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.overridewith.btndesc"), "%n%"));
			if (!(MinecraftClient.getInstance().currentScreen instanceof CustomGuiBase)) {
				advancedMenu.addContent(overrideButton);
			} else if (((CustomGuiBase)MinecraftClient.getInstance().currentScreen).getOverriddenScreen() != null) {
				advancedMenu.addContent(overrideButton);
			}
			
			CustomizationButton advancedButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.advanced"), true, (press) -> {
				advancedMenu.setParentButton((AdvancedButton) press);
				advancedMenu.openMenuAt(0, press.y);
			});
			if (!MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {
				advancedButton.active = false;
			}
			advancedButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.advanced.desc"), "%n%"));
			if (FancyMenu.config.getOrDefault("advancedmode", false)) {
				currentMenu.addContent(advancedButton);
			}
			
			CustomizationButton closeCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.closegui"), true, (press) -> {
				MinecraftClient.getInstance().openScreen(null);
			});
			closeCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			if ((MinecraftClient.getInstance().currentScreen instanceof CustomGuiBase) && (((CustomGuiBase)MinecraftClient.getInstance().currentScreen).getOverriddenScreen() == null)) {
				currentMenu.addContent(closeCustomGuiButton);
			}
			
			CustomizationButton currentTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current"), true, (press) -> {
				currentMenu.setParentButton((AdvancedButton) press);
				currentMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(currentTab, "fm.ui.tab.current", ElementAlignment.LEFT, false);
			/** CURRENT MENU TAB END **/
			
			/** CUSTOM GUI TAB START **/
			FMContextMenu customGuiMenu = new FMContextMenu();
			customGuiMenu.setAutoclose(true);
			bar.addChild(customGuiMenu, "fm.ui.tab.customguis", ElementAlignment.LEFT);
			
			CustomizationButton newCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis.new"), true, (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						PopupHandler.displayPopup(new CreateCustomGuiPopup());
					}
				}, Locals.localize("helper.ui.customguis.new.sure")));
			});
			newCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.customguis.new.desc"), "%n%"));
			customGuiMenu.addContent(newCustomGuiButton);
			
			ManageCustomGuiContextMenu manageCustomGuiMenu = new ManageCustomGuiContextMenu();
			manageCustomGuiMenu.setAutoclose(true);
			customGuiMenu.addChild(manageCustomGuiMenu);
			
			CustomizationButton manageCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis.manage"), true, (press) -> {
				manageCustomGuiMenu.setParentButton((AdvancedButton) press);
				manageCustomGuiMenu.openMenuAt(0, press.y);
			});
			manageCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.customguis.manage.desc"), "%n%"));
			customGuiMenu.addContent(manageCustomGuiButton);
			
			CustomizationButton customGuiTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.customguis"), true, (press) -> {
				customGuiMenu.setParentButton((AdvancedButton) press);
				customGuiMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			if (FancyMenu.config.getOrDefault("advancedmode", false)) {
				bar.addElement(customGuiTab, "fm.ui.tab.customguis", ElementAlignment.LEFT, false);
			}
			/** CUSTOM GUI TAB END **/
			
			/** TOOLS TAB START **/
			FMContextMenu toolsMenu = new FMContextMenu();
			toolsMenu.setAutoclose(true);
			bar.addChild(toolsMenu, "fm.ui.tab.tools", ElementAlignment.LEFT);
			
			CustomizationButton menuInfoButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.tools.menuinfo.off"), true, (press) -> {
				if (showMenuInfo) {
					showMenuInfo = false;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.menuinfo.off"));
				} else {
					showMenuInfo = true;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.menuinfo.on"));
				}
			});
			if (showMenuInfo) {
				menuInfoButton.setMessage(Locals.localize("helper.ui.tools.menuinfo.on"));
			}
			menuInfoButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.tools.menuinfo.desc"), "%n%"));
			toolsMenu.addContent(menuInfoButton);
			
			CustomizationButton buttonInfoButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.tools.buttoninfo.off"), true, (press) -> {
				if (showButtonInfo) {
					showButtonInfo = false;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.buttoninfo.off"));
				} else {
					showButtonInfo = true;
					((AdvancedButton)press).setMessage(Locals.localize("helper.ui.tools.buttoninfo.on"));
				}
			});
			if (showButtonInfo) {
				buttonInfoButton.setMessage(Locals.localize("helper.ui.tools.buttoninfo.on"));
			}
			buttonInfoButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.tools.buttoninfo.desc"), "%n%"));
			toolsMenu.addContent(buttonInfoButton);
			
			CustomizationButton toolsTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.tools"), true, (press) -> {
				toolsMenu.setParentButton((AdvancedButton) press);
				toolsMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(toolsTab, "fm.ui.tab.tools", ElementAlignment.LEFT, false);
			/** TOOLS TAB END **/
			
			/** MISCELLANEOUS TAB START **/
			FMContextMenu miscMenu = new FMContextMenu();
			miscMenu.setAutoclose(true);
			bar.addChild(miscMenu, "fm.ui.tab.misc", ElementAlignment.LEFT);
			
			CustomizationButton closeGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.closegui"), true, (press) -> {
				MinecraftClient.getInstance().openScreen(null);
			});
			closeGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			miscMenu.addContent(closeGuiButton);
			
			CustomizationButton openWorldLoadingScreenButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.openworldloading"), true, (press) -> {
				LevelLoadingScreen wl = new LevelLoadingScreen(new WorldGenerationProgressTracker(0));
				MinecraftClient.getInstance().openScreen(wl);
			});
			openWorldLoadingScreenButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.openworldloading.desc"), "%n%"));
			miscMenu.addContent(openWorldLoadingScreenButton);
			
			CustomizationButton openMessageScreenButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc.openmessagescreen"), true, (press) -> {
				MinecraftClient.getInstance().openScreen(new SaveLevelScreen(new LiteralText("hello ・ω・")));
			});
			openMessageScreenButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.openmessagescreen.desc"), "%n%"));
			miscMenu.addContent(openMessageScreenButton);

			CustomizationButton miscTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.misc"), true, (press) -> {
				miscMenu.setParentButton((AdvancedButton) press);
				miscMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(miscTab, "fm.ui.tab.misc", ElementAlignment.LEFT, false);
			/** MISCELLANEOUS TAB END **/
			
			/** CLOSE GUI BUTTON START **/
			AdvancedImageButton closeGuiButtonTab = new AdvancedImageButton(20, 20, 20, 20, CLOSE_BUTTON_TEXTURE, true, (press) -> {
				MinecraftClient.getInstance().openScreen(null);
			}) {
				@Override
				public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.render(matrix, mouseX, mouseY, partialTicks);
				}
			};
			closeGuiButtonTab.ignoreLeftMouseDownClickBlock = true;
			closeGuiButtonTab.ignoreBlockedInput = true;
			closeGuiButtonTab.enableRightclick = true;
			closeGuiButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.misc.closegui.desc"), "%n%"));
			bar.addElement(closeGuiButtonTab, "fm.ui.tab.closegui", ElementAlignment.RIGHT, false);
			/** CLOSE GUI BUTTON END **/
			
			/** RELOAD BUTTON START **/
			AdvancedImageButton reloadButtonTab = new AdvancedImageButton(20, 20, 20, 20, RELOAD_BUTTON_TEXTURE, true, (press) -> {
				CustomizationHelper.reloadSystemAndMenu();
			}) {
				@Override
				public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.render(matrix, mouseX, mouseY, partialTicks);
				}
			};
			reloadButtonTab.ignoreLeftMouseDownClickBlock = true;
			reloadButtonTab.ignoreBlockedInput = true;
			reloadButtonTab.enableRightclick = true;
			reloadButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.reload.desc"), "%n%"));
			bar.addElement(reloadButtonTab, "fm.ui.tab.reload", ElementAlignment.RIGHT, false);
			/** RELOAD BUTTON END **/
			
			AdvancedButton expandButton = bar.getElement("menubar.default.extendbtn");
			if (expandButton != null) {
				if (expandButton instanceof AdvancedImageButton) {
					if (!extended) {
						((AdvancedImageButton)expandButton).setImage(MenuBar.EXPAND_BTN_TEXTURE);
						expandButton.setDescription(Locals.localize("helper.menubar.expand"));
					}
				}
			}
			
			bar.setExtended(extended);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void render(MatrixStack matrix, Screen screen) {
		try {
			
			if (bar != null) {
				if (!PopupHandler.isPopupActive()) { 
					if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
						if (!(screen instanceof LayoutEditorScreen) && !(screen instanceof ConfigScreen) && !(screen instanceof GameIntroScreen) && AnimationHandler.isReady() && MenuCustomization.isValidScreen(screen)) {
							
							RenderUtils.setZLevelPre(matrix, 400);
							
							renderMenuInfo(matrix, screen);
							
							renderUnicodeWarning(matrix, screen);
							
							renderButtonInfo(matrix, screen);
							
							RenderUtils.setZLevelPost(matrix);
							
							bar.render(matrix, screen);
							
						}
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static void renderButtonInfo(MatrixStack matrix, Screen screen) {
		if (showButtonInfo) {
			for (ButtonData d : buttons) {
				if (d.getButton().isHovered()) {
					long id = d.getId();
					String idString = Locals.localize("helper.buttoninfo.idnotfound");
					if (id >= 0) {
						idString = String.valueOf(id);
					}
					String key = ButtonCache.getKeyForButton(d.getButton());
					if (key == null) {
						key = Locals.localize("helper.buttoninfo.keynotfound");
					}
					
					List<String> info = new ArrayList<String>();
					int width = MinecraftClient.getInstance().textRenderer.getWidth(Locals.localize("helper.button.buttoninfo")) + 10;
					
					info.add("§f" + Locals.localize("helper.buttoninfo.id") + ": " + idString);
					info.add("§f" + Locals.localize("general.width") + ": " + d.getButton().getWidth());
					info.add("§f" + Locals.localize("general.height") + ": " + d.getButton().getHeight());
					info.add("§f" + Locals.localize("helper.buttoninfo.labelwidth") + ": " + MinecraftClient.getInstance().textRenderer.getWidth(d.getButton().getMessage().getString()));
					
					for (String s : info) {
						int i = MinecraftClient.getInstance().textRenderer.getWidth(s) + 10;
						if (i > width) {
							width = i;
						}
					}
					
					matrix.push();
					
					matrix.scale(getUIScale(), getUIScale(), getUIScale());
					
					MouseInput.setRenderScale(getUIScale());
					
					int x = MouseInput.getMouseX();
					if ((screen.width / getUIScale()) < x + width + 10) {
						x -= width + 10;
					}
					
					int y = MouseInput.getMouseY();
					if ((screen.height / getUIScale()) < y + 80) {
						y -= 90;
					}
					
					fill(matrix, x, y, x + width + 10, y + 80, new Color(102, 0, 102, 200).getRGB());
					
					RenderSystem.enableBlend();
					drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, "§f§l" + Locals.localize("helper.button.buttoninfo"), x + 10, y + 10, 0);

					int i2 = 20;
					for (String s : info) {
						drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, s, x + 10, y + 10 + i2, 0);
						i2 += 10;
					}
					
					MouseInput.resetRenderScale();
					
					matrix.pop();
					
					RenderSystem.disableBlend();
					
					break;
				}
			}
		}
	}
	
	protected static void renderMenuInfo(MatrixStack matrix, Screen screen) {
		if (showMenuInfo) {
			String infoTitle = "§f§l" + Locals.localize("helper.menuinfo.identifier") + ":";
			String id = "";
			if (screen instanceof CustomGuiBase) {
				id = ((CustomGuiBase)screen).getIdentifier();
			} else {
				id = screen.getClass().getName();
			}
			int w = MinecraftClient.getInstance().textRenderer.getWidth(infoTitle);
			int w2 = MinecraftClient.getInstance().textRenderer.getWidth(id);
			if (w2 > w) {
				w = w2;
			}
			int h = bar.getHeight() + 5;
			
			RenderSystem.enableBlend();
			
			matrix.push();
			
			matrix.scale(getUIScale(), getUIScale(), getUIScale());
			
			fill(matrix, 3, h, 3 + w + 4, h + 23, new Color(0, 0, 0, 240).getRGB());

			drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, infoTitle, 5, h + 2, 0);
			if (tick == 0) {
				drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, "§f" + id, 5, h + 13, 0);
			} else {
				drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, "§a" + Locals.localize("helper.menuinfo.idcopied"), 5, h + 13, 0);
			}

			MouseInput.setRenderScale(getUIScale());
			
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			if (!bar.isChildOpen()) {
				if ((mouseX >= 5) && (mouseX <= 5 + w2) && (mouseY >= h + 13) && (mouseY <= h + 13 + 10) && (tick == 0)) {
					fill(matrix, 5, h + 13 + 10 - 1, 5 + w2, h + 13 + 10, -1);
					
					if (MouseInput.isLeftMouseDown()) {
						tick++;
						MinecraftClient.getInstance().keyboard.setClipboard(id);
					}
				}
			}
			if (tick > 0) {
				if (tick < 60) {
					tick++;
				} else {
					tick = 0;
				}
			}
			
			MouseInput.resetRenderScale();
			
			matrix.pop();
			
			RenderSystem.disableBlend();
		}
	}
	
	protected static void renderUnicodeWarning(MatrixStack matrix, Screen screen) {
		if (MinecraftClient.getInstance().options.forceUnicodeFont) {
			String title = Locals.localize("helper.ui.warning");
			int w = MinecraftClient.getInstance().textRenderer.getWidth(title);
			String[] lines = StringUtils.splitLines(Locals.localize("helper.ui.warning.unicode"), "%n%");
			for (String s : lines) {
				int w2 = MinecraftClient.getInstance().textRenderer.getWidth(s);
				if (w2 > w) {
					w = w2;
				}
			}
			
			int x = screen.width - w - 5;
			int y = (int) ((bar.getHeight() + 5) * UIBase.getUIScale());
			
			RenderSystem.enableBlend();
			
			int h = 13;
			if (lines.length > 0) {
				h += 10*lines.length;
			}
			fill(matrix, x - 4, y, x + w + 2, y + h, new Color(230, 15, 0, 240).getRGB());

			drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, title, x, y + 2, Color.WHITE.getRGB());
			
			int i = 0;
			for (String s : lines) {
				drawStringWithShadow(matrix, MinecraftClient.getInstance().textRenderer, s, x, y + 13 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			RenderSystem.disableBlend();
		}
	}
	
	@SubscribeEvent
	public void onButtonsCached(ButtonCachedEvent e) {
		buttons = e.getButtonDataList();
	}
	
	@SubscribeEvent
	public void onInitScreen(GuiScreenEvent.InitGuiEvent.Pre e) {
		try {
			
			if (e.getGui() != null) {
				if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
					
					updateUI();
					
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static class ManageCustomGuiContextMenu extends FMContextMenu {

		private ManageCustomGuiSubContextMenu manageMenu;

		public ManageCustomGuiContextMenu() {
			
			this.manageMenu = new ManageCustomGuiSubContextMenu();
			this.addChild(this.manageMenu);

		}

		@Override
		public void openMenuAt(int x, int y) {

			this.content.clear();

			List<String> l = CustomGuiLoader.getCustomGuis();
			if (!l.isEmpty()) {

				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.openbyname"), true, (press) -> {
					PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.openbyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								MinecraftClient.getInstance().openScreen(CustomGuiLoader.getGui(call, MinecraftClient.getInstance().currentScreen, null));
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));

				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.deletebyname"), true, (press) -> {
					PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.deletebyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
									if (call2) {
										if (CustomGuiLoader.guiExists(call)) {
											List<File> delete = new ArrayList<File>();
											for (String s : FileUtils.getFiles(FancyMenu.getCustomGuiPath().getPath())) {
												File f = new File(s);
												for (String s2 : FileUtils.getFileLines(f)) {
													if (s2.replace(" ", "").toLowerCase().equals("identifier=" + call)) {
														delete.add(f);
													}
												}
											}

											for (File f : delete) {
												if (f.isFile()) {
													f.delete();
												}
											}

											CustomizationHelper.reloadSystemAndMenu();
										}
									}
								}, Locals.localize("helper.buttons.tools.customguis.sure")));
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));
				
				this.addSeparator();

				for (String s : l) {
					String label = s;
					if (MinecraftClient.getInstance().textRenderer.getWidth(label) > 80) {
						label = MinecraftClient.getInstance().textRenderer.trimToWidth(label, 75) + "..";
					}

					this.addContent(new CustomizationButton(0, 0, 0, 0, label, true, (press) -> {
						this.manageMenu.setParentButton((AdvancedButton) press);
						this.manageMenu.openMenuAt(0, press.y, s);
					}));
				}

			}
			
			super.openMenuAt(x, y);

		}
		
	}
	
	private static class ManageCustomGuiSubContextMenu extends FMContextMenu {

		public void openMenuAt(int x, int y, String customGuiIdentifier) {
			this.content.clear();
			
			CustomizationButton openMenuButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.open"), (press) -> {
				if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
					MinecraftClient.getInstance().openScreen(CustomGuiLoader.getGui(customGuiIdentifier, MinecraftClient.getInstance().currentScreen, null));
				}
			});
			this.addContent(openMenuButton);

			CustomizationButton deleteMenuButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.delete"), (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
							List<File> delete = new ArrayList<File>();
							for (String s : FileUtils.getFiles(FancyMenu.getCustomGuiPath().getPath())) {
								File f = new File(s);
								for (String s2 : FileUtils.getFileLines(f)) {
									if (s2.replace(" ", "").toLowerCase().equals("identifier=" + customGuiIdentifier)) {
										delete.add(f);
									}
								}
							}

							for (File f : delete) {
								if (f.isFile()) {
									f.delete();
								}
							}

							CustomizationHelper.reloadSystemAndMenu();
						}
					}
				}, Locals.localize("helper.buttons.tools.customguis.sure")));
			});
			this.addContent(deleteMenuButton);

			this.openMenuAt(x, y);
		}
		
	}

	private static class ManageLayoutsContextMenu extends FMContextMenu {

		private ManageLayoutsSubContextMenu manageSubPopup;
		
		public ManageLayoutsContextMenu() {
			
			this.manageSubPopup = new ManageLayoutsSubContextMenu();
			this.addChild(this.manageSubPopup);
			
		}

		public void openMenuAt(PressableWidget parentBtn) {
			this.content.clear();

			String identifier = MinecraftClient.getInstance().currentScreen.getClass().getName();
			if (MinecraftClient.getInstance().currentScreen instanceof CustomGuiBase) {
				identifier = ((CustomGuiBase) MinecraftClient.getInstance().currentScreen).getIdentifier();
			}
			
			List<PropertiesSet> enabled = MenuCustomizationProperties.getPropertiesWithIdentifier(identifier);
			if (!enabled.isEmpty()) {
				for (PropertiesSet s : enabled) {
					List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
					if (secs.isEmpty()) {
						secs = s.getPropertiesOfType("type-meta");
					}
					if (!secs.isEmpty()) {
						String name = "<missing name>";
						PropertiesSection meta = secs.get(0);
						File f = new File(meta.getEntryValue("path"));
						if (f.isFile()) {
							name = Files.getNameWithoutExtension(f.getName());
							
							int totalactions = s.getProperties().size() - 1;
							CustomizationButton layoutEntryBtn = new CustomizationButton(0, 0, 0, 0, "§a" + name, (press) -> {
								this.manageSubPopup.setParentButton((AdvancedButton) press);
								this.manageSubPopup.openMenuAt(0, press.y, f, false);
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.enabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}
			
			List<PropertiesSet> disabled = MenuCustomizationProperties.getDisabledPropertiesWithIdentifier(identifier);
			if (!disabled.isEmpty()) {
				for (PropertiesSet s : disabled) {
					List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
					if (secs.isEmpty()) {
						secs = s.getPropertiesOfType("type-meta");
					}
					if (!secs.isEmpty()) {
						String name = "<missing name>";
						PropertiesSection meta = secs.get(0);
						File f = new File(meta.getEntryValue("path"));
						if (f.isFile()) {
							name = Files.getNameWithoutExtension(f.getName());
							
							int totalactions = s.getProperties().size() - 1;
							CustomizationButton layoutEntryBtn = new CustomizationButton(0, 0, 0, 0, "§c" + name, (press) -> {
								this.manageSubPopup.setParentButton((AdvancedButton) press);
								this.manageSubPopup.openMenuAt(0, press.y, f, true);
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.disabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}
			
			if (enabled.isEmpty() && disabled.isEmpty()) {
				CustomizationButton emptyBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), (press) -> {});
				this.addContent(emptyBtn);
			}

			this.openMenuAt(parentBtn.x - this.getWidth() - 2, parentBtn.y);
		}
		
		@Override
		public void render(MatrixStack matrix, int mouseX, int mouseY) {
			super.render(matrix, mouseX, mouseY);
			
			if (this.manageSubPopup != null) {
				this.manageSubPopup.render(matrix, mouseX, mouseY);
				if (!this.isOpen()) {
					this.manageSubPopup.closeMenu();
				}
			}
		}
		
		@Override
		public void closeMenu() {
			if (!this.manageSubPopup.isHovered()) {
				super.closeMenu();
			}
		}
		
		@Override
		public boolean isHovered() {
			if (this.manageSubPopup.isOpen() && this.manageSubPopup.isHovered()) {
				return true;
			} else {
				return super.isHovered();
			}
		}
		
	}

	private static class ManageLayoutsSubContextMenu extends FMContextMenu {

		public void openMenuAt(int x, int y, File layout, boolean disabled) {
			
			this.content.clear();
			
			String toggleLabel = Locals.localize("helper.buttons.customization.managelayouts.disable");
			if (disabled) {
				toggleLabel = Locals.localize("helper.buttons.customization.managelayouts.enable");
			}
			CustomizationButton toggleLayoutBtn = new CustomizationButton(0, 0, 0, 0, toggleLabel, (press) -> {
				if (disabled) {
					String name = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationPath().getPath(), Files.getNameWithoutExtension(layout.getName()), "txt");
					FileUtils.copyFile(layout, new File(FancyMenu.getCustomizationPath().getPath() + "/" + name));
					layout.delete();
				} else {
					String disPath = FancyMenu.getCustomizationPath().getPath() + "/.disabled";
					String name = FileUtils.generateAvailableFilename(disPath, Files.getNameWithoutExtension(layout.getName()), "txt");
					FileUtils.copyFile(layout, new File(disPath + "/" + name));
					layout.delete();
				}
				CustomizationHelper.reloadSystemAndMenu();
			});
			this.addContent(toggleLayoutBtn);

			CustomizationButton editLayoutBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.ui.current.layouts.manage.edit"), (press) -> {
				CustomizationHelper.editLayout(MinecraftClient.getInstance().currentScreen, layout);
			});
			editLayoutBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.current.layouts.manage.edit.desc"), "%n%"));
			this.addContent(editLayoutBtn);
			
			CustomizationButton openInTextEditorBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.openintexteditor"), (press) -> {
				CustomizationHelper.openFile(layout);
			});
			openInTextEditorBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.openintexteditor.desc"), "%n%"));
			this.addContent(openInTextEditorBtn);
			
			CustomizationButton deleteLayoutBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.delete"), (press) -> {
				PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (layout.exists()) {
							layout.delete();
							CustomizationHelper.reloadSystemAndMenu();
						}
					}
				}, Locals.localize("helper.buttons.customization.managelayouts.delete.msg"), "", "", "", ""));
				CustomizationHelper.reloadSystemAndMenu();
			});
			this.addContent(deleteLayoutBtn);
			
			this.openMenuAt(x, y);
			
		}
	}
	
	private static class OverrideMenuContextMenu extends FMContextMenu {
		
		@Override
		public void openMenuAt(int x, int y) {
			
			this.content.clear();

			List<String> l = CustomGuiLoader.getCustomGuis();

			if (!l.isEmpty()) {

				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.pickbyname"), true, (press) -> {
					PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.pickbyname"), null, 240, (call) -> {
						if (call != null) {
							if (CustomGuiLoader.guiExists(call)) {
								onOverrideWithCustomGui(MinecraftClient.getInstance().currentScreen, call);
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
							}
						}
					}));
				}));
				
				this.addSeparator();
				
				for (String s : l) {
					String label = s;
					if (MinecraftClient.getInstance().textRenderer.getWidth(label) > 80) {
						label = MinecraftClient.getInstance().textRenderer.trimToWidth(label, 75) + "..";
					}

					this.addContent(new CustomizationButton(0, 0, 0, 0, label, true, (press) -> {
						onOverrideWithCustomGui(MinecraftClient.getInstance().currentScreen, s);
					}));

				}

			} else {
				this.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), true, (press) -> {}));
			}
			
			super.openMenuAt(x, y);
			
		}
		
	}

	private static void onOverrideWithCustomGui(Screen current, String customGuiIdentifier) {
		if ((customGuiIdentifier != null) && CustomGuiLoader.guiExists(customGuiIdentifier)) {
			PropertiesSection meta = new PropertiesSection("customization-meta");
			meta.addEntry("identifier", current.getClass().getName());

			PropertiesSection or = new PropertiesSection("customization");
			or.addEntry("action", "overridemenu");
			or.addEntry("identifier", customGuiIdentifier);

			PropertiesSet props = new PropertiesSet("menu");
			props.addProperties(meta);
			props.addProperties(or);

			String screenname = current.getClass().getName();
			if (screenname.contains(".")) {
				screenname = new StringBuilder(new StringBuilder(screenname).reverse().toString().split("[.]", 2)[0]).reverse().toString();
			}
			String filename = FileUtils.generateAvailableFilename(FancyMenu.getCustomizationPath().getPath(), "overridemenu_" + screenname, "txt");

			String finalpath = FancyMenu.getCustomizationPath().getPath() + "/" + filename;
			PropertiesSerializer.writeProperties(props, finalpath);

			CustomizationHelper.reloadSystemAndMenu();
		}
	}

}
