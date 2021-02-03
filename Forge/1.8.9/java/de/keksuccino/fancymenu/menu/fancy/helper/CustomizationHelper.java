package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;

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
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.PreloadedLayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.ContextMenu;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.gui.screens.popup.NotificationPopup;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.gui.screens.popup.YesNoPopup;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenRealmsProxy;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomizationHelper {
	
	private static CustomizationHelper instance;
	
	private AdvancedButton dropdownButton;
	private ContextMenu dropdown;
	private ContextMenu overridePopup;
	private ContextMenu customGuisPopup;
	private ManageCustomGuiPopupMenu manageCustomGuiPopup;
	private ManageLayoutsPopupMenu manageLayoutsPopup;
	private boolean showButtonInfo = false;
	private boolean showMenuInfo = false;
	private List<ButtonData> buttons = new ArrayList<ButtonData>();
	private AdvancedButton buttonInfoButton;
	private AdvancedButton menuInfoButton;
	private AdvancedButton reloadButton;
	private AdvancedButton overrideButton;
	private AdvancedButton customGuisButton;
	private AdvancedButton toggleCustomizationButton;
	private AdvancedButton manageLayoutsButton;
	private int tick = 0;
	
	public GuiScreen current;
	
	private Color menuinfoBackground = new Color(0, 0, 0, 240);
	
	public static void init() {
		instance = new CustomizationHelper();
		MinecraftForge.EVENT_BUS.register(instance);
	}
	
	public static CustomizationHelper getInstance() {
		return instance;
	}
	
	@SubscribeEvent
	public void onButtonsCached(ButtonCachedEvent e) {
		this.buttons = e.getButtonDataList();
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {

		if (this.dropdown != null) {
			this.dropdown.closeMenu();
		}
		MouseInput.unblockVanillaInput("customizationhelper");
		
		if (!isValidScreen(e.gui)) {
			return;
		}
		
		this.current = e.gui;
			
		String infoLabel = Locals.localize("helper.button.buttoninfo");
		if (this.showButtonInfo) {
			infoLabel = "§a" + Locals.localize("helper.button.buttoninfo");
		}
		AdvancedButton iButton = new CustomizationButton(5, 5, 70, 20, infoLabel, true, (onPress) -> {
			this.onInfoButtonPress();
		}); 
		this.buttonInfoButton = iButton;
		this.buttonInfoButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.buttoninfo.btndesc"), "%n%"));

		String minfoLabel = Locals.localize("helper.button.menuinfo");
		if (this.showMenuInfo) {
			minfoLabel = "§a" + Locals.localize("helper.button.menuinfo");
		}
		AdvancedButton miButton = new CustomizationButton(80, 5, 70, 20, minfoLabel, true, (onPress) -> {
			this.onMoreInfoButtonPress();
		});
		this.menuInfoButton = miButton;
		this.menuInfoButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.menuinfo.btndesc"), "%n%"));

		this.reloadButton = new CustomizationButton(e.gui.width - 55, 5, 50, 20, Locals.localize("helper.button.reload"), true, (onPress) -> {
			onReloadButtonPress();
		});
		this.reloadButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.reload.btndesc"), "%n%"));

		AdvancedButton layoutCreatorButton = new CustomizationButton(e.gui.width - 150, 5, 90, 20, Locals.localize("helper.button.createlayout"), true, (onPress) -> {
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
		layoutCreatorButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.createlayout.btndesc"), "%n%"));

		AdvancedButton editLayoutButton = new CustomizationButton(e.gui.width - 245, 5, 90, 20, Locals.localize("helper.creator.editlayout"), true, (onPress) -> {
			String identifier = this.current.getClass().getName();
			if (this.current instanceof CustomGuiBase) {
				identifier = ((CustomGuiBase) this.current).getIdentifier();
			}
			List<PropertiesSet> l = MenuCustomizationProperties.getPropertiesWithIdentifier(identifier);
			if (l.isEmpty()) {
				PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.creator.editlayout.nolayouts.msg")));
			}
			if (l.size() == 1) {
				if (!MenuCustomization.containsCalculations(l.get(0))) {
					Minecraft.getMinecraft().displayGuiScreen(new PreloadedLayoutCreatorScreen(this.current, l));
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
		editLayoutButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.editlayout.btndesc"), "%n%"));

		String overrLabel = Locals.localize("helper.buttons.tools.overridemenu");
		if (this.isScreenOverridden()) {
			overrLabel = Locals.localize("helper.buttons.tools.resetoverride");
		}
		this.overrideButton = new CustomizationButton(e.gui.width - 150, 5, 90, 20, overrLabel, true, (onPress) -> {
			if (!this.isScreenOverridden()) {
				this.overridePopup = new ContextMenu(100, 20, -1);

				List<String> l = CustomGuiLoader.getCustomGuis();

				if (!l.isEmpty()) {

					this.overridePopup.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.pickbyname"), true, (press) -> {
						PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.pickbyname"), null, 240, (call) -> {
							if (call != null) {
								if (CustomGuiLoader.guiExists(call)) {
									this.onOverrideWithCustomGui(call);
								} else {
									PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
								}
							}
						}));
					}));
					
					for (String s : l) {
						String label = s;
						if (Minecraft.getMinecraft().fontRendererObj.getStringWidth(label) > 80) {
							label = Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(label, 75) + "..";
						}

						this.overridePopup.addContent(new CustomizationButton(0, 0, 0, 0, label, true, (press) -> {
							this.onOverrideWithCustomGui(s);
						}));

					}

				} else {
					this.overridePopup.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), true, (press) -> {}));
				}

				this.overridePopup.openMenuAt(onPress.xPosition - this.overridePopup.getWidth() - 2, onPress.yPosition);

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
						GuiScreen overridden = ((CustomGuiBase)this.current).getOverriddenScreen();
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

				this.onReloadButtonPress();
				if (this.current instanceof CustomGuiBase) {
					Minecraft.getMinecraft().displayGuiScreen(((CustomGuiBase) this.current).getOverriddenScreen());
				}
			}
		});
		overrideButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.overridewith.btndesc"), "%n%"));

		AdvancedButton createGuiButton = new CustomizationButton(e.gui.width - 55, 5, 50, 20, Locals.localize("helper.buttons.tools.creategui"), true, (onPress) -> {
			PopupHandler.displayPopup(new CreateCustomGuiPopup());
		});
		createGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.creategui.btndesc"), "%n%"));

		this.manageCustomGuiPopup = new ManageCustomGuiPopupMenu(100, 20, -1);
		this.customGuisPopup = new ContextMenu(100, 20, -1);
		List<String> l = CustomGuiLoader.getCustomGuis();
		if (!l.isEmpty()) {
			
			this.customGuisPopup.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.openbyname"), true, (press) -> {
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.openbyname"), null, 240, (call) -> {
					if (call != null) {
						if (CustomGuiLoader.guiExists(call)) {
							Minecraft.getMinecraft().displayGuiScreen(CustomGuiLoader.getGui(call, Minecraft.getMinecraft().currentScreen, null));
						} else {
							PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
						}
					}
				}));
			}));
			
			this.customGuisPopup.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.deletebyname"), true, (press) -> {
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.buttons.tools.customguis.deletebyname"), null, 240, (call) -> {
					if (call != null) {
						if (CustomGuiLoader.guiExists(call)) {
							CustomizationHelper.getInstance().dropdown.closeMenu();
							PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
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

										CustomizationHelper.getInstance().onReloadButtonPress();
									}
								}
							}, Locals.localize("helper.buttons.tools.customguis.sure")));
						} else {
							PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.buttons.tools.customguis.invalididentifier")));
						}
					}
				}));
			}));
			
			for (String s : l) {
				String label = s;
				if (Minecraft.getMinecraft().fontRendererObj.getStringWidth(label) > 80) {
					label = Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(label, 75) + "..";
				}

				this.customGuisPopup.addContent(new CustomizationButton(0, 0, 0, 0, label, true, (press) -> {
					this.manageCustomGuiPopup.openMenuAt(press.xPosition - this.manageCustomGuiPopup.getWidth() - 2, press.yPosition, s);
				}));
			}
		} else {
			this.customGuisPopup.addContent(new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), true, (press) -> {}));
		}

		this.customGuisButton = new CustomizationButton(e.gui.width - 55, 5, 50, 20, Locals.localize("helper.buttons.tools.customguis"), true, (press) -> {
			this.customGuisPopup.openMenuAt(press.xPosition - this.customGuisPopup.getWidth() - 2, press.yPosition);
		});
		customGuisButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.customguis.btndesc"), "%n%"));
		
		AdvancedButton closeCustomGuiButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.closecustomgui"), (press) -> {
			if (e.gui instanceof CustomGuiBase) {
				((CustomGuiBase)e.gui).onClose();
			}
		});
		closeCustomGuiButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.closecustomgui.btndesc"), "%n%"));

		String toggleLabel = Locals.localize("helper.popup.togglecustomization.enable");
		if (MenuCustomization.isMenuCustomizable(e.gui)) {
			toggleLabel = Locals.localize("helper.popup.togglecustomization.disable");
		}
		this.toggleCustomizationButton = new CustomizationButton(0, 0, 0, 0, toggleLabel, (press) -> {
			if (MenuCustomization.isMenuCustomizable(e.gui)) {
				press.displayString = Locals.localize("helper.popup.togglecustomization.enable");
				MenuCustomization.disableCustomizationForMenu(e.gui);
				onReloadButtonPress();
			} else {
				press.displayString = Locals.localize("helper.popup.togglecustomization.disable");
				MenuCustomization.enableCustomizationForMenu(e.gui);
				onReloadButtonPress();
			}
		});
		toggleCustomizationButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.onoff.btndesc"), "%n%"));

		this.manageLayoutsPopup = new ManageLayoutsPopupMenu(0, 20, -1);
		this.manageLayoutsButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts"), (press) -> {
			this.manageLayoutsPopup.openMenuAt(press);
		});
		manageLayoutsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.btndesc"), "%n%"));
		
		if (!MenuCustomization.isMenuCustomizable(e.gui)) {
			iButton.enabled = false;
			layoutCreatorButton.enabled = false;
			editLayoutButton.enabled = false;
			overrideButton.enabled = false;
			manageLayoutsButton.enabled = false;
		}

		this.dropdown = new ContextMenu(120, 20, -1);

		if (!(e.gui instanceof CustomGuiBase)) {
			this.dropdown.addContent(toggleCustomizationButton);
		}
		this.dropdown.addContent(miButton);
		this.dropdown.addContent(iButton);
		this.dropdown.addContent(layoutCreatorButton);
		this.dropdown.addContent(editLayoutButton);
		this.dropdown.addContent(manageLayoutsButton);
		this.dropdown.addContent(createGuiButton);
		this.dropdown.addContent(customGuisButton);
		if (this.isScreenOverridden()) {
			this.dropdown.addContent(overrideButton);
		} else if (!(e.gui instanceof CustomGuiBase)) {
			this.dropdown.addContent(overrideButton);
		} else {
			this.dropdown.addContent(closeCustomGuiButton);
		}

		this.dropdownButton = new CustomizationButton(e.gui.width - 180, 5, 120, 20, Locals.localize("helper.buttons.tools.dropdownlabel"), true, (press) -> {
			if (!this.dropdown.isOpen()) {
				this.dropdown.openMenuAt(press.xPosition, press.yPosition + press.height - 1);
			} else {
				this.dropdown.closeMenu();
			}
		});
		this.dropdownButton.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.btndesc"), "%n%"));
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (PopupHandler.isPopupActive()) {
			return;
		}
		if (!isValidScreen(e.gui)) {
			return;
		}

		if (FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
			this.dropdownButton.drawButton(Minecraft.getMinecraft(), e.mouseX, e.mouseY);
			this.reloadButton.drawButton(Minecraft.getMinecraft(), e.mouseX, e.mouseY);
			
			this.dropdown.render(e.mouseX, e.mouseY);
			
			if (this.dropdown.isOpen()) {
				MouseInput.blockVanillaInput("customizationhelper");
			} else {
				MouseInput.unblockVanillaInput("customizationhelper");
			}
			
			if (this.dropdown.isOpen() && !this.customGuisPopup.isHovered() && !this.manageLayoutsPopup.isHovered() && !this.dropdownButton.isMouseOver() && !this.dropdown.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
				this.dropdown.closeMenu();
			}
			if (this.overridePopup != null) {
				this.overridePopup.render(e.mouseX, e.mouseY);
				if (this.overridePopup.isOpen() && !this.overrideButton.isMouseOver() && !this.overridePopup.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
					this.overridePopup.closeMenu();
				}
				if (!this.dropdown.isOpen()) {
					this.overridePopup.closeMenu();
				}
			}
			if (this.customGuisPopup != null) {
				this.customGuisPopup.render(e.mouseX, e.mouseY);
				if (this.customGuisPopup.isOpen() && !this.customGuisButton.isMouseOver() && !this.customGuisPopup.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
					this.customGuisPopup.closeMenu();
				}
				if (!this.dropdown.isOpen()) {
					this.customGuisPopup.closeMenu();
				}
				if (this.manageCustomGuiPopup != null) {
					this.manageCustomGuiPopup.render(e.mouseX, e.mouseY);
					if (!this.customGuisPopup.isOpen()) {
						this.manageCustomGuiPopup.closeMenu();
					}
				}
			}
			if (this.manageLayoutsPopup != null) {
				this.manageLayoutsPopup.render(e.mouseX, e.mouseY);
				if (this.manageLayoutsPopup.isOpen() && !this.manageLayoutsPopup.isHovered() && !this.manageLayoutsButton.isMouseOver() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
					this.manageLayoutsPopup.closeMenu();
				}
				if (!this.dropdown.isOpen()) {
					this.manageLayoutsPopup.closeMenu();
				}
			}
		}
		
		if (this.showMenuInfo && !(e.gui instanceof LayoutCreatorScreen)) {
			String infoTitle = "§f§l" + Locals.localize("helper.menuinfo.identifier") + ":";
			String id = "";
			if (e.gui instanceof CustomGuiBase) {
				id = ((CustomGuiBase)e.gui).getIdentifier();
			} else {
				id = e.gui.getClass().getName();
			}
			int w = Minecraft.getMinecraft().fontRendererObj.getStringWidth(infoTitle);
			int w2 = Minecraft.getMinecraft().fontRendererObj.getStringWidth(id);
			if (w2 > w) {
				w = w2;
			}
			
			GlStateManager.enableBlend();
			
			Gui.drawRect(3, 3, 3 + w + 4, 25, this.menuinfoBackground.getRGB());
			
			e.gui.drawString(Minecraft.getMinecraft().fontRendererObj, infoTitle, 5, 5, 0);
			if (tick == 0) {
				e.gui.drawString(Minecraft.getMinecraft().fontRendererObj, "§f" + id, 5, 15, 0);
			} else {
				e.gui.drawString(Minecraft.getMinecraft().fontRendererObj, "§a" + Locals.localize("helper.menuinfo.idcopied"), 5, 15, 0);
			}
			
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			if ((mouseX >= 5) && (mouseX <= 5 + w2) && (mouseY >= 15) && (mouseY <= 15 + 10) && (tick == 0)) {
				Gui.drawRect(5, 15 + 10 - 1, 5 + w2, 15 + 10, -1);
				
				if (MouseInput.isLeftMouseDown()) {
					tick++;
					GuiScreen.setClipboardString(id);
				}
			}
			if (tick > 0) {
				if (tick < 60) {
					tick++;
				} else {
					tick = 0;
				}
			}
			
			GlStateManager.disableBlend();
		}

		if (this.showButtonInfo) {
			for (ButtonData d : this.buttons) {
				if (d.getButton().isMouseOver()) {
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
					int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(Locals.localize("helper.button.buttoninfo")) + 10;
					
					info.add("§f" + Locals.localize("helper.buttoninfo.id") + ": " + idString);
					info.add("§f" + Locals.localize("helper.buttoninfo.key") + ": " + key);
					info.add("§f" + Locals.localize("general.width") + ": " + d.getButton().width);
					info.add("§f" + Locals.localize("general.height") + ": " + d.getButton().height);
					info.add("§f" + Locals.localize("helper.buttoninfo.labelwidth") + ": " + Minecraft.getMinecraft().fontRendererObj.getStringWidth(d.getButton().displayString));
					
					//Getting the longest string from the list to render the background with the correct width
					for (String s : info) {
						int i = Minecraft.getMinecraft().fontRendererObj.getStringWidth(s) + 10;
						if (i > width) {
							width = i;
						}
					}
					
					int x = e.mouseX;
					if (e.gui.width < x + width + 10) {
						x -= width + 10;
					}
					
					int y = e.mouseY;
					if (e.gui.height < y + 90) {
						y -= 90;
					}
					
					drawInfoBackground(x, y, width + 10, 90);
					
					GlStateManager.enableBlend();
					e.gui.drawString(Minecraft.getMinecraft().fontRendererObj, "§f§l" + Locals.localize("helper.button.buttoninfo"), x + 10, y + 10, 0);

					int i2 = 20;
					for (String s : info) {
						e.gui.drawString(Minecraft.getMinecraft().fontRendererObj, s, x + 10, y + 10 + i2, 0);
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
	
	public void updateCustomizationButtons() {
		GuiScreen current = Minecraft.getMinecraft().currentScreen;
		if (current != null) {
			Minecraft.getMinecraft().displayGuiScreen(current);
		}
	}
	
	public void onInfoButtonPress() {
		if (this.showButtonInfo) {
			this.showButtonInfo = false;
			this.buttonInfoButton.displayString = Locals.localize("helper.button.buttoninfo");
			
		} else {
			this.showButtonInfo = true;
			this.buttonInfoButton.displayString = "§a" + Locals.localize("helper.button.buttoninfo");
		}
	}
	
	public void onMoreInfoButtonPress() {
		if (this.showMenuInfo) {
			this.showMenuInfo = false;
			this.menuInfoButton.displayString = Locals.localize("helper.button.menuinfo");
			
		} else {
			this.showMenuInfo = true;
			this.menuInfoButton.displayString = "§a" + Locals.localize("helper.button.menuinfo");
		}
	}

	public void onReloadButtonPress() {
		FancyMenu.updateConfig();
		MenuCustomization.resetSounds();
		MenuCustomization.reload();
		MenuHandlerRegistry.setActiveHandler(null);
		CustomGuiLoader.loadCustomGuis();
		if (!FancyMenu.config.getOrDefault("showcustomizationbuttons", true)) {
			this.showButtonInfo = false;
			this.showMenuInfo = false;
		}
		
		MinecraftForge.EVENT_BUS.post(new MenuReloadedEvent(Minecraft.getMinecraft().currentScreen));
		
		try {
			Minecraft.getMinecraft().displayGuiScreen(Minecraft.getMinecraft().currentScreen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean isScreenOverridden() {
		if ((this.current != null) && (this.current instanceof CustomGuiBase) && (((CustomGuiBase)this.current).getOverriddenScreen() != null)) {
			return true;
		}
		return false;
	}
	
	private void onOverrideWithCustomGui(String customGuiIdentifier) {
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

			this.onReloadButtonPress();
		}
	}

	private static class ManageCustomGuiPopupMenu extends ContextMenu {

		public ManageCustomGuiPopupMenu(int width, int buttonHeight, int space) {
			super(width, buttonHeight, space);
		}

		public void openMenuAt(int x, int y, String customGuiIdentifier) {
			this.content.clear();

			CustomizationButton openMenuButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.open"), (press) -> {
				if (CustomGuiLoader.guiExists(customGuiIdentifier)) {
					Minecraft.getMinecraft().displayGuiScreen(CustomGuiLoader.getGui(customGuiIdentifier, Minecraft.getMinecraft().currentScreen, null));
				}
			});
			this.addContent(openMenuButton);

			CustomizationButton deleteMenuButton = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.tools.customguis.delete"), (press) -> {
				CustomizationHelper.getInstance().dropdown.closeMenu();
				PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
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

							CustomizationHelper.getInstance().onReloadButtonPress();
						}
					}
				}, Locals.localize("helper.buttons.tools.customguis.sure")));
			});
			this.addContent(deleteMenuButton);

			this.openMenuAt(x, y);
		}
	}
	
	private static class ManageLayoutsPopupMenu extends ContextMenu {

		private ManageLayoutsSubPopupMenu manageSubPopup;
		
		public ManageLayoutsPopupMenu(int width, int buttonHeight, int space) {
			super(width, buttonHeight, space);
			
			this.manageSubPopup = new ManageLayoutsSubPopupMenu(120, 20, -1);
		}

		public void openMenuAt(GuiButton parentBtn) {
			this.content.clear();

			String identifier = Minecraft.getMinecraft().currentScreen.getClass().getName();
			if (Minecraft.getMinecraft().currentScreen instanceof CustomGuiBase) {
				identifier = ((CustomGuiBase) Minecraft.getMinecraft().currentScreen).getIdentifier();
			}
			
			int finalwidth = 80;
			
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
							name = f.getName();
							int namewidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(name);
							if (namewidth + 5 > finalwidth) {
								finalwidth = namewidth + 5;
							}
							
							int totalactions = s.getProperties().size() - 1;
							CustomizationButton layoutEntryBtn = new CustomizationButton(0, 0, 0, 0, "§a" + name, (press) -> {
								this.manageSubPopup.openMenuAt(press.xPosition - this.manageSubPopup.getWidth() - 2, press.yPosition, f, false);
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
							name = f.getName();
							int namewidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(name);
							if (namewidth + 5 > finalwidth) {
								finalwidth = namewidth + 5;
							}
							
							int totalactions = s.getProperties().size() - 1;
							CustomizationButton layoutEntryBtn = new CustomizationButton(0, 0, 0, 0, "§c" + name, (press) -> {
								this.manageSubPopup.openMenuAt(press.xPosition - this.manageSubPopup.getWidth() - 2, press.yPosition, f, true);
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
			
			this.setWidth(finalwidth);

			this.openMenuAt(parentBtn.xPosition - this.getWidth() - 2, parentBtn.yPosition);
		}
		
		@Override
		public void render(int mouseX, int mouseY) {
			super.render(mouseX, mouseY);
			
			if (this.manageSubPopup != null) {
				this.manageSubPopup.render(mouseX, mouseY);
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

	private static class ManageLayoutsSubPopupMenu extends ContextMenu {

		public ManageLayoutsSubPopupMenu(int width, int buttonHeight, int space) {
			super(width, buttonHeight, space);
		}

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
				CustomizationHelper.getInstance().onReloadButtonPress();
			});
			this.addContent(toggleLayoutBtn);
			
			CustomizationButton deleteLayoutBtn = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.buttons.customization.managelayouts.delete"), (press) -> {
				PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						if (layout.exists()) {
							layout.delete();
							CustomizationHelper.getInstance().onReloadButtonPress();
						}
					}
				}, Locals.localize("helper.buttons.customization.managelayouts.delete.msg"), "", "", "", ""));
				CustomizationHelper.getInstance().onReloadButtonPress();
			});
			this.addContent(deleteLayoutBtn);
			
			this.openMenuAt(x, y);
			
		}
	}
	
}
