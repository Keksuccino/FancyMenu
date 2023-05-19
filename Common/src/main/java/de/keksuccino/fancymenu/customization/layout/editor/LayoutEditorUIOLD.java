//package de.keksuccino.fancymenu.customization.layout.editor;
//
//import com.google.common.io.Files;
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.FancyMenu;
//import de.keksuccino.fancymenu.customization.ScreenCustomization;
//import de.keksuccino.fancymenu.customization.action.ActionExecutor;
//import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
//import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
//import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
//import de.keksuccino.fancymenu.customization.element.ElementBuilder;
//import de.keksuccino.fancymenu.customization.element.ElementRegistry;
//import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
//import de.keksuccino.fancymenu.customization.element.v1.ShapeCustomizationItem.Shape;
//import de.keksuccino.fancymenu.customization.element.v1.SplashTextCustomizationItem;
//import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
//import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.BackgroundOptionsPopup;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.ChooseFilePopup;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.LayoutSplashText;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutButton;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutVanillaButton;
//import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
//import de.keksuccino.fancymenu.customization.overlay.OverlayButton;
//import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
//import de.keksuccino.fancymenu.properties.PropertyContainer;
//import de.keksuccino.fancymenu.properties.PropertyContainerSet;
//import de.keksuccino.fancymenu.rendering.ui.MenuBar;
//import de.keksuccino.fancymenu.rendering.ui.MenuBar.ElementAlignment;
//import de.keksuccino.fancymenu.rendering.ui.UIBase;
//import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMYesNoPopup;
//import de.keksuccino.fancymenu.rendering.ui.screen.ChooseFromStringListScreen;
//import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.CharacterFilter;
//import de.keksuccino.fancymenu.utils.LocalizationUtils;
//import net.minecraft.client.resources.language.I18n;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
//import de.keksuccino.konkrete.sound.SoundHandler;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//
//import java.awt.*;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Consumer;
//
//public class LayoutEditorUIOLD extends UIBase {
//
//	public MenuBar bar;
//	public LayoutEditorScreen parent;
//
//	protected int tick = 0;
//
//	protected static final ResourceLocation CLOSE_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "close_btn.png");
//
//	public LayoutEditorUIOLD(LayoutEditorScreen parent) {
//		this.parent = parent;
//		this.updateUI();
//	}
//
//	public void updateUI() {
//		try {
//
//			boolean extended = true;
//			if (bar != null) {
//				extended = bar.isExtended();
//			}
//
//			bar = new MenuBar();
//			bar.setExtended(extended);
//
//			/** LAYOUT TAB **/
//			ContextMenu layoutMenu = new ContextMenu();
//			layoutMenu.setAutoclose(true);
//			bar.addChild(layoutMenu, "fm.editor.ui.tab.layout", ElementAlignment.LEFT);
//
//			AdvancedButton newLayoutButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.layout.new"), true, (press) -> {
//				this.displayUnsavedWarning((call) -> {
//					if (call) {
//						ScreenCustomization.stopSounds();
//						ScreenCustomization.resetSounds();
//						Minecraft.getInstance().setScreen(new LayoutEditorScreen(this.parent.layoutTargetScreen));
//					}
//				});
//			});
//			layoutMenu.addContent(newLayoutButton);
//
//			OpenLayoutContextMenu openLayoutMenu = new OpenLayoutContextMenu(this);
//			openLayoutMenu.setAutoclose(true);
//			layoutMenu.addChild(openLayoutMenu);
//
//			AdvancedButton openLayoutButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.layout.open"), true, (press) -> {
//				openLayoutMenu.setParentButton((AdvancedButton) press);
//				openLayoutMenu.openMenuAt(0, press.y);
//			});
//			layoutMenu.addContent(openLayoutButton);
//
//			AdvancedButton layoutSaveButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.layout.save"), true, (press) -> {
//				this.parent.saveLayout();
//			});
//			layoutMenu.addContent(layoutSaveButton);
//
//			AdvancedButton layoutSaveAsButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.layout.saveas"), true, (press) -> {
//				this.parent.saveLayoutAs();
//			});
//			layoutMenu.addContent(layoutSaveAsButton);
//
//			LayoutPropertiesContextMenu layoutPropertiesMenu = new LayoutPropertiesContextMenu(this.parent, false);
//			layoutPropertiesMenu.setAutoclose(true);
//			layoutMenu.addChild(layoutPropertiesMenu);
//
//			AdvancedButton layoutPropertiesButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.layout.properties"), true, (press) -> {
//				layoutPropertiesMenu.setParentButton((AdvancedButton) press);
//				layoutPropertiesMenu.openMenuAt(0, press.y);
//			});
//			layoutMenu.addContent(layoutPropertiesButton);
//
//			AdvancedButton exitButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.exit"), true, (press) -> {
//				this.closeEditor();
//			});
//			layoutMenu.addContent(exitButton);
//
//			OverlayButton layoutTab = new OverlayButton(0, 0, 0, 0, I18n.get("fancymenu.editor.layout"), true, (press) -> {
//				layoutMenu.setParentButton((AdvancedButton) press);
//				layoutMenu.openMenuAt(press.x, press.y + press.getHeight());
//			});
//			bar.addElement(layoutTab, "fm.editor.ui.tab.layout", ElementAlignment.LEFT, false);
//
//			/** EDIT TAB **/
//			ContextMenu editMenu = new ContextMenu();
//			editMenu.setAutoclose(true);
//			bar.addChild(editMenu, "fm.editor.ui.tab.edit", ElementAlignment.LEFT);
//
//			AdvancedButton undoButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.edit.undo"), true, (press) -> {
//				this.parent.history.stepBack();
//				try {
//					((LayoutEditorScreen)Minecraft.getInstance().screen).ui.topMenuBar.getChild("fm.editor.ui.tab.edit").openMenuAt(editMenu.getX(), editMenu.getY());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			});
//			editMenu.addContent(undoButton);
//
//			AdvancedButton redoButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.edit.redo"), true, (press) -> {
//				this.parent.history.stepForward();
//				try {
//					((LayoutEditorScreen)Minecraft.getInstance().screen).ui.topMenuBar.getChild("fm.editor.ui.tab.edit").openMenuAt(editMenu.getX(), editMenu.getY());
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			});
//			editMenu.addContent(redoButton);
//
//			editMenu.addSeparator();
//
//			AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.edit.copy"), true, (press) -> {
//				this.parent.copySelectedElements();
//			});
//			editMenu.addContent(copyButton);
//
//			AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.edit.paste"), true, (press) -> {
//				this.parent.pasteElements();
//			});
//			editMenu.addContent(pasteButton);
//
//			OverlayButton editTab = new OverlayButton(0, 0, 0, 0, I18n.get("fancymenu.editor.edit"), true, (press) -> {
//				editMenu.setParentButton((AdvancedButton) press);
//				editMenu.openMenuAt(press.x, press.y + press.getHeight());
//			});
//			bar.addElement(editTab, "fm.editor.ui.tab.edit", ElementAlignment.LEFT, false);
//
//			/** ELEMENT TAB **/
//			ContextMenu elementMenu = new ContextMenu();
//			elementMenu.setAutoclose(true);
//			bar.addChild(elementMenu, "fm.editor.ui.tab.element", ElementAlignment.LEFT);
//
//			NewElementContextMenu newElementMenu = new NewElementContextMenu(this.parent);
//			newElementMenu.setAutoclose(true);
//			elementMenu.addChild(newElementMenu);
//
//			AdvancedButton newElementButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.element.new"), true, (press) -> {
//				newElementMenu.setParentButton((AdvancedButton) press);
//				newElementMenu.openMenuAt(0, press.y);
//			});
//			elementMenu.addContent(newElementButton);
//
//			ManageAudioContextMenu manageAudioMenu = new ManageAudioContextMenu(this.parent);
//			manageAudioMenu.setAutoclose(true);
//			elementMenu.addChild(manageAudioMenu);
//
//			AdvancedButton manageAudioButton = new AdvancedButton(0, 0, 0, 0, "§m" + I18n.get("fancymenu.editor.element.manageaudio"), true, (press) -> {
//				manageAudioMenu.setParentButton((AdvancedButton) press);
//				manageAudioMenu.openMenuAt(0, press.y);
//			});
//			manageAudioButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.extension.dummy.audio.manageaudio.btn.desc")));
//			elementMenu.addContent(manageAudioButton);
//
//			HiddenVanillaButtonContextMenu hiddenVanillaMenu = new HiddenVanillaButtonContextMenu(this.parent);
//			hiddenVanillaMenu.setAutoclose(true);
//			elementMenu.addChild(hiddenVanillaMenu);
//
//			AdvancedButton hiddenVanillaButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.fancymenu.editor.element.deleted_vanilla_elements"), true, (press) -> {
//				hiddenVanillaMenu.setParentButton((AdvancedButton) press);
//				hiddenVanillaMenu.openMenuAt(0, press.y);
//			});
//			hiddenVanillaButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.fancymenu.editor.element.deleted_vanilla_elements.desc")));
//			elementMenu.addContent(hiddenVanillaButton);
//
//			OverlayButton elementTab = new OverlayButton(0, 0, 0, 0, I18n.get("fancymenu.editor.element"), true, (press) -> {
//				elementMenu.setParentButton((AdvancedButton) press);
//				elementMenu.openMenuAt(press.x, press.y + press.getHeight());
//			});
//			bar.addElement(elementTab, "fm.editor.ui.tab.element", ElementAlignment.LEFT, false);
//
//			/** CLOSE GUI BUTTON TAB **/
//			AdvancedImageButton exitEditorButtonTab = new AdvancedImageButton(20, 20, 0, 0, CLOSE_BUTTON_TEXTURE, true, (press) -> {
//				this.closeEditor();
//			}) {
//				@Override
//				public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
//					this.width = this.height;
//					super.render(matrix, mouseX, mouseY, partialTicks);
//				}
//			};
//			exitEditorButtonTab.ignoreLeftMouseDownClickBlock = true;
//			exitEditorButtonTab.ignoreBlockedInput = true;
//			exitEditorButtonTab.enableRightclick = true;
//			exitEditorButtonTab.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.exit.desc")));
//			bar.addElement(exitEditorButtonTab, "fm.editor.ui.tab.exit", ElementAlignment.RIGHT, false);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void render(PoseStack matrix, Screen screen) {
//		try {
//
//			if (bar != null) {
//				if (!PopupHandler.isPopupActive()) {
//					if (screen instanceof LayoutEditorScreen) {
//
//						bar.render(matrix, screen);
//
//					}
//				}
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	protected void displayUnsavedWarning(Consumer<Boolean> callback) {
//		PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, callback, I18n.get("fancymenu.editor.warning.unsaved")));
//	}
//
//	public void closeEditor() {
//		this.displayUnsavedWarning((call) -> {
//			if (call) {
//				LayoutEditorScreen.isActive = false;
//				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
//					if (r instanceof AdvancedAnimation) {
//						((AdvancedAnimation)r).stopAudio();
//						if (((AdvancedAnimation)r).replayIntro()) {
//							((AdvancedAnimation)r).resetAnimation();
//						}
//					}
//				}
//				ScreenCustomization.stopSounds();
//				ScreenCustomization.resetSounds();
//				LayoutHandler.reloadLayouts();
//
//				Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
//				this.parent.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
//				this.parent.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
//
//				Screen s = this.parent.layoutTargetScreen;
//				if ((s instanceof CustomGuiBase) && ((CustomGuiBase)s).getIdentifier().equals("%fancymenu:universal_layout%")) {
//					s = ((CustomGuiBase)s).parent;
//				}
//				Minecraft.getInstance().setScreen(s);
//			}
//		});
//	}
//
//	private static class OpenLayoutContextMenu extends ContextMenu {
//
//		private LayoutEditorUIOLD ui;
//
//		public OpenLayoutContextMenu(LayoutEditorUIOLD ui) {
//			this.ui = ui;
//		}
//
//		@Override
//		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
//
//			this.content.clear();
//
//			String identifier = this.ui.parent.layoutTargetScreen.getClass().getName();
//			if (this.ui.parent.layoutTargetScreen instanceof CustomGuiBase) {
//				identifier = ((CustomGuiBase) this.ui.parent.layoutTargetScreen).getIdentifier();
//			}
//
//			List<PropertyContainerSet> enabled = LayoutHandler.getEnabledLayoutsForMenuIdentifier(identifier);
//			if (!enabled.isEmpty()) {
//				for (PropertyContainerSet s : enabled) {
//					List<PropertyContainer> secs = s.getSectionsOfType("customization-meta");
//					if (secs.isEmpty()) {
//						secs = s.getSectionsOfType("type-meta");
//					}
//					if (!secs.isEmpty()) {
//						String name = "<missing name>";
//						PropertyContainer meta = secs.get(0);
//						File f = new File(meta.getValue("path"));
//						if (f.isFile()) {
//							name = Files.getNameWithoutExtension(f.getName());
//
//							int totalactions = s.getContainers().size() - 1;
//							AdvancedButton layoutEntryBtn = new AdvancedButton(0, 0, 0, 0, "§a" + name, (press) -> {
//								this.ui.displayUnsavedWarning((call) -> {
//									LayoutHandler.openLayoutEditor(this.ui.parent.layoutTargetScreen, f);
//								});
//							});
//							layoutEntryBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.overlay.ui.customization.managelayouts.layout.btndesc", I18n.get("fancymenu.overlay.ui.customization.managelayouts.enabled"), "" + totalactions)));
//							this.addContent(layoutEntryBtn);
//						}
//					}
//				}
//			}
//
//			List<PropertyContainerSet> disabled = LayoutHandler.getDisabledLayoutsForMenuIdentifier(identifier);
//			if (!disabled.isEmpty()) {
//				for (PropertyContainerSet s : disabled) {
//					List<PropertyContainer> secs = s.getSectionsOfType("customization-meta");
//					if (secs.isEmpty()) {
//						secs = s.getSectionsOfType("type-meta");
//					}
//					if (!secs.isEmpty()) {
//						String name = "<missing name>";
//						PropertyContainer meta = secs.get(0);
//						File f = new File(meta.getValue("path"));
//						if (f.isFile()) {
//							name = Files.getNameWithoutExtension(f.getName());
//
//							int totalactions = s.getContainers().size() - 1;
//							AdvancedButton layoutEntryBtn = new AdvancedButton(0, 0, 0, 0, "§c" + name, (press) -> {
//								this.ui.displayUnsavedWarning((call) -> {
//									LayoutHandler.openLayoutEditor(this.ui.parent.layoutTargetScreen, f);
//								});
//							});
//							layoutEntryBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.overlay.ui.customization.managelayouts.layout.btndesc", I18n.get("fancymenu.overlay.ui.customization.managelayouts.disabled"), "" + totalactions)));
//							this.addContent(layoutEntryBtn);
//						}
//					}
//				}
//			}
//
//			if (enabled.isEmpty() && disabled.isEmpty()) {
//				AdvancedButton emptyBtn = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.empty"), (press) -> {});
//				this.addContent(emptyBtn);
//			}
//
//			super.openMenuAt(x, y, screenWidth, screenHeight);
//		}
//
//	}
//
//	public static class LayoutPropertiesContextMenu extends ContextMenu {
//
//		private LayoutEditorScreen parent;
//
//		private AdvancedButton renderingOrderBackgroundButton;
//		private AdvancedButton renderingOrderForegroundButton;
//
//		private boolean isRightclickOpened;
//
//		public LayoutPropertiesContextMenu(LayoutEditorScreen parent, boolean openedByRightclick) {
//			this.parent = parent;
//			this.isRightclickOpened = openedByRightclick;
//		}
//
//		@Override
//		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
//
//			this.content.clear();
//
//			if (this.parent.isUniversalLayout()) {
//
//				ContextMenu universalLayoutMenu = new ContextMenu();
//				universalLayoutMenu.setAutoclose(true);
//				this.addChild(universalLayoutMenu);
//
//				AdvancedButton universalLayoutButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options"), true, (press) -> {
//					universalLayoutMenu.setParentButton((AdvancedButton) press);
//					universalLayoutMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//				});
//				this.addContent(universalLayoutButton);
//
//				//Add to Blacklist -----------------
//				AdvancedButton addBlacklistButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist"), true, (press) -> {
//					FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, 240, (call) -> {
//						if (call != null) {
//							if (!this.parent.universalLayoutBlacklist.contains(call)) {
//								this.parent.universalLayoutBlacklist.add(call);
//							}
//						}
//					});
//					PopupHandler.displayPopup(p);
//				});
//				addBlacklistButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist.desc")));
//				universalLayoutMenu.addContent(addBlacklistButton);
//
//				//Remove From Blacklist -----------------
//				AdvancedButton removeBlacklistButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist"), true, (press) -> {
//					ChooseFromStringListScreen s = new ChooseFromStringListScreen(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), this.parent, this.parent.universalLayoutBlacklist, (call) -> {
//						if (call != null) {
//							FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call2) -> {
//								if (call2) {
//									this.parent.universalLayoutBlacklist.remove(call);
//								}
//							}, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist.confirm")));
//							PopupHandler.displayPopup(p);
//						}
//						Minecraft.getInstance().setScreen(this.parent);
//					});
//					Minecraft.getInstance().setScreen(s);
//				});
//				universalLayoutMenu.addContent(removeBlacklistButton);
//
//				//Clear Blacklist -----------------
//				AdvancedButton clearBlacklistButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist"), true, (press) -> {
//					FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
//						if (call) {
//							this.parent.universalLayoutBlacklist.clear();
//						}
//					}, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist.confirm")));
//					PopupHandler.displayPopup(p);
//				});
//				universalLayoutMenu.addContent(clearBlacklistButton);
//
//				universalLayoutMenu.addSeparator();
//
//				//Add to Whitelist -----------------
//				AdvancedButton addWhitelistButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist"), true, (press) -> {
//					FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, 240, (call) -> {
//						if (call != null) {
//							if (!this.parent.universalLayoutWhitelist.contains(call)) {
//								this.parent.universalLayoutWhitelist.add(call);
//							}
//						}
//					});
//					PopupHandler.displayPopup(p);
//				});
//				addWhitelistButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist.desc")));
//				universalLayoutMenu.addContent(addWhitelistButton);
//
//				//Remove From Whitelist -----------------
//				AdvancedButton removeWhitelistButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist"), true, (press) -> {
//					ChooseFromStringListScreen s = new ChooseFromStringListScreen(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), this.parent, this.parent.universalLayoutWhitelist, (call) -> {
//						if (call != null) {
//							FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call2) -> {
//								if (call2) {
//									this.parent.universalLayoutWhitelist.remove(call);
//								}
//							}, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist.confirm")));
//							PopupHandler.displayPopup(p);
//						}
//						Minecraft.getInstance().setScreen(this.parent);
//					});
//					Minecraft.getInstance().setScreen(s);
//				});
//				universalLayoutMenu.addContent(removeWhitelistButton);
//
//				//Clear Whitelist -----------------
//				AdvancedButton clearWhitelistButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist"), true, (press) -> {
//					FMYesNoPopup p = new FMYesNoPopup(300, new Color(0,0,0,0), 240, (call) -> {
//						if (call) {
//							this.parent.universalLayoutWhitelist.clear();
//						}
//					}, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist.confirm")));
//					PopupHandler.displayPopup(p);
//				});
//				universalLayoutMenu.addContent(clearWhitelistButton);
//
//				this.addSeparator();
//
//			}
//
//			/** SET BACKGROUND **/
//			AdvancedButton backgroundOptionsButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground"), true, (press) -> {
//				PopupHandler.displayPopup(new BackgroundOptionsPopup(this.parent));
//			});
//			backgroundOptionsButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground.btn.desc")));
//			this.addContent(backgroundOptionsButton);
//
//			/** RESET BACKGROUND **/
//			AdvancedButton resetBackgroundButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.resetbackground"), true, (press) -> {
//				if ((this.parent.backgroundTexture != null) || (this.parent.backgroundAnimation != null) || (this.parent.backgroundPanorama != null)) {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				}
//
//				if (this.parent.backgroundAnimation != null) {
//					((AdvancedAnimation)this.parent.backgroundAnimation).stopAudio();
//				}
//
//				this.parent.backgroundAnimationNames = new ArrayList<String>();
//				this.parent.backgroundPanorama = null;
//				this.parent.backgroundSlideshow = null;
//				this.parent.backgroundAnimation = null;
//				this.parent.backgroundTexture = null;
//				if (this.parent.customMenuBackground != null) {
//					this.parent.customMenuBackground.onResetBackground();
//				}
//				this.parent.customMenuBackground = null;
//				this.parent.customMenuBackgroundInputString = null;
//			});
//			this.addContent(resetBackgroundButton);
//
//			/** KEEP BACKGROUND ASPECT RATIO **/
//			String backgroundAspectLabel = I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.on");
//			if (!this.parent.keepBackgroundAspectRatio) {
//				backgroundAspectLabel = I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.off");
//			}
//			AdvancedButton backgroundAspectButton = new AdvancedButton(0, 0, 0, 16, backgroundAspectLabel, true, (press) -> {
//				if (this.parent.keepBackgroundAspectRatio) {
//					this.parent.keepBackgroundAspectRatio = false;
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.off"));
//				} else {
//					this.parent.keepBackgroundAspectRatio = true;
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.on"));
//				}
//			});
//			this.addContent(backgroundAspectButton);
//
//			/** SLIDE BACKGROUND IMAGE **/
//			String slideBackgroundLabel = I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.on");
//			if (!this.parent.panorama) {
//				slideBackgroundLabel = I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.off");
//			}
//			AdvancedButton slideBackgroundButton = new AdvancedButton(0, 0, 0, 16, slideBackgroundLabel, true, (press) -> {
//				if (this.parent.panorama) {
//					this.parent.panorama = false;
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.off"));
//				} else {
//					this.parent.panorama = true;
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.on"));
//				}
//			});
//			slideBackgroundButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.layoutoptions.backgroundoptions.slideimage.btn.desc")));
//			this.addContent(slideBackgroundButton);
//
//			/** RESTART ANIMATION ON LOAD **/
//			AdvancedButton restartOnLoadButton = new AdvancedButton(0, 0, 0, 16, "", true, (press) -> {
//				if (this.parent.restartAnimationBackgroundOnLoad) {
//					this.parent.restartAnimationBackgroundOnLoad = false;
//				} else {
//					this.parent.restartAnimationBackgroundOnLoad = true;
//				}
//			}) {
//				@Override
//				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					if (parent.backgroundAnimation != null) {
//						this.active = true;
//					} else {
//						this.active = false;
//					}
//					if (parent.restartAnimationBackgroundOnLoad) {
//						this.setMessage(I18n.get("fancymenu.helper.editor.backgrounds.animation.restart_on_load.on"));
//					} else {
//						this.setMessage(I18n.get("fancymenu.helper.editor.backgrounds.animation.restart_on_load.off"));
//					}
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			restartOnLoadButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.backgrounds.animation.restart_on_load.desc")));
//			this.addContent(restartOnLoadButton);
//
//			this.addSeparator();
//
//			/** EDIT MENU TITLE **/
//			String defaultMenuTitleRaw = "";
//			if (this.parent.layoutTargetScreen.getTitle() != null) {
//				defaultMenuTitleRaw = this.parent.layoutTargetScreen.getTitle().getString();
//			}
//			String defaultMenuTitle = defaultMenuTitleRaw;
//			AdvancedButton editMenuTitleButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.edit_menu_title"), true, (press) -> {
//
//				TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.helper.editor.edit_menu_title")), this.parent, null, (call) -> {
//					if (call != null) {
//						if (!call.equals(defaultMenuTitle)) {
//							if ((this.parent.customMenuTitle == null) || !this.parent.customMenuTitle.equals(call)) {
//								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//							}
//							this.parent.customMenuTitle = call;
//						} else {
//							if (this.parent.customMenuTitle != null) {
//								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//							}
//							this.parent.customMenuTitle = null;
//						}
//					}
//				});
//				s.multilineMode = false;
//				if (this.parent.customMenuTitle != null) {
//					s.setText(this.parent.customMenuTitle);
//				} else {
//					s.setText(defaultMenuTitle);
//				}
//				Minecraft.getInstance().setScreen(s);
//
//			});
//			editMenuTitleButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.edit_menu_title.desc")));
//			this.addContent(editMenuTitleButton);
//
//			/** RESET MENU TITLE **/
//			AdvancedButton resetMenuTitleButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.edit_menu_title.reset"), true, (press) -> {
//				if (this.parent.customMenuTitle != null) {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				}
//				this.parent.customMenuTitle = null;
//			});
//			resetMenuTitleButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.edit_menu_title.reset.desc")));
//			this.addContent(resetMenuTitleButton);
//
//			this.addSeparator();
//
//			/** RANDOM MODE **/
//			String randomModeString = I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.on");
//			if (!this.parent.randomMode) {
//				randomModeString = I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.off");
//			}
//			AdvancedButton randomModeButton = new AdvancedButton(0, 0, 0, 16, randomModeString, true, (press) -> {
//				if (this.parent.randomMode) {
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.off"));
//					this.parent.randomMode = false;
//				} else {
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.on"));
//					this.parent.randomMode = true;
//				}
//			});
//			randomModeButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.btn.desc")));
//			this.addContent(randomModeButton);
//
//			AdvancedButton randomModeGroupButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), true, (press) -> {
//				FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//					if (call != null) {
//						if (!MathUtils.isInteger(call)) {
//							call = "1";
//						}
//						if (!call.equalsIgnoreCase(this.parent.randomGroup)) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//						this.parent.randomGroup = call;
//					}
//				});
//				if (this.parent.randomGroup != null) {
//					pop.setText(this.parent.randomGroup);
//				}
//				PopupHandler.displayPopup(pop);
//			}) {
//				@Override
//				public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
//					if (parent.randomMode) {
//						this.active = true;
//					} else {
//						this.active = false;
//					}
//					super.render(matrixStack, mouseX, mouseY, partialTicks);
//				}
//			};
//			randomModeGroupButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup.btn.desc")));
//			this.addContent(randomModeGroupButton);
//
//			String randomModeFirstTimeString = I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.on");
//			if (!this.parent.randomOnlyFirstTime) {
//				randomModeFirstTimeString = I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.off");
//			}
//			AdvancedButton randomModeFirstTimeButton = new AdvancedButton(0, 0, 0, 16, randomModeFirstTimeString, true, (press) -> {
//				if (this.parent.randomOnlyFirstTime) {
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.off"));
//					this.parent.randomOnlyFirstTime = false;
//				} else {
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.on"));
//					this.parent.randomOnlyFirstTime = true;
//				}
//			}) {
//				@Override
//				public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
//					if (parent.randomMode) {
//						this.active = true;
//					} else {
//						this.active = false;
//					}
//					super.render(matrixStack, mouseX, mouseY, partialTicks);
//				}
//			};
//			randomModeFirstTimeButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.btn.desc")));
//			this.addContent(randomModeFirstTimeButton);
//
//			this.addSeparator();
//
//			/** RENDERING ORDER **/
//			ContextMenu renderingOrderMenu = new ContextMenu();
//			renderingOrderMenu.setAutoclose(true);
//			this.addChild(renderingOrderMenu);
//
//			this.renderingOrderBackgroundButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.renderorder.background"), true, (press) -> {
//				((AdvancedButton)press).setMessage("§a" + I18n.get("fancymenu.editor.layoutoptions.renderorder.background"));
//				this.renderingOrderForegroundButton.setMessage(I18n.get("fancymenu.editor.layoutoptions.renderorder.foreground"));
//				if (!this.parent.renderorder.equals("background")) {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				}
//
//				this.parent.renderorder = "background";
//			});
//			renderingOrderMenu.addContent(renderingOrderBackgroundButton);
//
//			this.renderingOrderForegroundButton = new AdvancedButton(0, 0, 0, 16, "§a" + I18n.get("fancymenu.editor.layoutoptions.renderorder.foreground"), true, (press) -> {
//				((AdvancedButton)press).setMessage("§a" + I18n.get("fancymenu.editor.layoutoptions.renderorder.foreground"));
//				this.renderingOrderBackgroundButton.setMessage(I18n.get("fancymenu.editor.layoutoptions.renderorder.background"));
//				if (!this.parent.renderorder.equals("foreground")) {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				}
//
//				this.parent.renderorder = "foreground";
//			});
//			renderingOrderMenu.addContent(renderingOrderForegroundButton);
//
//			if (this.parent.renderorder.equals("background")) {
//				renderingOrderForegroundButton.setMessage(I18n.get("fancymenu.editor.layoutoptions.renderorder.foreground"));
//				renderingOrderBackgroundButton.setMessage("§a" + I18n.get("fancymenu.editor.layoutoptions.renderorder.background"));
//			}
//
//			AdvancedButton renderingOrderButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.renderorder"), true, (press) -> {
//				renderingOrderMenu.setParentButton((AdvancedButton) press);
//				renderingOrderMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(renderingOrderButton);
//
//			/** AUTO-SCALING **/
//			String autoScalingLabel = I18n.get("fancymenu.helper.editor.properties.autoscale.off");
//			if ((this.parent.autoScalingWidth != 0) && (this.parent.autoScalingHeight != 0)) {
//				autoScalingLabel = I18n.get("fancymenu.helper.editor.properties.autoscale.on");
//			}
//			AdvancedButton autoScalingButton = new AdvancedButton(0, 0, 0, 16, autoScalingLabel, true, (press) -> {
//				if ((this.parent.autoScalingWidth != 0) && (this.parent.autoScalingHeight != 0)) {
//					((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.properties.autoscale.off"));
//					this.parent.autoScalingWidth = 0;
//					this.parent.autoScalingHeight = 0;
//					this.parent.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
//				} else {
//					PopupHandler.displayPopup(new AutoScalingPopup(this.parent, (call) -> {
//						if (call) {
//							((AdvancedButton)press).setMessage(I18n.get("fancymenu.helper.editor.properties.autoscale.on"));
//							this.parent.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
//						}
//					}));
//				}
//			}) {
//				@Override
//				public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					if (parent.scale != 0) {
//						this.active = true;
//						this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.properties.autoscale.btn.desc")));
//					} else {
//						this.active = false;
//						this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.properties.autoscale.forced_scale_needed")));
//					}
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			this.addContent(autoScalingButton);
//
//			/** FORCE GUI SCALE **/
//			AdvancedButton menuScaleButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.rightclick.scale"), true, (press) -> {
//				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.editor.rightclick.scale"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//					if (call != null) {
//						int s = 0;
//						if (MathUtils.isInteger(call)) {
//							s = Integer.parseInt(call);
//						}
//						if (s < 0) {
//							UIBase.displayNotification(I18n.get("fancymenu.editor.rightclick.scale.invalid"), "", "", "", "");
//						} else {
//							if (this.parent.scale != s) {
//								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//							}
//							this.parent.scale = s;
//							this.parent.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
//						}
//					}
//				});
//				p.setText("" + this.parent.scale);
//				PopupHandler.displayPopup(p);
//			});
//			menuScaleButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.properties.scale.btn.desc")));
//			this.addContent(menuScaleButton);
//
//			/** OPEN/CLOSE SOUND **/
//			ContextMenu openCloseSoundMenu = new ContextMenu();
//			openCloseSoundMenu.setAutoclose(true);
//			this.addChild(openCloseSoundMenu);
//
//			AdvancedButton openSoundBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.openaudio"), true, (press) -> {
//				ChooseFilePopup p = new ChooseFilePopup((call) -> {
//					if (call != null) {
//						if (call.length() < 3) {
//							if (this.parent.openAudio != null) {
//								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//							}
//							this.parent.openAudio = null;
//						} else {
//							File f = new File(call);
//							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//								f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
//							}
//							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
//								if (this.parent.openAudio != call) {
//									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//								}
//								this.parent.openAudio = call;
//							} else {
//								UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.invalidaudio.title"), "", I18n.get("fancymenu.editor.invalidaudio.desc"), "", "", "", "", "");
//							}
//						}
//					} else {
//						if (this.parent.openAudio != null) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//						this.parent.openAudio = null;
//					}
//				}, "wav");
//				if (this.parent.openAudio != null) {
//					p.setText(this.parent.openAudio);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			openSoundBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.openaudio.desc")));
//			openCloseSoundMenu.addContent(openSoundBtn);
//
//			AdvancedButton resetOpenBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.openaudio.reset"), true, (press) -> {
//				if (this.parent.openAudio != null) {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				}
//				this.parent.openAudio = null;
//			});
//			resetOpenBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.opencloseaudio.reset.desc")));
//			openCloseSoundMenu.addContent(resetOpenBtn);
//
//			AdvancedButton closeSoundBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.closeaudio"), true, (press) -> {
//				ChooseFilePopup p = new ChooseFilePopup((call) -> {
//					if (call != null) {
//						if (call.length() < 3) {
//							if (this.parent.closeAudio != null) {
//								this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//							}
//							this.parent.closeAudio = null;
//						} else {
//							File f = new File(call);
//							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//								f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
//							}
//							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
//								if (this.parent.closeAudio != call) {
//									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//								}
//								this.parent.closeAudio = call;
//							} else {
//								UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.invalidaudio.title"), "", I18n.get("fancymenu.editor.invalidaudio.desc"), "", "", "", "", "");
//							}
//						}
//					} else {
//						if (this.parent.closeAudio != null) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//						this.parent.closeAudio = null;
//					}
//				}, "wav");
//				if (this.parent.closeAudio != null) {
//					p.setText(this.parent.closeAudio);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			closeSoundBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.closeaudio.desc")));
//			openCloseSoundMenu.addContent(closeSoundBtn);
//
//			AdvancedButton resetCloseBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.closeaudio.reset"), true, (press) -> {
//				if (this.parent.closeAudio != null) {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				}
//				this.parent.closeAudio = null;
//			});
//			resetCloseBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.opencloseaudio.reset.desc")));
//			openCloseSoundMenu.addContent(resetCloseBtn);
//
//			AdvancedButton openCloseSoundButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.opencloseaudio"), true, (press) -> {
//				openCloseSoundMenu.setParentButton((AdvancedButton) press);
//				openCloseSoundMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			openCloseSoundButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.opencloseaudio.desc")));
//			this.addContent(openCloseSoundButton);
//
//			this.addSeparator();
//
//
//			/** LOADING REQUIREMENTS [LAYOUT-WIDE] **/
//			AdvancedButton loadingRequirementsButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.loading_requirement.layouts.loading_requirements"), (press) -> {
//				ManageRequirementsScreen s = new ManageRequirementsScreen(this.parent, this.parent.layoutWideLoadingRequirementContainer, (call) -> {});
//				this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				Minecraft.getInstance().setScreen(s);
//			});
//			loadingRequirementsButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.loading_requirement.layouts.loading_requirements.desc")));
//			this.addContent(loadingRequirementsButton);
//
//
//			/** WINDOW SIZE RESTRICTIONS **/
//			ContextMenu windowSizeMenu = new ContextMenu();
//			windowSizeMenu.setAutoclose(true);
//			this.addChild(windowSizeMenu);
//
//			AdvancedButton biggerThanButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.windowsize.biggerthan"), true, (press) -> {
//				PopupHandler.displayPopup(new WindowSizePopup(this.parent, WindowSizePopup.ActionType.BIGGERTHAN));
//			});
//			windowSizeMenu.addContent(biggerThanButton);
//
//			AdvancedButton smallerThanButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.windowsize.smallerthan"), true, (press) -> {
//				PopupHandler.displayPopup(new WindowSizePopup(this.parent, WindowSizePopup.ActionType.SMALLERTHAN));
//			});
//			windowSizeMenu.addContent(smallerThanButton);
//
//			AdvancedButton windowSizeButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.windowsize"), true, (press) -> {
//				windowSizeMenu.setParentButton((AdvancedButton) press);
//				windowSizeMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			windowSizeButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.windowsizerestrictions.btn.desc")));
//			this.addContent(windowSizeButton);
//
//			AdvancedButton resetWindowSizeRestrictionsButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.windowsizerestrictions.reset"), true, (press) -> {
//				this.parent.biggerThanWidth = 0;
//				this.parent.biggerThanHeight = 0;
//				this.parent.smallerThanWidth = 0;
//				this.parent.smallerThanHeight = 0;
//			});
//			resetWindowSizeRestrictionsButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.windowsizerestrictions.reset.btn.desc")));
//			this.addContent(resetWindowSizeRestrictionsButton);
//
//			this.addSeparator();
//
//			/** REQUIRED MODS **/
//			AdvancedButton requiredModsButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.requiredmods"), true, (press) -> {
//				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.layoutoptions.requiredmods.desc"), null, 240, (call) -> {
//					if (call != null) {
//						if (this.parent.requiredmods != call) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//
//						this.parent.requiredmods = call;
//					}
//				});
//				if (this.parent.requiredmods != null) {
//					p.setText(this.parent.requiredmods);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			this.addContent(requiredModsButton);
//
//			/** MC VERSION **/
//			ContextMenu mcVersionMenu = new ContextMenu();
//			mcVersionMenu.setAutoclose(true);
//			this.addChild(mcVersionMenu);
//
//			AdvancedButton minMcVersionButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.version.minimum"), true, (press) -> {
//				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.layoutoptions.version.minimum.mc"), null, 240, (call) -> {
//					if (call != null) {
//						if (this.parent.minimumMC != call) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//
//						this.parent.minimumMC = call;
//					}
//				});
//				if (this.parent.minimumMC != null) {
//					p.setText(this.parent.minimumMC);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			mcVersionMenu.addContent(minMcVersionButton);
//
//			AdvancedButton maxMcVersionButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.version.maximum"), true, (press) -> {
//				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.layoutoptions.version.maximum.mc"), null, 240, (call) -> {
//					if (call != null) {
//						if (this.parent.maximumMC != call) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//
//						this.parent.maximumMC = call;
//					}
//				});
//				if (this.parent.maximumMC != null) {
//					p.setText(this.parent.maximumMC);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			mcVersionMenu.addContent(maxMcVersionButton);
//
//			AdvancedButton mcVersionButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.version.mc"), true, (press) -> {
//				mcVersionMenu.setParentButton((AdvancedButton) press);
//				mcVersionMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(mcVersionButton);
//
//			/** FM VERSION **/
//			ContextMenu fmVersionMenu = new ContextMenu();
//			fmVersionMenu.setAutoclose(true);
//			this.addChild(fmVersionMenu);
//
//			AdvancedButton minFmVersionButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.version.minimum"), true, (press) -> {
//				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.layoutoptions.version.minimum.fm"), null, 240, (call) -> {
//					if (call != null) {
//						if (this.parent.minimumFM != call) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//
//						this.parent.minimumFM = call;
//					}
//				});
//				if (this.parent.minimumFM != null) {
//					p.setText(this.parent.minimumFM);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			fmVersionMenu.addContent(minFmVersionButton);
//
//			AdvancedButton maxFmVersionButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.version.maximum"), true, (press) -> {
//				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.layoutoptions.version.maximum.fm"), null, 240, (call) -> {
//					if (call != null) {
//						if (this.parent.maximumFM != call) {
//							this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						}
//
//						this.parent.maximumFM = call;
//					}
//				});
//				if (this.parent.maximumFM != null) {
//					p.setText(this.parent.maximumFM);
//				}
//				PopupHandler.displayPopup(p);
//			});
//			fmVersionMenu.addContent(maxFmVersionButton);
//
//			AdvancedButton fmVersionButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutoptions.version.fm"), true, (press) -> {
//				fmVersionMenu.setParentButton((AdvancedButton) press);
//				fmVersionMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(fmVersionButton);
//
//			if (this.isRightclickOpened) {
//				this.addSeparator();
//			}
//
//			/** PASTE **/
//			AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.edit.paste"), (press) -> {
//				this.parent.pasteElements();
//			});
//			if (this.isRightclickOpened) {
//				this.addContent(pasteButton);
//			}
//
//			/** NEW ELEMENT **/
//			NewElementContextMenu newElementMenu = new NewElementContextMenu(this.parent);
//			newElementMenu.setAutoclose(true);
//			this.addChild(newElementMenu);
//
//			AdvancedButton newElementButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.layoutproperties.newelement"), (press) -> {
//				newElementMenu.setParentButton((AdvancedButton) press);
//				newElementMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			if (this.isRightclickOpened) {
//				this.addContent(newElementButton);
//			}
//
//
//			super.openMenuAt(x, y, screenWidth, screenHeight);
//		}
//
//	}
//
//	public static class NewElementContextMenu extends ContextMenu {
//
//		private LayoutEditorScreen parent;
//
//		public NewElementContextMenu(LayoutEditorScreen parent) {
//			this.parent = parent;
//		}
//
//		@Override
//		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
//
//			this.content.clear();
//
//			/** IMAGE **/
//			AdvancedButton imageButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.image"), (press) -> {
//				PopupHandler.displayPopup(new ChooseFilePopup(this.parent::addTexture, "jpg", "jpeg", "png", "gif"));
//			});
//			this.addContent(imageButton);
//
//			/** WEB IMAGE **/
//			AdvancedButton webImageButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.webimage"), (press) -> {
//
//				TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.editor.elements.web_image.enter_url")), this.parent, null, this.parent::addWebTexture);
//				s.multilineMode = false;
//				Minecraft.getInstance().setScreen(s);
//
//			});
//			this.addContent(webImageButton);
//
//
////			/** TEXT **/
////			AdvancedButton textButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.text"), (press) -> {
////				PopupHandler.displayPopup(new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.add.text.newtext") + ":", null, 240, this.parent::addText));
////			});
////			this.addContent(textButton);
////
////			/** WEB TEXT **/
////			AdvancedButton webTextButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.webtext"), (press) -> {
////				PopupHandler.displayPopup(new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.web.enterurl"), null, 240, this.parent::addWebText));
////			});
////			this.addContent(webTextButton);
//
//			/** SPLASH TEXT **/
//			ContextMenu splashMenu = new ContextMenu();
//			splashMenu.setAutoclose(true);
//			this.addChild(splashMenu);
//
//			AdvancedButton singleSplashButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.add.splash.single"), true, (press) -> {
//
//				TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.editor.elements.splash.single.enter_text")), this.parent, null, this.parent::addSingleSplashText);
//				s.multilineMode = false;
//				Minecraft.getInstance().setScreen(s);
//
//			});
//			singleSplashButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.add.splash.single.desc")));
//			splashMenu.addContent(singleSplashButton);
//
//			AdvancedButton multiSplashButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.add.splash.multi"), true, (press) -> {
//				PopupHandler.displayPopup(new ChooseFilePopup(this.parent::addMultiSplashText, "txt"));
//			});
//			multiSplashButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.add.splash.multi.desc")));
//			splashMenu.addContent(multiSplashButton);
//
//			AdvancedButton vanillaLikeSplashButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.splash.vanilla_like"), true, (press) -> {
//				this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//				PropertyContainer sec = new PropertyContainer("customization");
//				sec.putProperty("action", "addsplash");
//				sec.putProperty("vanilla-like", "true");
//				sec.putProperty("y", "" + (int)(this.parent.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//				SplashTextCustomizationItem i = new SplashTextCustomizationItem(sec);
//				this.parent.addContent(new LayoutSplashText(i, this.parent));
//			});
//			vanillaLikeSplashButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.splash.vanilla_like.desc")));
//			splashMenu.addContent(vanillaLikeSplashButton);
//
//			AdvancedButton splashButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.splash"), (press) -> {
//				splashMenu.setParentButton((AdvancedButton) press);
//				splashMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(splashButton);
//
//			/** BUTTON **/
//			AdvancedButton buttonButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.button"), (press) -> {
//
//				this.parent.addButton("New Button");
//
//			});
//			this.addContent(buttonButton);
//
//			/** ANIMATION **/
//			ContextMenu animationMenu = new ContextMenu();
//			animationMenu.setAutoclose(true);
//			this.addChild(animationMenu);
//
//			AdvancedButton inputAnimationButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.animation.entername"), true, (press) -> {
//				PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.add.animation.entername.title") + ":", null, 240, this.parent::addAnimation));
//			});
//			animationMenu.addContent(inputAnimationButton);
//
//			animationMenu.addSeparator();
//
//			for (String s : AnimationHandler.getCustomAnimationNames()) {
//				AdvancedButton aniB = new AdvancedButton(0, 0, 0, 20, s, true, (press) -> {
//					this.parent.addAnimation(s);
//				});
//				animationMenu.addContent(aniB);
//			}
//
//			AdvancedButton animationButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.animation"), (press) -> {
//				animationMenu.setParentButton((AdvancedButton) press);
//				animationMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(animationButton);
//
//			/** SLIDESHOW **/
//			ContextMenu slideshowMenu = new ContextMenu();
//			slideshowMenu.setAutoclose(true);
//			this.addChild(slideshowMenu);
//
//			AdvancedButton inputSlideshowButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.slideshow.entername"), true, (press) -> {
//				PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.add.slideshow.entername.title") + ":", null, 240, this.parent::addSlideshow));
//			});
//			slideshowMenu.addContent(inputSlideshowButton);
//
//			slideshowMenu.addSeparator();
//
//			for (String s : SlideshowHandler.getSlideshowNames()) {
//				String name = s;
//				if (Minecraft.getInstance().font.width(name) > 90) {
//					name = Minecraft.getInstance().font.plainSubstrByWidth(name, 90) + "..";
//				}
//
//				AdvancedButton slideshowB = new AdvancedButton(0, 0, 0, 20, name, true, (press) -> {
//					if (SlideshowHandler.slideshowExists(s)) {
//						this.parent.addSlideshow(s);
//					}
//				});
//				slideshowMenu.addContent(slideshowB);
//			}
//
//			AdvancedButton slideshowButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.slideshow"), (press) -> {
//				slideshowMenu.setParentButton((AdvancedButton) press);
//				slideshowMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(slideshowButton);
//
//			/** SHAPE **/
//			ContextMenu shapesMenu = new ContextMenu();
//			shapesMenu.setAutoclose(true);
//			this.addChild(shapesMenu);
//
//			AdvancedButton addRectangleButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.shape.rectangle"), (press) -> {
//				this.parent.addShape(Shape.RECTANGLE);
//			});
//			shapesMenu.addContent(addRectangleButton);
//
//			AdvancedButton shapesButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.shape"), (press) -> {
//				shapesMenu.setParentButton((AdvancedButton) press);
//				shapesMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//			});
//			this.addContent(shapesButton);
//
//			/** DUMMY BUTTON: INSTALL AUDIO EXTENSION **/
//			AdvancedButton audioButton = new AdvancedButton(0, 0, 0, 20, I18n.get("fancymenu.editor.add.audio"), (press) -> {
//				ActionExecutor.openWebLink("https://www.curseforge.com/minecraft/mc-mods/audio-extension-for-fancymenu-" + FancyMenu.MOD_LOADER);
//			});
//			audioButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.extension.dummy.audio.btn.desc")));
//			if (!FancyMenu.isAudioExtensionLoaded()) {
//				this.addContent(audioButton);
//			}
//
//			/** CUSTOM ITEMS (API) **/
//			for (ElementBuilder c : ElementRegistry.getBuilders()) {
//
//				AdvancedButton cusItemButton = new AdvancedButton(0, 0, 0, 20, c.getDisplayName(), (press) -> {
//					this.parent.addContent(c.wrapIntoEditorElement(c.buildDefaultInstance(), this.parent));
//				});
//				String[] desc = c.getDescription();
//				if ((desc != null) && (desc.length > 0)) {
//					cusItemButton.setDescription(desc);
//				}
//				this.addContent(cusItemButton);
//
//			}
//
//			super.openMenuAt(x, y, screenWidth, screenHeight);
//		}
//
//	}
//
//	public static class ManageAudioContextMenu extends ContextMenu {
//
//		private LayoutEditorScreen parent;
//
//		public ManageAudioContextMenu(LayoutEditorScreen parent) {
//			this.parent = parent;
//		}
//
//		@Override
//		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
//
//			this.content.clear();
//
//			if (this.parent.audio.isEmpty()) {
//
//				AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.empty"), true, (press) -> {});
//				this.addContent(bt);
//
//			} else {
//
//				for (Map.Entry<String, Boolean> m : this.parent.audio.entrySet()) {
//
//					String label = new File(m.getKey()).getName();
//					if (Minecraft.getInstance().font.width(label) > 200) {
//						label = Minecraft.getInstance().font.plainSubstrByWidth(label, 200) + "..";
//					}
//
//					ContextMenu actionsMenu = new ContextMenu();
//					actionsMenu.setAutoclose(true);
//					this.addChild(actionsMenu);
//
//					AdvancedButton deleteButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.audio.delete"), true, (press2) -> {
//						this.closeMenu();
//						PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
//							if (call) {
//								this.parent.audio.remove(m.getKey());
//								SoundHandler.stopSound(m.getKey());
//								ScreenCustomization.unregisterSound(m.getKey());
//							}
//						}, "§c§l" + I18n.get("fancymenu.editor.messages.sure"), "", "", I18n.get("fancymenu.editor.audio.delete.msg"), "", ""));
//					});
//					actionsMenu.addContent(deleteButton);
//
//					String lab = I18n.get("fancymenu.editor.element.manageaudio.loop.off");
//					if (m.getValue()) {
//						lab = I18n.get("fancymenu.editor.element.manageaudio.loop.on");
//					}
//					AdvancedButton toggleLoopButton = new AdvancedButton(0, 0, 0, 16, lab, true, (press2) -> {
//						if (((AdvancedButton)press2).getMessage().getString().equals(I18n.get("fancymenu.editor.element.manageaudio.loop.off"))) {
//							SoundHandler.setLooped(m.getKey(), true);
//							this.parent.audio.put(m.getKey(), true);
//							((AdvancedButton)press2).setMessage(I18n.get("fancymenu.editor.element.manageaudio.loop.on"));;
//						} else {
//							SoundHandler.setLooped(m.getKey(), false);
//							this.parent.audio.put(m.getKey(), false);
//							((AdvancedButton)press2).setMessage(I18n.get("fancymenu.editor.element.manageaudio.loop.off"));;
//						}
//					});
//					actionsMenu.addContent(toggleLoopButton);
//
//					AdvancedButton actionsButton = new AdvancedButton(0, 0, 0, 16, label, true, (press) -> {
//						actionsMenu.setParentButton((AdvancedButton) press);
//						actionsMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//					});
//					this.addContent(actionsButton);
//
//				}
//			}
//
//
//			super.openMenuAt(x, y, screenWidth, screenHeight);
//		}
//
//	}
//
//	public static class MultiselectContextMenu extends ContextMenu {
//
//		private LayoutEditorScreen parent;
//
//		public MultiselectContextMenu(LayoutEditorScreen parent) {
//			this.parent = parent;
//		}
//
//		@Override
//		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
//
//			this.content.clear();
//
//			if (this.parent.isObjectFocused()) {
//
//				this.parent.focusedObjectsCache = this.parent.getFocusedObjects();
//
//				this.parent.multiselectStretchedX = false;
//				this.parent.multiselectStretchedY = false;
//
//				/** DELETE ALL **/
//				AdvancedButton deleteBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.multiselect.object.deleteall"), true, (press) -> {
//					this.parent.deleteFocusedObjects();
//				});
//				deleteBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.object.deleteall.btndesc")));
//				this.addContent(deleteBtn);
//
//				/** STRETCH ALL **/
//				ContextMenu stretchMenu = new ContextMenu();
//				stretchMenu.setAutoclose(true);
//				this.addChild(stretchMenu);
//
//				AdvancedButton stretchXBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.object.stretch.x"), true, (press) -> {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//
//					for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//						if (o.isStretchable()) {
//							o.setStretchedX(!this.parent.multiselectStretchedX, false);
//						}
//					}
//
//					this.parent.multiselectStretchedX = !this.parent.multiselectStretchedX;
//
//					if (!this.parent.multiselectStretchedX) {
//						press.setMessage(Component.literal(I18n.get("fancymenu.editor.object.stretch.x")));
//					} else {
//						press.setMessage(Component.literal("§a" + I18n.get("fancymenu.editor.object.stretch.x")));
//					}
//
//				});
//				stretchMenu.addContent(stretchXBtn);
//
//				AdvancedButton stretchYBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.object.stretch.y"), true, (press) -> {
//					this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//
//					for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//						if (o.isStretchable()) {
//							o.setStretchedY(!this.parent.multiselectStretchedY, false);
//						}
//					}
//
//					this.parent.multiselectStretchedY = !this.parent.multiselectStretchedY;
//
//					if (!this.parent.multiselectStretchedY) {
//						press.setMessage(Component.literal(I18n.get("fancymenu.editor.object.stretch.y")));
//					} else {
//						press.setMessage(Component.literal("§a" + I18n.get("fancymenu.editor.object.stretch.y")));
//					}
//
//				});
//				stretchMenu.addContent(stretchYBtn);
//
//				AdvancedButton stretchBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.multiselect.object.stretchall"), true, (press) -> {
//					stretchMenu.setParentButton((AdvancedButton) press);
//					stretchMenu.openMenuAt(0, press.y, screenWidth, screenHeight);
//				});
//				stretchBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.object.stretchall.btndesc")));
//				this.addContent(stretchBtn);
//
//				/** COPY **/
//				AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.edit.copy"), (press) -> {
//					this.parent.copySelectedElements();
//				});
//				this.addContent(copyButton);
//
//				/** PASTE **/
//				AdvancedButton pasteButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.edit.paste"), (press) -> {
//					this.parent.pasteElements();
//				});
//				this.addContent(pasteButton);
//
//				boolean allVanillaBtns = true;
//				boolean allBtns = true;
//				for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//					if (!(o instanceof LayoutVanillaButton)) {
//						allVanillaBtns = false;
//					}
//					if (!(o instanceof LayoutVanillaButton) && !(o instanceof LayoutButton)) {
//						allBtns = false;
//					}
//				}
//				if (this.parent.focusedObjectsCache.isEmpty()) {
//					allVanillaBtns = false;
//					allBtns = false;
//				}
//
//				if (allVanillaBtns) {
//
//					/** VANILLA: RESET ORIENTATION **/
//					AdvancedButton resetOriBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.multiselect.vanillabutton.resetorientation"), true, (press) -> {
//						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//
//						for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//							if (o instanceof LayoutVanillaButton) {
//								LayoutVanillaButton vb = (LayoutVanillaButton) o;
//								vb.element.anchorPoint = "original";
//								vb.element.baseX = vb.button.x;
//								vb.element.baseY = vb.button.y;
//								vb.element.setWidth(vb.button.width);
//								vb.element.setHeight(vb.button.height);
//							}
//						}
//						this.closeMenu();
//						Minecraft.getInstance().setScreen(this.parent);
//					});
//					resetOriBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.vanillabutton.resetorientation.btndesc")));
//					this.addContent(resetOriBtn);
//
//					/** VANILLA: DELETE **/
//					AdvancedButton hideAllBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.multiselect.vanillabutton.hideall"), true, (press) -> {
//						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						this.parent.history.setPreventSnapshotSaving(true);
//
//						for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//							if (o instanceof LayoutVanillaButton) {
//								LayoutVanillaButton vb = (LayoutVanillaButton) o;
//								this.parent.hideVanillaButton(vb);
//							}
//						}
//
//						this.parent.focusedObjects.clear();
//						this.parent.focusedObjectsCache.clear();
//						this.parent.multiselectRightclickMenu.closeMenu();
//
//						this.parent.history.setPreventSnapshotSaving(false);
//					});
//					hideAllBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.vanillabutton.hideall.btndesc")));
//					this.addContent(hideAllBtn);
//
//				}
//
//				if (allBtns) {
//
//					/** BUTTONS: BACKGROUND **/
//					AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.helper.editor.items.buttons.buttonbackground"), true, (press) -> {
//						this.parent.setButtonTexturesForFocusedObjects();
//					});
//					buttonBackgroundButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.button.buttontexture.btndesc")));
//					this.addContent(buttonBackgroundButton);
//
//					/** BUTTONS: CLICK SOUND **/
//					AdvancedButton clickSoundBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.clicksound"), true, (press) -> {
//						ChooseFilePopup cf = new ChooseFilePopup((call) -> {
//							if (call != null) {
//								File f = new File(call);
//								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
//								}
//								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//									this.parent.history.setPreventSnapshotSaving(true);
//
//									for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//										if (o instanceof LayoutVanillaButton) {
//											LayoutVanillaButton vb = (LayoutVanillaButton) o;
//											vb.customizationContainer.clickSound = call;
//										} else if (o instanceof LayoutButton) {
//											LayoutButton lb = (LayoutButton) o;
//											lb.customizationContainer.clickSound = call;
//										}
//									}
//
//									this.parent.history.setPreventSnapshotSaving(false);
//								} else {
//									UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.invalidaudio.title"), "", I18n.get("fancymenu.editor.invalidaudio.desc"), "", "", "", "", "", "");
//								}
//							}
//						}, "wav");
//
//						PopupHandler.displayPopup(cf);
//					});
//					clickSoundBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.button.clicksound.btndesc")));
//					this.addContent(clickSoundBtn);
//
//					/** BUTTONS: RESET CLICK SOUND **/
//					AdvancedButton resetClickSoundBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.clicksound.reset"), true, (press) -> {
//
//						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						this.parent.history.setPreventSnapshotSaving(true);
//
//						for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//							if (o instanceof LayoutVanillaButton) {
//								LayoutVanillaButton vb = (LayoutVanillaButton) o;
//								vb.customizationContainer.clickSound = null;
//							} else if (o instanceof LayoutButton) {
//								LayoutButton lb = (LayoutButton) o;
//								lb.customizationContainer.clickSound = null;
//							}
//						}
//
//						this.parent.history.setPreventSnapshotSaving(false);
//
//					});
//					resetClickSoundBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.button.clicksound.reset.btndesc")));
//					this.addContent(resetClickSoundBtn);
//
//					/** BUTTONS: HOVER SOUND **/
//					AdvancedButton hoverSoundBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.hoversound"), true, (press) -> {
//						ChooseFilePopup cf = new ChooseFilePopup((call) -> {
//							if (call != null) {
//								File f = new File(call);
//								if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//									f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + call);
//								}
//								if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//									this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//									this.parent.history.setPreventSnapshotSaving(true);
//
//									for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//										if (o instanceof LayoutVanillaButton) {
//											LayoutVanillaButton vb = (LayoutVanillaButton) o;
//											vb.customizationContainer.hoverSound = call;
//										} else if (o instanceof LayoutButton) {
//											LayoutButton lb = (LayoutButton) o;
//											lb.customizationContainer.hoverSound = call;
//										}
//									}
//
//									this.parent.history.setPreventSnapshotSaving(false);
//								} else {
//									UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.invalidaudio.title"), "", I18n.get("fancymenu.editor.invalidaudio.desc"), "", "", "", "", "", "");
//								}
//							}
//						}, "wav");
//
//						PopupHandler.displayPopup(cf);
//					});
//					hoverSoundBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.button.hoversound.btndesc")));
//					this.addContent(hoverSoundBtn);
//
//					/** BUTTONS: RESET HOVERSOUND **/
//					AdvancedButton resetHoverSoundBtn = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.button.hoversound.reset"), true, (press) -> {
//
//						this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
//						this.parent.history.setPreventSnapshotSaving(true);
//
//						for (AbstractEditorElement o : this.parent.focusedObjectsCache) {
//							if (o instanceof LayoutVanillaButton) {
//								LayoutVanillaButton vb = (LayoutVanillaButton) o;
//								vb.customizationContainer.hoverSound = null;
//							} else if (o instanceof LayoutButton) {
//								LayoutButton lb = (LayoutButton) o;
//								lb.customizationContainer.hoverSound = null;
//							}
//						}
//
//						this.parent.history.setPreventSnapshotSaving(false);
//
//					});
//					resetHoverSoundBtn.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.multiselect.button.hoversound.reset.btndesc")));
//					this.addContent(resetHoverSoundBtn);
//				}
//
//			}
//
//
//			super.openMenuAt(x, y, screenWidth, screenHeight);
//		}
//
//	}
//
//	public static class HiddenVanillaButtonContextMenu extends ContextMenu {
//
//		private LayoutEditorScreen parent;
//
//		public HiddenVanillaButtonContextMenu(LayoutEditorScreen parent) {
//			this.parent = parent;
//		}
//
//		@Override
//		public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
//
//			this.content.clear();
//			this.separators.clear();
//
//			boolean containsHiddenDeeps = false;
//			for (AbstractEditorElement e : this.parent.content) {
//				if (e instanceof AbstractDeepEditorElement) {
//					if (((AbstractDeepEditorElement)e).getDeepCustomizationItem().deepElementHidden) {
//						String name = ((AbstractDeepEditorElement) e).parentDeepElementBuilder.getDisplayName();
//						AdvancedButton hiddenButton = new AdvancedButton(0, 0, 0, 0, name, true, (press) -> {
//							((AbstractDeepEditorElement) e).getDeepCustomizationItem().deepElementHidden = false;
//							this.parent.updateContent();
//							this.closeMenu();
//						});
//						hiddenButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.element.deletedvanillabuttons.entry.desc")));
//						this.addContent(hiddenButton);
//						containsHiddenDeeps = true;
//					}
//				}
//			}
//
//			if (!this.parent.getHiddenButtons().isEmpty()) {
//				for (LayoutVanillaButton b : this.parent.getHiddenButtons()) {
//
//					String name = b.button.getButton().getMessage().getString();
//					AdvancedButton hiddenButton = new AdvancedButton(0, 0, 0, 0, name, true, (press) -> {
//						this.parent.showVanillaButton(b);
//						this.closeMenu();
//					});
//					hiddenButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.element.deletedvanillabuttons.entry.desc")));
//					this.addContent(hiddenButton);
//
//				}
//			} else if (!containsHiddenDeeps) {
//				AdvancedButton emptyButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.empty"), true, (press) -> {});
//				this.addContent(emptyButton);
//			}
//
//			super.openMenuAt(x, y, screenWidth, screenHeight);
//		}
//
//	}
//
//}
