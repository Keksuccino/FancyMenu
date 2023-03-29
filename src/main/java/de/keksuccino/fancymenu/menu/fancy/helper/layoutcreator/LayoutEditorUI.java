package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.io.Files;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.CustomizationItemRegistry;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationButton;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.WindowSizePopup.ActionType;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.BackgroundOptionsPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutSplashText;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.loadingrequirement.ManageRequirementsScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ChooseFromStringListScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.ShapeCustomizationItem.Shape;
import de.keksuccino.fancymenu.menu.fancy.item.SplashTextCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.MenuBar.ElementAlignment;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LayoutEditorUI extends UIBase {

	public MenuBar bar;
	public LayoutEditorScreen parent;

	protected int tick = 0;

	protected static final ResourceLocation CLOSE_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "close_btn.png");

	public LayoutEditorUI(LayoutEditorScreen parent) {
		this.parent = parent;
		this.updateUI();
	}

	public void updateUI() {
		try {

			boolean extended = true;
			if (bar != null) {
				extended = bar.isExtended();
			}

			bar = new MenuBar();
			bar.setExtended(extended);

			/** LAYOUT TAB **/
			FMContextMenu layoutMenu = new FMContextMenu();
			layoutMenu.setAutoclose(true);
			bar.addChild(layoutMenu, "fm.editor.ui.tab.layout", ElementAlignment.LEFT);

			AdvancedButton newLayoutButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.layout.new"), true, (press) -> {
				this.displayUnsavedWarning((call) -> {
					if (call) {
						MenuCustomization.stopSounds();
						MenuCustomization.resetSounds();
						Minecraft.getInstance().setScreen(new LayoutEditorScreen(this.parent.screen));
					}
				});
			});
			layoutMenu.addContent(newLayoutButton);

			OpenLayoutContextMenu openLayoutMenu = new OpenLayoutContextMenu(this);
			openLayoutMenu.setAutoclose(true);
			layoutMenu.addChild(openLayoutMenu);

			AdvancedButton openLayoutButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.layout.open"), true, (press) -> {
				openLayoutMenu.setParentButton((AdvancedButton) press);
				openLayoutMenu.openMenuAt(0, press.y);
			});
			layoutMenu.addContent(openLayoutButton);

			AdvancedButton layoutSaveButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.layout.save"), true, (press) -> {
				this.parent.saveLayout();
			});
			layoutMenu.addContent(layoutSaveButton);

			AdvancedButton layoutSaveAsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.layout.saveas"), true, (press) -> {
				this.parent.saveLayoutAs();
			});
			layoutMenu.addContent(layoutSaveAsButton);

			LayoutPropertiesContextMenu layoutPropertiesMenu = new LayoutPropertiesContextMenu(this.parent, false);
			layoutPropertiesMenu.setAutoclose(true);
			layoutMenu.addChild(layoutPropertiesMenu);

			AdvancedButton layoutPropertiesButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.layout.properties"), true, (press) -> {
				layoutPropertiesMenu.setParentButton((AdvancedButton) press);
				layoutPropertiesMenu.openMenuAt(0, press.y);
			});
			layoutMenu.addContent(layoutPropertiesButton);

			AdvancedButton exitButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.exit"), true, (press) -> {
				this.closeEditor();
			});
			layoutMenu.addContent(exitButton);

			CustomizationButton layoutTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.layout"), true, (press) -> {
				layoutMenu.setParentButton((AdvancedButton) press);
				layoutMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(layoutTab, "fm.editor.ui.tab.layout", ElementAlignment.LEFT, false);

			/** EDIT TAB **/
			FMContextMenu editMenu = new FMContextMenu();
			editMenu.setAutoclose(true);
			bar.addChild(editMenu, "fm.editor.ui.tab.edit", ElementAlignment.LEFT);

			AdvancedButton undoButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.undo"), true, (press) -> {
				this.parent.history.stepBack();
				try {
					((LayoutEditorScreen)Minecraft.getInstance().screen).ui.bar.getChild("fm.editor.ui.tab.edit").openMenuAt(editMenu.getX(), editMenu.getY());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			editMenu.addContent(undoButton);

			AdvancedButton redoButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.redo"), true, (press) -> {
				this.parent.history.stepForward();
				try {
					((LayoutEditorScreen)Minecraft.getInstance().screen).ui.bar.getChild("fm.editor.ui.tab.edit").openMenuAt(editMenu.getX(), editMenu.getY());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			editMenu.addContent(redoButton);

			editMenu.addSeparator();

			AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.copy"), true, (press) -> {
				this.parent.copySelectedElements();
			});
			editMenu.addContent(copyButton);

			AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.paste"), true, (press) -> {
				this.parent.pasteElements();
			});
			editMenu.addContent(pasteButton);

			CustomizationButton editTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit"), true, (press) -> {
				editMenu.setParentButton((AdvancedButton) press);
				editMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(editTab, "fm.editor.ui.tab.edit", ElementAlignment.LEFT, false);

			/** ELEMENT TAB **/
			FMContextMenu elementMenu = new FMContextMenu();
			elementMenu.setAutoclose(true);
			bar.addChild(elementMenu, "fm.editor.ui.tab.element", ElementAlignment.LEFT);

			NewElementContextMenu newElementMenu = new NewElementContextMenu(this.parent);
			newElementMenu.setAutoclose(true);
			elementMenu.addChild(newElementMenu);

			AdvancedButton newElementButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.element.new"), true, (press) -> {
				newElementMenu.setParentButton((AdvancedButton) press);
				newElementMenu.openMenuAt(0, press.y);
			});
			elementMenu.addContent(newElementButton);

			ManageAudioContextMenu manageAudioMenu = new ManageAudioContextMenu(this.parent);
			manageAudioMenu.setAutoclose(true);
			elementMenu.addChild(manageAudioMenu);

			AdvancedButton manageAudioButton = new AdvancedButton(0, 0, 0, 0, "§m" + Locals.localize("helper.editor.ui.element.manageaudio"), true, (press) -> {
				manageAudioMenu.setParentButton((AdvancedButton) press);
				manageAudioMenu.openMenuAt(0, press.y);
			});
			manageAudioButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.extension.dummy.audio.manageaudio.btn.desc"), "%n%"));
			elementMenu.addContent(manageAudioButton);

			HiddenVanillaButtonContextMenu hiddenVanillaMenu = new HiddenVanillaButtonContextMenu(this.parent);
			hiddenVanillaMenu.setAutoclose(true);
			elementMenu.addChild(hiddenVanillaMenu);

			AdvancedButton hiddenVanillaButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.ui.element.deleted_vanilla_elements"), true, (press) -> {
				hiddenVanillaMenu.setParentButton((AdvancedButton) press);
				hiddenVanillaMenu.openMenuAt(0, press.y);
			});
			hiddenVanillaButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.ui.element.deleted_vanilla_elements.desc"), "%n%"));
			elementMenu.addContent(hiddenVanillaButton);

			CustomizationButton elementTab = new CustomizationButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.element"), true, (press) -> {
				elementMenu.setParentButton((AdvancedButton) press);
				elementMenu.openMenuAt(press.x, press.y + press.getHeight());
			});
			bar.addElement(elementTab, "fm.editor.ui.tab.element", ElementAlignment.LEFT, false);

			/** CLOSE GUI BUTTON TAB **/
			AdvancedImageButton exitEditorButtonTab = new AdvancedImageButton(20, 20, 0, 0, CLOSE_BUTTON_TEXTURE, true, (press) -> {
				this.closeEditor();
			}) {
				@Override
				public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
					this.width = this.height;
					super.render(matrix, mouseX, mouseY, partialTicks);
				}
			};
			exitEditorButtonTab.ignoreLeftMouseDownClickBlock = true;
			exitEditorButtonTab.ignoreBlockedInput = true;
			exitEditorButtonTab.enableRightclick = true;
			exitEditorButtonTab.setDescription(StringUtils.splitLines(Locals.localize("helper.editor.ui.exit.desc"), "%n%"));
			bar.addElement(exitEditorButtonTab, "fm.editor.ui.tab.exit", ElementAlignment.RIGHT, false);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void render(PoseStack matrix, Screen screen) {
		try {

			if (bar != null) {
				if (!PopupHandler.isPopupActive()) {
					if (screen instanceof LayoutEditorScreen) {

						bar.render(matrix, screen);

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void displayUnsavedWarning(Consumer<Boolean> callback) {
		PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, callback, Locals.localize("helper.editor.ui.unsavedwarning")));
	}

	public void closeEditor() {
		this.displayUnsavedWarning((call) -> {
			if (call) {
				LayoutEditorScreen.isActive = false;
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
				MenuCustomization.stopSounds();
				MenuCustomization.resetSounds();
				MenuCustomizationProperties.loadProperties();

				Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
				this.parent.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
				this.parent.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

				Screen s = this.parent.screen;
				if ((s instanceof CustomGuiBase) && ((CustomGuiBase)s).getIdentifier().equals("%fancymenu:universal_layout%")) {
					s = ((CustomGuiBase)s).parent;
				}
				Minecraft.getInstance().setScreen(s);
			}
		});
	}

	private static class OpenLayoutContextMenu extends FMContextMenu {

		private LayoutEditorUI ui;

		public OpenLayoutContextMenu(LayoutEditorUI ui) {
			this.ui = ui;
		}

		@Override
		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {

			this.content.clear();

			String identifier = this.ui.parent.screen.getClass().getName();
			if (this.ui.parent.screen instanceof CustomGuiBase) {
				identifier = ((CustomGuiBase) this.ui.parent.screen).getIdentifier();
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
							AdvancedButton layoutEntryBtn = new AdvancedButton(0, 0, 0, 0, "§a" + name, (press) -> {
								this.ui.displayUnsavedWarning((call) -> {
									CustomizationHelper.editLayout(this.ui.parent.screen, f);
								});
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
							AdvancedButton layoutEntryBtn = new AdvancedButton(0, 0, 0, 0, "§c" + name, (press) -> {
								this.ui.displayUnsavedWarning((call) -> {
									CustomizationHelper.editLayout(this.ui.parent.screen, f);
								});
							});
							layoutEntryBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.buttons.customization.managelayouts.layout.btndesc", Locals.localize("helper.buttons.customization.managelayouts.disabled"), "" + totalactions), "%n%"));
							this.addContent(layoutEntryBtn);
						}
					}
				}
			}

			if (enabled.isEmpty() && disabled.isEmpty()) {
				AdvancedButton emptyBtn = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.empty"), (press) -> {});
				this.addContent(emptyBtn);
			}

			super.openMenuAt(x, y, screenWidth, screenHeight);
		}

	}

	public static class LayoutPropertiesContextMenu extends FMContextMenu {

		private LayoutEditorScreen parent;

		private AdvancedButton renderingOrderBackgroundButton;
		private AdvancedButton renderingOrderForegroundButton;

		private boolean isRightclickOpened;

		public LayoutPropertiesContextMenu(LayoutEditorScreen parent, boolean openedByRightclick) {
			this.parent = parent;
			this.isRightclickOpened = openedByRightclick;
		}

		@Override
		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {

			this.content.clear();

			if (this.parent.isUniversalLayout()) {

				FMContextMenu universalLayoutMenu = new FMContextMenu();
				universalLayoutMenu.setAutoclose(true);
				this.addChild(universalLayoutMenu);

				AdvancedButton universalLayoutButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options"), true, (press) -> {
					universalLayoutMenu.setParentButton((AdvancedButton) press);
					universalLayoutMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
				});
				this.addContent(universalLayoutButton);

				//Add to Blacklist -----------------
				AdvancedButton addBlacklistButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist"), true, (press) -> {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, 240, (call) -> {
						if (call != null) {
							if (!this.parent.universalLayoutBlacklist.contains(call)) {
								this.parent.universalLayoutBlacklist.add(call);
							}
						}
					});
					PopupHandler.displayPopup(p);
				});
				addBlacklistButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist.desc"), "%n%"));
				universalLayoutMenu.addContent(addBlacklistButton);

				//Remove From Blacklist -----------------
				AdvancedButton removeBlacklistButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist"), true, (press) -> {
					ChooseFromStringListScreen s = new ChooseFromStringListScreen(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), this.parent, this.parent.universalLayoutBlacklist, (call) -> {
						if (call != null) {
							FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call2) -> {
								if (call2) {
									this.parent.universalLayoutBlacklist.remove(call);
								}
							}, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist.confirm"), "%n%"));
							PopupHandler.displayPopup(p);
						}
						Minecraft.getInstance().setScreen(this.parent);
					});
					Minecraft.getInstance().setScreen(s);
				});
				universalLayoutMenu.addContent(removeBlacklistButton);

				//Clear Blacklist -----------------
				AdvancedButton clearBlacklistButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist"), true, (press) -> {
					FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
						if (call) {
							this.parent.universalLayoutBlacklist.clear();
						}
					}, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist.confirm"), "%n%"));
					PopupHandler.displayPopup(p);
				});
				universalLayoutMenu.addContent(clearBlacklistButton);

				universalLayoutMenu.addSeparator();

				//Add to Whitelist -----------------
				AdvancedButton addWhitelistButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist"), true, (press) -> {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, 240, (call) -> {
						if (call != null) {
							if (!this.parent.universalLayoutWhitelist.contains(call)) {
								this.parent.universalLayoutWhitelist.add(call);
							}
						}
					});
					PopupHandler.displayPopup(p);
				});
				addWhitelistButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist.desc"), "%n%"));
				universalLayoutMenu.addContent(addWhitelistButton);

				//Remove From Whitelist -----------------
				AdvancedButton removeWhitelistButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist"), true, (press) -> {
					ChooseFromStringListScreen s = new ChooseFromStringListScreen(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), this.parent, this.parent.universalLayoutWhitelist, (call) -> {
						if (call != null) {
							FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call2) -> {
								if (call2) {
									this.parent.universalLayoutWhitelist.remove(call);
								}
							}, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist.confirm"), "%n%"));
							PopupHandler.displayPopup(p);
						}
						Minecraft.getInstance().setScreen(this.parent);
					});
					Minecraft.getInstance().setScreen(s);
				});
				universalLayoutMenu.addContent(removeWhitelistButton);

				//Clear Whitelist -----------------
				AdvancedButton clearWhitelistButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist"), true, (press) -> {
					FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
						if (call) {
							this.parent.universalLayoutWhitelist.clear();
						}
					}, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist.confirm"), "%n%"));
					PopupHandler.displayPopup(p);
				});
				universalLayoutMenu.addContent(clearWhitelistButton);

				this.addSeparator();

			}

			/** SET BACKGROUND **/
			AdvancedButton backgroundOptionsButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground"), true, (press) -> {
				PopupHandler.displayPopup(new BackgroundOptionsPopup(this.parent));
			});
			backgroundOptionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground.btn.desc"), "%n%"));
			this.addContent(backgroundOptionsButton);

			/** RESET BACKGROUND **/
			AdvancedButton resetBackgroundButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.resetbackground"), true, (press) -> {
				if ((this.parent.backgroundTexture != null) || (this.parent.backgroundAnimation != null) || (this.parent.backgroundPanorama != null)) {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				}

				if (this.parent.backgroundAnimation != null) {
					((AdvancedAnimation)this.parent.backgroundAnimation).stopAudio();
				}

				this.parent.backgroundAnimationNames = new ArrayList<String>();
				this.parent.backgroundPanorama = null;
				this.parent.backgroundSlideshow = null;
				this.parent.backgroundAnimation = null;
				this.parent.backgroundTexture = null;
				if (this.parent.customMenuBackground != null) {
					this.parent.customMenuBackground.onResetBackground();
				}
				this.parent.customMenuBackground = null;
				this.parent.customMenuBackgroundInputString = null;
			});
			this.addContent(resetBackgroundButton);

			/** KEEP BACKGROUND ASPECT RATIO **/
			String backgroundAspectLabel = Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.on");
			if (!this.parent.keepBackgroundAspectRatio) {
				backgroundAspectLabel = Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.off");
			}
			AdvancedButton backgroundAspectButton = new AdvancedButton(0, 0, 0, 16, backgroundAspectLabel, true, (press) -> {
				if (this.parent.keepBackgroundAspectRatio) {
					this.parent.keepBackgroundAspectRatio = false;
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.off"));
				} else {
					this.parent.keepBackgroundAspectRatio = true;
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.on"));
				}
			});
			this.addContent(backgroundAspectButton);

			/** SLIDE BACKGROUND IMAGE **/
			String slideBackgroundLabel = Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.on");
			if (!this.parent.panorama) {
				slideBackgroundLabel = Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.off");
			}
			AdvancedButton slideBackgroundButton = new AdvancedButton(0, 0, 0, 16, slideBackgroundLabel, true, (press) -> {
				if (this.parent.panorama) {
					this.parent.panorama = false;
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.off"));
				} else {
					this.parent.panorama = true;
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.on"));
				}
			});
			slideBackgroundButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.btn.desc"), "%n%"));
			this.addContent(slideBackgroundButton);

			/** RESTART ANIMATION ON LOAD **/
			AdvancedButton restartOnLoadButton = new AdvancedButton(0, 0, 0, 16, "", true, (press) -> {
				if (this.parent.restartAnimationBackgroundOnLoad) {
					this.parent.restartAnimationBackgroundOnLoad = false;
				} else {
					this.parent.restartAnimationBackgroundOnLoad = true;
				}
			}) {
				@Override
				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
					if (parent.backgroundAnimation != null) {
						this.active = true;
					} else {
						this.active = false;
					}
					if (parent.restartAnimationBackgroundOnLoad) {
						this.setMessage(Locals.localize("fancymenu.helper.editor.backgrounds.animation.restart_on_load.on"));
					} else {
						this.setMessage(Locals.localize("fancymenu.helper.editor.backgrounds.animation.restart_on_load.off"));
					}
					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
				}
			};
			restartOnLoadButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.backgrounds.animation.restart_on_load.desc"), "%n%"));
			this.addContent(restartOnLoadButton);

			this.addSeparator();

			/** EDIT MENU TITLE **/
			String defaultMenuTitleRaw = "";
			if (this.parent.screen.getTitle() != null) {
				defaultMenuTitleRaw = this.parent.screen.getTitle().getString();
			}
			String defaultMenuTitle = defaultMenuTitleRaw;
			AdvancedButton editMenuTitleButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.edit_menu_title"), true, (press) -> {
				//TODO übernehmenn
				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.edit_menu_title")), this.parent, null, (call) -> {
					if (call != null) {
						if (!call.equals(defaultMenuTitle)) {
							if ((this.parent.customMenuTitle == null) || !this.parent.customMenuTitle.equals(call)) {
								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
							}
							this.parent.customMenuTitle = call;
						} else {
							if (this.parent.customMenuTitle != null) {
								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
							}
							this.parent.customMenuTitle = null;
						}
					}
				});
				s.multilineMode = false;
				if (this.parent.customMenuTitle != null) {
					s.setText(this.parent.customMenuTitle);
				} else {
					s.setText(defaultMenuTitle);
				}
				Minecraft.getInstance().setScreen(s);
				//---------------------
			});
			editMenuTitleButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.edit_menu_title.desc"), "%n%"));
			this.addContent(editMenuTitleButton);

			/** RESET MENU TITLE **/
			AdvancedButton resetMenuTitleButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.edit_menu_title.reset"), true, (press) -> {
				if (this.parent.customMenuTitle != null) {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				}
				this.parent.customMenuTitle = null;
			});
			resetMenuTitleButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.edit_menu_title.reset.desc"), "%n%"));
			this.addContent(resetMenuTitleButton);

			this.addSeparator();

			/** RANDOM MODE **/
			String randomModeString = Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.on");
			if (!this.parent.randomMode) {
				randomModeString = Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.off");
			}
			AdvancedButton randomModeButton = new AdvancedButton(0, 0, 0, 16, randomModeString, true, (press) -> {
				if (this.parent.randomMode) {
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.off"));
					this.parent.randomMode = false;
				} else {
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.on"));
					this.parent.randomMode = true;
				}
			});
			randomModeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.btn.desc"), "%n%"));
			this.addContent(randomModeButton);

			AdvancedButton randomModeGroupButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.setgroup"), true, (press) -> {
				FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.setgroup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
					if (call != null) {
						if (!MathUtils.isInteger(call)) {
							call = "1";
						}
						if (!call.equalsIgnoreCase(this.parent.randomGroup)) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}
						this.parent.randomGroup = call;
					}
				});
				if (this.parent.randomGroup != null) {
					pop.setText(this.parent.randomGroup);
				}
				PopupHandler.displayPopup(pop);
			}) {
				@Override
				public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
					if (parent.randomMode) {
						this.active = true;
					} else {
						this.active = false;
					}
					super.render(matrixStack, mouseX, mouseY, partialTicks);
				}
			};
			randomModeGroupButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.setgroup.btn.desc"), "%n%"));
			this.addContent(randomModeGroupButton);

			String randomModeFirstTimeString = Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.onlyfirsttime.on");
			if (!this.parent.randomOnlyFirstTime) {
				randomModeFirstTimeString = Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.onlyfirsttime.off");
			}
			AdvancedButton randomModeFirstTimeButton = new AdvancedButton(0, 0, 0, 16, randomModeFirstTimeString, true, (press) -> {
				if (this.parent.randomOnlyFirstTime) {
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.onlyfirsttime.off"));
					this.parent.randomOnlyFirstTime = false;
				} else {
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.onlyfirsttime.on"));
					this.parent.randomOnlyFirstTime = true;
				}
			}) {
				@Override
				public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
					if (parent.randomMode) {
						this.active = true;
					} else {
						this.active = false;
					}
					super.render(matrixStack, mouseX, mouseY, partialTicks);
				}
			};
			randomModeFirstTimeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.creator.layoutoptions.randommode.onlyfirsttime.btn.desc"), "%n%"));
			this.addContent(randomModeFirstTimeButton);

			this.addSeparator();

			/** RENDERING ORDER **/
			FMContextMenu renderingOrderMenu = new FMContextMenu();
			renderingOrderMenu.setAutoclose(true);
			this.addChild(renderingOrderMenu);

			this.renderingOrderBackgroundButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.renderorder.background"), true, (press) -> {
				((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.layoutoptions.renderorder.background"));
				this.renderingOrderForegroundButton.setMessage(Locals.localize("helper.creator.layoutoptions.renderorder.foreground"));
				if (!this.parent.renderorder.equals("background")) {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				}

				this.parent.renderorder = "background";
			});
			renderingOrderMenu.addContent(renderingOrderBackgroundButton);

			this.renderingOrderForegroundButton = new AdvancedButton(0, 0, 0, 16, "§a" + Locals.localize("helper.creator.layoutoptions.renderorder.foreground"), true, (press) -> {
				((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.layoutoptions.renderorder.foreground"));
				this.renderingOrderBackgroundButton.setMessage(Locals.localize("helper.creator.layoutoptions.renderorder.background"));
				if (!this.parent.renderorder.equals("foreground")) {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				}

				this.parent.renderorder = "foreground";
			});
			renderingOrderMenu.addContent(renderingOrderForegroundButton);

			if (this.parent.renderorder.equals("background")) {
				renderingOrderForegroundButton.setMessage(Locals.localize("helper.creator.layoutoptions.renderorder.foreground"));
				renderingOrderBackgroundButton.setMessage("§a" + Locals.localize("helper.creator.layoutoptions.renderorder.background"));
			}

			AdvancedButton renderingOrderButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.renderorder"), true, (press) -> {
				renderingOrderMenu.setParentButton((AdvancedButton) press);
				renderingOrderMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(renderingOrderButton);

			/** AUTO-SCALING **/
			String autoScalingLabel = Locals.localize("fancymenu.helper.editor.properties.autoscale.off");
			if ((this.parent.autoScalingWidth != 0) && (this.parent.autoScalingHeight != 0)) {
				autoScalingLabel = Locals.localize("fancymenu.helper.editor.properties.autoscale.on");
			}
			AdvancedButton autoScalingButton = new AdvancedButton(0, 0, 0, 16, autoScalingLabel, true, (press) -> {
				if ((this.parent.autoScalingWidth != 0) && (this.parent.autoScalingHeight != 0)) {
					((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.properties.autoscale.off"));
					this.parent.autoScalingWidth = 0;
					this.parent.autoScalingHeight = 0;
					this.parent.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
				} else {
					PopupHandler.displayPopup(new AutoScalingPopup(this.parent, (call) -> {
						if (call) {
							((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.properties.autoscale.on"));
							this.parent.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
						}
					}));
				}
			}) {
				@Override
				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
					if (parent.scale != 0) {
						this.active = true;
						this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.properties.autoscale.btn.desc"), "%n%"));
					} else {
						this.active = false;
						this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.properties.autoscale.forced_scale_needed"), "%n%"));
					}
					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
				}
			};
			this.addContent(autoScalingButton);

			/** FORCE GUI SCALE **/
			AdvancedButton menuScaleButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.rightclick.scale"), true, (press) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.rightclick.scale"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
					if (call != null) {
						int s = 0;
						if (MathUtils.isInteger(call)) {
							s = Integer.parseInt(call);
						}
						if (s < 0) {
							LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.rightclick.scale.invalid"), "", "", "", "");
						} else {
							if (this.parent.scale != s) {
								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
							}
							this.parent.scale = s;
							this.parent.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
						}
					}
				});
				p.setText("" + this.parent.scale);
				PopupHandler.displayPopup(p);
			});
			menuScaleButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.properties.scale.btn.desc"), "%n%"));
			this.addContent(menuScaleButton);

			/** OPEN/CLOSE SOUND **/
			FMContextMenu openCloseSoundMenu = new FMContextMenu();
			openCloseSoundMenu.setAutoclose(true);
			this.addChild(openCloseSoundMenu);

			AdvancedButton openSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.openaudio"), true, (press) -> {
				ChooseFilePopup p = new ChooseFilePopup((call) -> {
					if (call != null) {
						if (call.length() < 3) {
							if (this.parent.openAudio != null) {
								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
							}
							this.parent.openAudio = null;
						} else {
							File f = new File(call);
							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
								f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
							}
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								if (this.parent.openAudio != call) {
									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
								}
								this.parent.openAudio = call;
							} else {
								LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "");
							}
						}
					} else {
						if (this.parent.openAudio != null) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}
						this.parent.openAudio = null;
					}
				}, "wav");
				if (this.parent.openAudio != null) {
					p.setText(this.parent.openAudio);
				}
				PopupHandler.displayPopup(p);
			});
			openSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.openaudio.desc"), "%n%"));
			openCloseSoundMenu.addContent(openSoundBtn);

			AdvancedButton resetOpenBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.openaudio.reset"), true, (press) -> {
				if (this.parent.openAudio != null) {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				}
				this.parent.openAudio = null;
			});
			resetOpenBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.opencloseaudio.reset.desc"), "%n%"));
			openCloseSoundMenu.addContent(resetOpenBtn);

			AdvancedButton closeSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.closeaudio"), true, (press) -> {
				ChooseFilePopup p = new ChooseFilePopup((call) -> {
					if (call != null) {
						if (call.length() < 3) {
							if (this.parent.closeAudio != null) {
								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
							}
							this.parent.closeAudio = null;
						} else {
							File f = new File(call);
							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
								f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
							}
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								if (this.parent.closeAudio != call) {
									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
								}
								this.parent.closeAudio = call;
							} else {
								LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "");
							}
						}
					} else {
						if (this.parent.closeAudio != null) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}
						this.parent.closeAudio = null;
					}
				}, "wav");
				if (this.parent.closeAudio != null) {
					p.setText(this.parent.closeAudio);
				}
				PopupHandler.displayPopup(p);
			});
			closeSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.closeaudio.desc"), "%n%"));
			openCloseSoundMenu.addContent(closeSoundBtn);

			AdvancedButton resetCloseBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.closeaudio.reset"), true, (press) -> {
				if (this.parent.closeAudio != null) {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				}
				this.parent.closeAudio = null;
			});
			resetCloseBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.opencloseaudio.reset.desc"), "%n%"));
			openCloseSoundMenu.addContent(resetCloseBtn);

			AdvancedButton openCloseSoundButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.opencloseaudio"), true, (press) -> {
				openCloseSoundMenu.setParentButton((AdvancedButton) press);
				openCloseSoundMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			openCloseSoundButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.opencloseaudio.desc"), "%n%"));
			this.addContent(openCloseSoundButton);

			this.addSeparator();

			//TODO übernehmenn
			/** LOADING REQUIREMENTS [LAYOUT-WIDE] **/
			AdvancedButton loadingRequirementsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.loading_requirement.layouts.loading_requirements"), (press) -> {
				ManageRequirementsScreen s = new ManageRequirementsScreen(this.parent, this.parent.layoutWideLoadingRequirementContainer, (call) -> {});
				this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				Minecraft.getInstance().setScreen(s);
			});
			loadingRequirementsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.layouts.loading_requirements.desc"), "%n%"));
			this.addContent(loadingRequirementsButton);
			//----------------------

			/** WINDOW SIZE RESTRICTIONS **/
			FMContextMenu windowSizeMenu = new FMContextMenu();
			windowSizeMenu.setAutoclose(true);
			this.addChild(windowSizeMenu);

			AdvancedButton biggerThanButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.windowsize.biggerthan"), true, (press) -> {
				PopupHandler.displayPopup(new WindowSizePopup(this.parent, ActionType.BIGGERTHAN));
			});
			windowSizeMenu.addContent(biggerThanButton);

			AdvancedButton smallerThanButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.windowsize.smallerthan"), true, (press) -> {
				PopupHandler.displayPopup(new WindowSizePopup(this.parent, ActionType.SMALLERTHAN));
			});
			windowSizeMenu.addContent(smallerThanButton);

			AdvancedButton windowSizeButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.windowsize"), true, (press) -> {
				windowSizeMenu.setParentButton((AdvancedButton) press);
				windowSizeMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			windowSizeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.windowsizerestrictions.btn.desc"), "%n%"));
			this.addContent(windowSizeButton);

			AdvancedButton resetWindowSizeRestrictionsButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.windowsizerestrictions.reset"), true, (press) -> {
				this.parent.biggerThanWidth = 0;
				this.parent.biggerThanHeight = 0;
				this.parent.smallerThanWidth = 0;
				this.parent.smallerThanHeight = 0;
			});
			resetWindowSizeRestrictionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.windowsizerestrictions.reset.btn.desc"), "%n%"));
			this.addContent(resetWindowSizeRestrictionsButton);

			this.addSeparator();

			/** REQUIRED MODS **/
			AdvancedButton requiredModsButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.requiredmods"), true, (press) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.requiredmods.desc"), null, 240, (call) -> {
					if (call != null) {
						if (this.parent.requiredmods != call) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}

						this.parent.requiredmods = call;
					}
				});
				if (this.parent.requiredmods != null) {
					p.setText(this.parent.requiredmods);
				}
				PopupHandler.displayPopup(p);
			});
			this.addContent(requiredModsButton);

			/** MC VERSION **/
			FMContextMenu mcVersionMenu = new FMContextMenu();
			mcVersionMenu.setAutoclose(true);
			this.addChild(mcVersionMenu);

			AdvancedButton minMcVersionButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.minimum"), true, (press) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.minimum.mc"), null, 240, (call) -> {
					if (call != null) {
						if (this.parent.minimumMC != call) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}

						this.parent.minimumMC = call;
					}
				});
				if (this.parent.minimumMC != null) {
					p.setText(this.parent.minimumMC);
				}
				PopupHandler.displayPopup(p);
			});
			mcVersionMenu.addContent(minMcVersionButton);

			AdvancedButton maxMcVersionButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.maximum"), true, (press) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.maximum.mc"), null, 240, (call) -> {
					if (call != null) {
						if (this.parent.maximumMC != call) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}

						this.parent.maximumMC = call;
					}
				});
				if (this.parent.maximumMC != null) {
					p.setText(this.parent.maximumMC);
				}
				PopupHandler.displayPopup(p);
			});
			mcVersionMenu.addContent(maxMcVersionButton);

			AdvancedButton mcVersionButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.mc"), true, (press) -> {
				mcVersionMenu.setParentButton((AdvancedButton) press);
				mcVersionMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(mcVersionButton);

			/** FM VERSION **/
			FMContextMenu fmVersionMenu = new FMContextMenu();
			fmVersionMenu.setAutoclose(true);
			this.addChild(fmVersionMenu);

			AdvancedButton minFmVersionButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.minimum"), true, (press) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.minimum.fm"), null, 240, (call) -> {
					if (call != null) {
						if (this.parent.minimumFM != call) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}

						this.parent.minimumFM = call;
					}
				});
				if (this.parent.minimumFM != null) {
					p.setText(this.parent.minimumFM);
				}
				PopupHandler.displayPopup(p);
			});
			fmVersionMenu.addContent(minFmVersionButton);

			AdvancedButton maxFmVersionButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.maximum"), true, (press) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.maximum.fm"), null, 240, (call) -> {
					if (call != null) {
						if (this.parent.maximumFM != call) {
							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						}

						this.parent.maximumFM = call;
					}
				});
				if (this.parent.maximumFM != null) {
					p.setText(this.parent.maximumFM);
				}
				PopupHandler.displayPopup(p);
			});
			fmVersionMenu.addContent(maxFmVersionButton);

			AdvancedButton fmVersionButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.fm"), true, (press) -> {
				fmVersionMenu.setParentButton((AdvancedButton) press);
				fmVersionMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(fmVersionButton);

			if (this.isRightclickOpened) {
				this.addSeparator();
			}

			/** PASTE **/
			AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.editor.ui.edit.paste"), (press) -> {
				this.parent.pasteElements();
			});
			if (this.isRightclickOpened) {
				this.addContent(pasteButton);
			}

			/** NEW ELEMENT **/
			NewElementContextMenu newElementMenu = new NewElementContextMenu(this.parent);
			newElementMenu.setAutoclose(true);
			this.addChild(newElementMenu);

			AdvancedButton newElementButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.editor.ui.layoutproperties.newelement"), (press) -> {
				newElementMenu.setParentButton((AdvancedButton) press);
				newElementMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			if (this.isRightclickOpened) {
				this.addContent(newElementButton);
			}


			super.openMenuAt(x, y, screenWidth, screenHeight);
		}

	}

	public static class NewElementContextMenu extends FMContextMenu {

		private LayoutEditorScreen parent;

		public NewElementContextMenu(LayoutEditorScreen parent) {
			this.parent = parent;
		}

		@Override
		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {

			this.content.clear();

			/** IMAGE **/
			AdvancedButton imageButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.image"), (press) -> {
				PopupHandler.displayPopup(new ChooseFilePopup(this.parent::addTexture, "jpg", "jpeg", "png", "gif"));
			});
			this.addContent(imageButton);

			/** WEB IMAGE **/
			AdvancedButton webImageButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.webimage"), (press) -> {
				//TODO übernehmenn
				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.editor.elements.web_image.enter_url")), this.parent, null, this.parent::addWebTexture);
				s.multilineMode = false;
				Minecraft.getInstance().setScreen(s);
				//----------------------
			});
			this.addContent(webImageButton);


//			/** TEXT **/
//			AdvancedButton textButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.text"), (press) -> {
//				PopupHandler.displayPopup(new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.text.newtext") + ":", null, 240, this.parent::addText));
//			});
//			this.addContent(textButton);
//
//			/** WEB TEXT **/
//			AdvancedButton webTextButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.webtext"), (press) -> {
//				PopupHandler.displayPopup(new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.web.enterurl"), null, 240, this.parent::addWebText));
//			});
//			this.addContent(webTextButton);

			/** SPLASH TEXT **/
			FMContextMenu splashMenu = new FMContextMenu();
			splashMenu.setAutoclose(true);
			this.addChild(splashMenu);

			AdvancedButton singleSplashButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.add.splash.single"), true, (press) -> {
				//TODO übernehmenn
				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.editor.elements.splash.single.enter_text")), this.parent, null, this.parent::addSingleSplashText);
				s.multilineMode = false;
				Minecraft.getInstance().setScreen(s);
				//--------------------
			});
			singleSplashButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.add.splash.single.desc"), "%n%"));
			splashMenu.addContent(singleSplashButton);

			AdvancedButton multiSplashButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.add.splash.multi"), true, (press) -> {
				PopupHandler.displayPopup(new ChooseFilePopup(this.parent::addMultiSplashText, "txt"));
			});
			multiSplashButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.add.splash.multi.desc"), "%n%"));
			splashMenu.addContent(multiSplashButton);

			AdvancedButton vanillaLikeSplashButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.splash.vanilla_like"), true, (press) -> {
				this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
				PropertiesSection sec = new PropertiesSection("customization");
				sec.addEntry("action", "addsplash");
				sec.addEntry("vanilla-like", "true");
				sec.addEntry("y", "" + (int)(this.parent.ui.bar.getHeight() * UIBase.getUIScale()));
				SplashTextCustomizationItem i = new SplashTextCustomizationItem(sec);
				this.parent.addContent(new LayoutSplashText(i, this.parent));
			});
			vanillaLikeSplashButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.splash.vanilla_like.desc"), "%n%"));
			splashMenu.addContent(vanillaLikeSplashButton);

			AdvancedButton splashButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.splash"), (press) -> {
				splashMenu.setParentButton((AdvancedButton) press);
				splashMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(splashButton);

			/** BUTTON **/
			AdvancedButton buttonButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.button"), (press) -> {
				//TODO übernehmenn
				this.parent.addButton("New Button");
				//------------------
			});
			this.addContent(buttonButton);

			/** ANIMATION **/
			FMContextMenu animationMenu = new FMContextMenu();
			animationMenu.setAutoclose(true);
			this.addChild(animationMenu);

			AdvancedButton inputAnimationButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.animation.entername"), true, (press) -> {
				PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.animation.entername.title") + ":", null, 240, this.parent::addAnimation));
			});
			animationMenu.addContent(inputAnimationButton);

			animationMenu.addSeparator();

			for (String s : AnimationHandler.getCustomAnimationNames()) {
				AdvancedButton aniB = new AdvancedButton(0, 0, 0, 20, s, true, (press) -> {
					this.parent.addAnimation(s);
				});
				animationMenu.addContent(aniB);
			}

			AdvancedButton animationButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.animation"), (press) -> {
				animationMenu.setParentButton((AdvancedButton) press);
				animationMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(animationButton);

			/** SLIDESHOW **/
			FMContextMenu slideshowMenu = new FMContextMenu();
			slideshowMenu.setAutoclose(true);
			this.addChild(slideshowMenu);

			AdvancedButton inputSlideshowButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.slideshow.entername"), true, (press) -> {
				PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.slideshow.entername.title") + ":", null, 240, this.parent::addSlideshow));
			});
			slideshowMenu.addContent(inputSlideshowButton);

			slideshowMenu.addSeparator();

			for (String s : SlideshowHandler.getSlideshowNames()) {
				String name = s;
				if (Minecraft.getInstance().font.width(name) > 90) {
					name = Minecraft.getInstance().font.plainSubstrByWidth(name, 90) + "..";
				}

				AdvancedButton slideshowB = new AdvancedButton(0, 0, 0, 20, name, true, (press) -> {
					if (SlideshowHandler.slideshowExists(s)) {
						this.parent.addSlideshow(s);
					}
				});
				slideshowMenu.addContent(slideshowB);
			}

			AdvancedButton slideshowButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.slideshow"), (press) -> {
				slideshowMenu.setParentButton((AdvancedButton) press);
				slideshowMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(slideshowButton);

			/** SHAPE **/
			FMContextMenu shapesMenu = new FMContextMenu();
			shapesMenu.setAutoclose(true);
			this.addChild(shapesMenu);

			AdvancedButton addRectangleButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.shapes.rectangle"), (press) -> {
				this.parent.addShape(Shape.RECTANGLE);
			});
			shapesMenu.addContent(addRectangleButton);

			AdvancedButton shapesButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.shapes"), (press) -> {
				shapesMenu.setParentButton((AdvancedButton) press);
				shapesMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
			});
			this.addContent(shapesButton);

			/** DUMMY BUTTON: INSTALL AUDIO EXTENSION **/
			AdvancedButton audioButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.audio"), (press) -> {
				ButtonScriptEngine.openWebLink("https://www.curseforge.com/minecraft/mc-mods/audio-extension-for-fancymenu-" + FancyMenu.MOD_LOADER);
			});
			audioButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.extension.dummy.audio.btn.desc"), "%n%"));
			if (!FancyMenu.isAudioExtensionLoaded()) {
				this.addContent(audioButton);
			}

			/** CUSTOM ITEMS (API) **/
			for (CustomizationItemContainer c : CustomizationItemRegistry.getItems()) {

				AdvancedButton cusItemButton = new AdvancedButton(0, 0, 0, 20, c.getDisplayName(), (press) -> {
					this.parent.addContent(c.constructEditorElementInstance(c.constructDefaultItemInstance(), this.parent));
				});
				String[] desc = c.getDescription();
				if ((desc != null) && (desc.length > 0)) {
					cusItemButton.setDescription(desc);
				}
				this.addContent(cusItemButton);

			}

			super.openMenuAt(x, y, screenWidth, screenHeight);
		}

	}

	public static class ManageAudioContextMenu extends FMContextMenu {

		private LayoutEditorScreen parent;

		public ManageAudioContextMenu(LayoutEditorScreen parent) {
			this.parent = parent;
		}

		@Override
		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {

			this.content.clear();

			if (this.parent.audio.isEmpty()) {

				AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.empty"), true, (press) -> {});
				this.addContent(bt);

			} else {

				for (Map.Entry<String, Boolean> m : this.parent.audio.entrySet()) {

					String label = new File(m.getKey()).getName();
					if (Minecraft.getInstance().font.width(label) > 200) {
						label = Minecraft.getInstance().font.plainSubstrByWidth(label, 200) + "..";
					}

					FMContextMenu actionsMenu = new FMContextMenu();
					actionsMenu.setAutoclose(true);
					this.addChild(actionsMenu);

					AdvancedButton deleteButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.audio.delete"), true, (press2) -> {
						this.closeMenu();
						PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
							if (call) {
								this.parent.audio.remove(m.getKey());
								SoundHandler.stopSound(m.getKey());
								MenuCustomization.unregisterSound(m.getKey());
							}
						}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", "", Locals.localize("helper.creator.audio.delete.msg"), "", ""));
					});
					actionsMenu.addContent(deleteButton);

					String lab = Locals.localize("helper.editor.ui.element.manageaudio.loop.off");
					if (m.getValue()) {
						lab = Locals.localize("helper.editor.ui.element.manageaudio.loop.on");
					}
					AdvancedButton toggleLoopButton = new AdvancedButton(0, 0, 0, 16, lab, true, (press2) -> {
						if (((AdvancedButton)press2).getMessage().getString().equals(Locals.localize("helper.editor.ui.element.manageaudio.loop.off"))) {
							SoundHandler.setLooped(m.getKey(), true);
							this.parent.audio.put(m.getKey(), true);
							((AdvancedButton)press2).setMessage(Locals.localize("helper.editor.ui.element.manageaudio.loop.on"));;
						} else {
							SoundHandler.setLooped(m.getKey(), false);
							this.parent.audio.put(m.getKey(), false);
							((AdvancedButton)press2).setMessage(Locals.localize("helper.editor.ui.element.manageaudio.loop.off"));;
						}
					});
					actionsMenu.addContent(toggleLoopButton);

					AdvancedButton actionsButton = new AdvancedButton(0, 0, 0, 16, label, true, (press) -> {
						actionsMenu.setParentButton((AdvancedButton) press);
						actionsMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
					});
					this.addContent(actionsButton);

				}
			}


			super.openMenuAt(x, y, screenWidth, screenHeight);
		}

	}

	public static class MultiselectContextMenu extends FMContextMenu {

		private LayoutEditorScreen parent;

		public MultiselectContextMenu(LayoutEditorScreen parent) {
			this.parent = parent;
		}

		@Override
		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {

			this.content.clear();

			if (this.parent.isObjectFocused()) {

				this.parent.focusedObjectsCache = this.parent.getFocusedObjects();

				this.parent.multiselectStretchedX = false;
				this.parent.multiselectStretchedY = false;

				/** DELETE ALL **/
				AdvancedButton deleteBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.object.deleteall"), true, (press) -> {
					this.parent.deleteFocusedObjects();
				});
				deleteBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.object.deleteall.btndesc"), "%n%"));
				this.addContent(deleteBtn);

				/** STRETCH ALL **/
				FMContextMenu stretchMenu = new FMContextMenu();
				stretchMenu.setAutoclose(true);
				this.addChild(stretchMenu);

				AdvancedButton stretchXBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.object.stretch.x"), true, (press) -> {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());

					for (LayoutElement o : this.parent.focusedObjectsCache) {
						if (o.isStretchable()) {
							o.setStretchedX(!this.parent.multiselectStretchedX, false);
						}
					}

					this.parent.multiselectStretchedX = !this.parent.multiselectStretchedX;

					if (!this.parent.multiselectStretchedX) {
						press.setMessage(Component.literal(Locals.localize("helper.creator.object.stretch.x")));
					} else {
						press.setMessage(Component.literal("§a" + Locals.localize("helper.creator.object.stretch.x")));
					}

				});
				stretchMenu.addContent(stretchXBtn);

				AdvancedButton stretchYBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.object.stretch.y"), true, (press) -> {
					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());

					for (LayoutElement o : this.parent.focusedObjectsCache) {
						if (o.isStretchable()) {
							o.setStretchedY(!this.parent.multiselectStretchedY, false);
						}
					}

					this.parent.multiselectStretchedY = !this.parent.multiselectStretchedY;

					if (!this.parent.multiselectStretchedY) {
						press.setMessage(Component.literal(Locals.localize("helper.creator.object.stretch.y")));
					} else {
						press.setMessage(Component.literal("§a" + Locals.localize("helper.creator.object.stretch.y")));
					}

				});
				stretchMenu.addContent(stretchYBtn);

				AdvancedButton stretchBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.object.stretchall"), true, (press) -> {
					stretchMenu.setParentButton((AdvancedButton) press);
					stretchMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
				});
				stretchBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.object.stretchall.btndesc"), "%n%"));
				this.addContent(stretchBtn);

				/** COPY **/
				AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.editor.ui.edit.copy"), (press) -> {
					this.parent.copySelectedElements();
				});
				this.addContent(copyButton);

				/** PASTE **/
				AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.editor.ui.edit.paste"), (press) -> {
					this.parent.pasteElements();
				});
				this.addContent(pasteButton);

				boolean allVanillaBtns = true;
				boolean allBtns = true;
				for (LayoutElement o : this.parent.focusedObjectsCache) {
					if (!(o instanceof LayoutVanillaButton)) {
						allVanillaBtns = false;
					}
					if (!(o instanceof LayoutVanillaButton) && !(o instanceof LayoutButton)) {
						allBtns = false;
					}
				}
				if (this.parent.focusedObjectsCache.isEmpty()) {
					allVanillaBtns = false;
					allBtns = false;
				}

				if (allVanillaBtns) {

					/** VANILLA: RESET ORIENTATION **/
					AdvancedButton resetOriBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.vanillabutton.resetorientation"), true, (press) -> {
						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());

						for (LayoutElement o : this.parent.focusedObjectsCache) {
							if (o instanceof LayoutVanillaButton) {
								LayoutVanillaButton vb = (LayoutVanillaButton) o;
								vb.object.orientation = "original";
								vb.object.posX = vb.button.x;
								vb.object.posY = vb.button.y;
								vb.object.setWidth(vb.button.width);
								vb.object.setHeight(vb.button.height);
							}
						}
						this.closeMenu();
						Minecraft.getInstance().setScreen(this.parent);
					});
					resetOriBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.vanillabutton.resetorientation.btndesc"), "%n%"));
					this.addContent(resetOriBtn);

					/** VANILLA: DELETE **/
					AdvancedButton hideAllBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.vanillabutton.hideall"), true, (press) -> {
						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						this.parent.history.setPreventSnapshotSaving(true);

						for (LayoutElement o : this.parent.focusedObjectsCache) {
							if (o instanceof LayoutVanillaButton) {
								LayoutVanillaButton vb = (LayoutVanillaButton) o;
								this.parent.hideVanillaButton(vb);
							}
						}

						this.parent.focusedObjects.clear();
						this.parent.focusedObjectsCache.clear();
						this.parent.multiselectRightclickMenu.closeMenu();

						this.parent.history.setPreventSnapshotSaving(false);
					});
					hideAllBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.vanillabutton.hideall.btndesc"), "%n%"));
					this.addContent(hideAllBtn);

				}

				if (allBtns) {

					/** BUTTONS: BACKGROUND **/
					AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground"), true, (press) -> {
						this.parent.setButtonTexturesForFocusedObjects();
					});
					buttonBackgroundButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.buttontexture.btndesc"), "%n%"));
					this.addContent(buttonBackgroundButton);

					/** BUTTONS: CLICK SOUND **/
					AdvancedButton clickSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), true, (press) -> {
						ChooseFilePopup cf = new ChooseFilePopup((call) -> {
							if (call != null) {
								File f = new File(call);
								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
								}
								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
									this.parent.history.setPreventSnapshotSaving(true);

									for (LayoutElement o : this.parent.focusedObjectsCache) {
										if (o instanceof LayoutVanillaButton) {
											LayoutVanillaButton vb = (LayoutVanillaButton) o;
											vb.customizationContainer.clickSound = call;
										} else if (o instanceof LayoutButton) {
											LayoutButton lb = (LayoutButton) o;
											lb.customizationContainer.clickSound = call;
										}
									}

									this.parent.history.setPreventSnapshotSaving(false);
								} else {
									LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
								}
							}
						}, "wav");

						PopupHandler.displayPopup(cf);
					});
					clickSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.clicksound.btndesc"), "%n%"));
					this.addContent(clickSoundBtn);

					/** BUTTONS: RESET CLICK SOUND **/
					AdvancedButton resetClickSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound.reset"), true, (press) -> {

						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						this.parent.history.setPreventSnapshotSaving(true);

						for (LayoutElement o : this.parent.focusedObjectsCache) {
							if (o instanceof LayoutVanillaButton) {
								LayoutVanillaButton vb = (LayoutVanillaButton) o;
								vb.customizationContainer.clickSound = null;
							} else if (o instanceof LayoutButton) {
								LayoutButton lb = (LayoutButton) o;
								lb.customizationContainer.clickSound = null;
							}
						}

						this.parent.history.setPreventSnapshotSaving(false);

					});
					resetClickSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.clicksound.reset.btndesc"), "%n%"));
					this.addContent(resetClickSoundBtn);

					/** BUTTONS: HOVER SOUND **/
					AdvancedButton hoverSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), true, (press) -> {
						ChooseFilePopup cf = new ChooseFilePopup((call) -> {
							if (call != null) {
								File f = new File(call);
								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
								}
								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
									this.parent.history.setPreventSnapshotSaving(true);

									for (LayoutElement o : this.parent.focusedObjectsCache) {
										if (o instanceof LayoutVanillaButton) {
											LayoutVanillaButton vb = (LayoutVanillaButton) o;
											vb.customizationContainer.hoverSound = call;
										} else if (o instanceof LayoutButton) {
											LayoutButton lb = (LayoutButton) o;
											lb.customizationContainer.hoverSound = call;
										}
									}

									this.parent.history.setPreventSnapshotSaving(false);
								} else {
									LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
								}
							}
						}, "wav");

						PopupHandler.displayPopup(cf);
					});
					hoverSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.hoversound.btndesc"), "%n%"));
					this.addContent(hoverSoundBtn);

					/** BUTTONS: RESET HOVERSOUND **/
					AdvancedButton resetHoverSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound.reset"), true, (press) -> {

						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
						this.parent.history.setPreventSnapshotSaving(true);

						for (LayoutElement o : this.parent.focusedObjectsCache) {
							if (o instanceof LayoutVanillaButton) {
								LayoutVanillaButton vb = (LayoutVanillaButton) o;
								vb.customizationContainer.hoverSound = null;
							} else if (o instanceof LayoutButton) {
								LayoutButton lb = (LayoutButton) o;
								lb.customizationContainer.hoverSound = null;
							}
						}

						this.parent.history.setPreventSnapshotSaving(false);

					});
					resetHoverSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.hoversound.reset.btndesc"), "%n%"));
					this.addContent(resetHoverSoundBtn);
				}

			}


			super.openMenuAt(x, y, screenWidth, screenHeight);
		}

	}

	public static class HiddenVanillaButtonContextMenu extends FMContextMenu {

		private LayoutEditorScreen parent;

		public HiddenVanillaButtonContextMenu(LayoutEditorScreen parent) {
			this.parent = parent;
		}

		@Override
		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {

			this.content.clear();
			this.separators.clear();

			boolean containsHiddenDeeps = false;
			for (LayoutElement e : this.parent.content) {
				if (e instanceof DeepCustomizationLayoutEditorElement) {
					if (((DeepCustomizationLayoutEditorElement)e).getDeepCustomizationItem().hidden) {
						String name = ((DeepCustomizationLayoutEditorElement) e).parentDeepCustomizationElement.getDisplayName();
						AdvancedButton hiddenButton = new AdvancedButton(0, 0, 0, 0, name, true, (press) -> {
							((DeepCustomizationLayoutEditorElement) e).getDeepCustomizationItem().hidden = false;
							this.parent.updateContent();
							this.closeMenu();
						});
						hiddenButton.setDescription(StringUtils.splitLines(Locals.localize("helper.editor.ui.element.deletedvanillabuttons.entry.desc"), "%n%"));
						this.addContent(hiddenButton);
						containsHiddenDeeps = true;
					}
				}
			}

			if (!this.parent.getHiddenButtons().isEmpty()) {
				for (LayoutVanillaButton b : this.parent.getHiddenButtons()) {

					String name = b.button.getButton().getMessage().getString();
					AdvancedButton hiddenButton = new AdvancedButton(0, 0, 0, 0, name, true, (press) -> {
						this.parent.showVanillaButton(b);
						this.closeMenu();
					});
					hiddenButton.setDescription(StringUtils.splitLines(Locals.localize("helper.editor.ui.element.deletedvanillabuttons.entry.desc"), "%n%"));
					this.addContent(hiddenButton);

				}
			} else if (!containsHiddenDeeps) {
				AdvancedButton emptyButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.empty"), true, (press) -> {});
				this.addContent(emptyButton);
			}

			super.openMenuAt(x, y, screenWidth, screenHeight);
		}

	}

}
