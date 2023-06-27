package de.keksuccino.fancymenu.customization.layout.editor;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.io.Files;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.background.ChooseMenuBackgroundScreen;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.overlay.OverlayButton;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.AdvancedContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ChooseFromStringListScreen;
import de.keksuccino.fancymenu.util.rendering.ui.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.fancymenu.util.rendering.ui.MenuBar.ElementAlignment;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class LayoutEditorUI extends UIBase {

	protected static final ResourceLocation CLOSE_BUTTON_TEXTURE = new ResourceLocation("keksuccino", "close_icon.png");

	public MenuBar topMenuBar;
	public LayoutEditorScreen editor;

	public AdvancedContextMenu layoutMenu = new AdvancedContextMenu();
	public AdvancedContextMenu editMenu = new AdvancedContextMenu();
	public AdvancedContextMenu elementMenu = new AdvancedContextMenu();

	public LayoutEditorUI(LayoutEditorScreen editor) {
		this.editor = editor;
		this.updateTopMenuBar();
	}

	public void updateTopMenuBar() {
		
		try {

			boolean extended = true;
			if (this.topMenuBar != null) {
				extended = this.topMenuBar.isExtended();
			}
			this.topMenuBar = new MenuBar();
			this.topMenuBar.setExtended(extended);

			this.layoutMenu = new AdvancedContextMenu();
			this.editMenu = new AdvancedContextMenu();
			this.elementMenu = new AdvancedContextMenu();

			// LAYOUT TAB
			this.layoutMenu.getContextMenu().setAutoclose(true);
			this.topMenuBar.addChild(this.layoutMenu.getContextMenu(), "fm.editor.ui.tab.layout", ElementAlignment.LEFT);
			OverlayButton layoutTabButton = new OverlayButton(0, 0, 0, 0, Component.translatable("fancymenu.editor.layout"), true, (press) -> {
				layoutMenu.getContextMenu().setParentButton((AdvancedButton)press);
				layoutMenu.openMenu(press.x, press.y + press.getHeight());
			});
			this.topMenuBar.addElement(layoutTabButton, "fm.editor.ui.tab.layout", ElementAlignment.LEFT, false);

			this.layoutMenu.addClickableEntry("new_layout", false, Component.translatable("fancymenu.editor.layout.new"), null, Boolean.class, (entry, inherited, pass) -> {
				this.displayUnsavedWarning((call) -> {
					if (call) {
						SoundRegistry.stopSounds();
						SoundRegistry.resetSounds();
						Minecraft.getInstance().setScreen(new LayoutEditorScreen(this.editor.layoutTargetScreen, new Layout(this.editor.layoutTargetScreen)));
					}
				});
			});

			AdvancedContextMenu openLayoutMenu = this.buildOpenLayoutContextMenu();
			openLayoutMenu.getContextMenu().setAutoclose(true);
			this.layoutMenu.addClickableEntry("open_layout", false, Component.translatable("fancymenu.editor.layout.open"), openLayoutMenu, Boolean.class, (entry, inherited, pass) -> {
				openLayoutMenu.getContextMenu().setParentButton(entry.getButton());
				openLayoutMenu.openMenu(0, entry.getButton().y);
			});

			this.layoutMenu.addClickableEntry("save_layout", false, Component.translatable("fancymenu.editor.layout.save"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.saveLayout();
			});

			this.layoutMenu.addClickableEntry("save_layout_as", false, Component.translatable("fancymenu.editor.layout.saveas"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.saveLayoutAs();
			});

			this.layoutMenu.addSeparatorEntry("separator_1", false);

			AdvancedContextMenu layoutPropertiesMenu = this.buildEditorRightClickContextMenu();
			layoutPropertiesMenu.getContextMenu().setAutoclose(true);
			layoutPropertiesMenu.getContextMenu().setAutoAlignment(false);
			this.layoutMenu.addClickableEntry("layout_properties", false, Component.translatable("fancymenu.editor.layout.properties"), layoutPropertiesMenu, Boolean.class, (entry, inherited, pass) -> {
				layoutPropertiesMenu.getContextMenu().setParentButton(entry.getButton());
				layoutPropertiesMenu.openMenu(0, entry.getButton().y);
			});

			// EDIT TAB
			this.editMenu.getContextMenu().setAutoclose(true);
			this.topMenuBar.addChild(this.editMenu.getContextMenu(), "fm.editor.ui.tab.edit", ElementAlignment.LEFT);
			OverlayButton editTabButton = new OverlayButton(0, 0, 0, 0, Component.translatable("fancymenu.editor.edit"), true, (press) -> {
				editMenu.getContextMenu().setParentButton((AdvancedButton)press);
				editMenu.openMenu(press.x, press.y + press.getHeight());
			});
			this.topMenuBar.addElement(editTabButton, "fm.editor.ui.tab.edit", ElementAlignment.LEFT, false);

			this.editMenu.addClickableEntry("undo_action", false, Component.translatable("fancymenu.editor.edit.undo"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.stepBack();
				try {
					if (Minecraft.getInstance().screen != null) ((LayoutEditorScreen)Minecraft.getInstance().screen).ui.topMenuBar.getChild("fm.editor.ui.tab.edit").openMenuAt(editMenu.getContextMenu().getX(), editMenu.getContextMenu().getY());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			this.editMenu.addClickableEntry("redo_action", false, Component.translatable("fancymenu.editor.edit.redo"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.stepForward();
				try {
					if (Minecraft.getInstance().screen != null) ((LayoutEditorScreen)Minecraft.getInstance().screen).ui.topMenuBar.getChild("fm.editor.ui.tab.edit").openMenuAt(editMenu.getContextMenu().getX(), editMenu.getContextMenu().getY());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

			this.editMenu.addSeparatorEntry("separator_1", false);

			this.editMenu.addClickableEntry("copy", false, Component.translatable("fancymenu.editor.edit.copy"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.copyElementsToClipboard(this.editor.getSelectedElements().toArray(new AbstractEditorElement[0]));
			});

			this.editMenu.addClickableEntry("paste", false, Component.translatable("fancymenu.editor.edit.paste"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.editor.pasteElementsFromClipboard();
			});

			// ELEMENT TAB
			this.elementMenu.getContextMenu().setAutoclose(true);
			this.topMenuBar.addChild(this.elementMenu.getContextMenu(), "fm.editor.ui.tab.element", ElementAlignment.LEFT);
			OverlayButton elementTabButton = new OverlayButton(0, 0, 0, 0, Component.translatable("fancymenu.editor.element"), true, (press) -> {
				elementMenu.getContextMenu().setParentButton((AdvancedButton)press);
				elementMenu.openMenu(press.x, press.y + press.getHeight());
			});
			this.topMenuBar.addElement(elementTabButton, "fm.editor.ui.tab.element", ElementAlignment.LEFT, false);

			AdvancedContextMenu newElementMenu = this.buildNewElementContextMenu();
			newElementMenu.getContextMenu().setAutoclose(true);
			this.elementMenu.addClickableEntry("new_element", false, Component.translatable("fancymenu.editor.element.new"), newElementMenu, Boolean.class, (entry, inherited, pass) -> {
				newElementMenu.getContextMenu().setParentButton(entry.getButton());
				newElementMenu.openMenu(0, entry.getButton().y);
			});

			//TODO add "Editor" tab with entry to toggle grid (+ shortcut text Ctrl+G)

			this.elementMenu.addClickableEntry("hidden_vanilla_elements", false, Component.translatable("fancymenu.fancymenu.editor.element.deleted_vanilla_elements"), null, Boolean.class, (entry, inherited, pass) -> {
				AdvancedContextMenu hiddenVanillaMenu = this.buildHiddenVanillaElementContextMenu();
				hiddenVanillaMenu.getContextMenu().setAutoclose(true);
				this.elementMenu.getContextMenu().addChild(hiddenVanillaMenu.getContextMenu());
				hiddenVanillaMenu.getContextMenu().setParentButton(entry.getButton());
				hiddenVanillaMenu.openMenu(0, entry.getButton().y);
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.element.deleted_vanilla_elements.desc")));

			// CLOSE GUI BUTTON TAB
			AdvancedImageButton exitButton = new AdvancedImageButton(20, 20, 0, 0, CLOSE_BUTTON_TEXTURE, true, (press) -> {
				this.displayUnsavedWarning((call) -> {
					if (call) {
						for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
							if (r instanceof AdvancedAnimation) {
								((AdvancedAnimation)r).stopAudio();
								if (((AdvancedAnimation)r).replayIntro()) {
									r.resetAnimation();
								}
							}
						}
						SoundRegistry.stopSounds();
						SoundRegistry.resetSounds();
						LayoutHandler.reloadLayouts();
						Screen s = this.editor.layoutTargetScreen;
						if (this.editor.layout.isUniversalLayout()) {
							s = (Minecraft.getInstance().level != null) ? new PauseScreen(true) : new TitleScreen();
						}
						Minecraft.getInstance().setScreen(s);
					}
				});
			}) {
				@Override
				@SuppressWarnings("all")
				public void render(PoseStack pose, int mouseX, int mouseY, float partial) {
					TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.exit.desc")), false, true);
					this.width = this.height;
					super.render(pose, mouseX, mouseY, partial);
				}
			};
			exitButton.ignoreLeftMouseDownClickBlock = true;
			exitButton.ignoreBlockedInput = true;
			exitButton.enableRightclick = true;
			this.topMenuBar.addElement(exitButton, "fm.editor.ui.tab.exit", ElementAlignment.RIGHT, false);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void renderTopMenuBar(PoseStack pose, Screen screen) {
		try {
			if ((this.topMenuBar != null) && !PopupHandler.isPopupActive()) {
				this.topMenuBar.render(pose, screen);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void displayUnsavedWarning(Consumer<Boolean> callback) {
		PopupHandler.displayPopup(
				new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, callback, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.warning.unsaved"))
		);
	}

	@NotNull
	public AdvancedContextMenu buildOpenLayoutContextMenu() {

		AdvancedContextMenu menu = new AdvancedContextMenu();

		try {

			String identifier = this.editor.layout.menuIdentifier;

			List<Layout> enabled = LayoutHandler.getEnabledLayoutsForMenuIdentifier(identifier, false);
			if (!enabled.isEmpty()) {
				int count = 0;
				for (Layout l : enabled) {
					String name = "huh? unknown layout, who dis?";
					if (l.layoutFile != null) {
						name = Files.getNameWithoutExtension(l.layoutFile.getName());
					}
					menu.addClickableEntry("layout_" + count, false, Component.literal(name).withStyle(ChatFormatting.GREEN), null, Boolean.class, (entry, inherited, pass) -> {
						this.displayUnsavedWarning((call) -> {
							LayoutHandler.openLayoutEditor(l, this.editor.layoutTargetScreen);
						});
					});
					count++;
				}
			}

			List<Layout> disabled = LayoutHandler.getDisabledLayoutsForMenuIdentifier(identifier);
			if (!disabled.isEmpty()) {
				int count = 0;
				for (Layout l : disabled) {
					String name = "huh? unknown layout, who dis?";
					if (l.layoutFile != null) {
						name = Files.getNameWithoutExtension(l.layoutFile.getName());
					}
					menu.addClickableEntry("layout_" + count, false, Component.literal(name).withStyle(ChatFormatting.RED), null, Boolean.class, (entry, inherited, pass) -> {
						this.displayUnsavedWarning((call) -> {
							LayoutHandler.openLayoutEditor(l, this.editor.layoutTargetScreen);
						});
					});
					count++;
				}
			}

			if (enabled.isEmpty() && disabled.isEmpty()) {
				menu.addClickableEntry("empty", false, Component.translatable("fancymenu.editor.empty"), null, Boolean.class, (entry, inherited, pass) -> {});
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return menu;

	}

	@NotNull
	public AdvancedContextMenu buildEditorRightClickContextMenu() {

		AdvancedContextMenu menu = new AdvancedContextMenu();

		try {

			if (this.editor.layout.isUniversalLayout()) {

				AdvancedContextMenu universalLayoutMenu = new AdvancedContextMenu();
				universalLayoutMenu.getContextMenu().setAutoclose(true);
				menu.addClickableEntry("universal_layout_options", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options"), universalLayoutMenu, Boolean.class, (entry, inherited, pass) -> {
					universalLayoutMenu.getContextMenu().setParentButton(entry.getButton());
					universalLayoutMenu.openMenu(0, entry.getButton().y);
				});

				universalLayoutMenu.addClickableEntry("add_blacklist", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist"), null, Boolean.class, (entry, inherited, pass) -> {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, 240, (call) -> {
						if (call != null) {
							if (!this.editor.layout.universalLayoutMenuBlacklist.contains(call)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
								this.editor.layout.universalLayoutMenuBlacklist.add(call);
							}
						}
					});
					PopupHandler.displayPopup(p);
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist.desc")));

				universalLayoutMenu.addClickableEntry("remove_blacklist", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist"), null, Boolean.class, (entry, inherited, pass) -> {
					ChooseFromStringListScreen s = new ChooseFromStringListScreen(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), this.editor, this.editor.layout.universalLayoutMenuBlacklist, (call) -> {
						if (call != null) {
							Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
								if (call2) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
									this.editor.layout.universalLayoutMenuBlacklist.remove(call);
								}
								Minecraft.getInstance().setScreen(this.editor);
							}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist.confirm")));
						}
					});
					Minecraft.getInstance().setScreen(s);
				});

				universalLayoutMenu.addClickableEntry("clear_blacklist", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist"), null, Boolean.class, (entry, inherited, pass) -> {
					Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
						if (call2) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							this.editor.layout.universalLayoutMenuBlacklist.clear();
						}
						Minecraft.getInstance().setScreen(this.editor);
					}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist.confirm")));
				});

				universalLayoutMenu.addSeparatorEntry("separator_1", false);

				universalLayoutMenu.addClickableEntry("add_whitelist", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist"), null, Boolean.class, (entry, inherited, pass) -> {
					Minecraft.getInstance().setScreen(TextInputScreen.build(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, (call) -> {
						if (call != null) {
							if (!this.editor.layout.universalLayoutMenuWhitelist.contains(call)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
								this.editor.layout.universalLayoutMenuWhitelist.add(call);
							}
						}
						Minecraft.getInstance().setScreen(this.editor);
					}));
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist.desc")));

				universalLayoutMenu.addClickableEntry("remove_whitelist", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist"), null, Boolean.class, (entry, inherited, pass) -> {
					ChooseFromStringListScreen s = new ChooseFromStringListScreen(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), this.editor, this.editor.layout.universalLayoutMenuWhitelist, (call) -> {
						if (call != null) {
							Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
								if (call2) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
									this.editor.layout.universalLayoutMenuWhitelist.remove(call);
								}
								Minecraft.getInstance().setScreen(this.editor);
							}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist.confirm")));
						}
					});
					Minecraft.getInstance().setScreen(s);
				});

				universalLayoutMenu.addClickableEntry("clear_whitelist", false, Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist"), null, Boolean.class, (entry, inherited, pass) -> {
					Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
						if (call2) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							this.editor.layout.universalLayoutMenuWhitelist.clear();
						}
						Minecraft.getInstance().setScreen(this.editor);
					}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist.confirm")));
				});

			}

			menu.addSeparatorEntry("separator_1", false);

			// SET BACKGROUND
			menu.addClickableEntry("set_background", false, Component.translatable("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground"), null, Boolean.class, (entry, inherited, pass) -> {
				ChooseMenuBackgroundScreen s = new ChooseMenuBackgroundScreen(this.editor.layout.menuBackground, true, (call) -> {
					if (call != null) {
						this.editor.history.saveSnapshot();
						this.editor.layout.menuBackground = (call != ChooseMenuBackgroundScreen.NO_BACKGROUND) ? call : null;
					}
					Minecraft.getInstance().setScreen(this.editor);
				});
				Minecraft.getInstance().setScreen(s);
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground.btn.desc")));

			// KEEP BACKGROUND ASPECT RATIO
			menu.addClickableEntry("keep_background_aspect_ratio", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.editor.layout.keepBackgroundAspectRatio = !this.editor.layout.keepBackgroundAspectRatio;
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					if (this.editor.layout.keepBackgroundAspectRatio) {
						e.setLabel(Component.translatable("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.on"));
					} else {
						e.setLabel(Component.translatable("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect.off"));
					}
				}
			});

			menu.addSeparatorEntry("separator_2", false);

			if ((this.editor.layoutTargetScreen != null) && (this.editor.layoutTargetScreen.getTitle() != null) && !this.editor.layout.isUniversalLayout()) {

				// EDIT MENU TITLE
				String defaultMenuTitleRaw = this.editor.layoutTargetScreen.getTitle().getString();
				menu.addClickableEntry("edit_menu_title", false, Component.translatable("fancymenu.helper.editor.edit_menu_title"), null, Boolean.class, (entry, inherited, pass) -> {
					TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.edit_menu_title"), this.editor, null, (call) -> {
						if (call != null) {
							if (!call.equals(defaultMenuTitleRaw)) {
								if ((this.editor.layout.customMenuTitle == null) || !this.editor.layout.customMenuTitle.equals(call)) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
								}
								this.editor.layout.customMenuTitle = call;
							} else {
								if (this.editor.layout.customMenuTitle != null) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
								}
								this.editor.layout.customMenuTitle = null;
							}
						}
					});
					s.multilineMode = false;
					if (this.editor.layout.customMenuTitle != null) {
						s.setText(this.editor.layout.customMenuTitle);
					} else {
						s.setText(defaultMenuTitleRaw);
					}
					Minecraft.getInstance().setScreen(s);
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.desc")));

				// RESET MENU TITLE
				menu.addClickableEntry("reset_menu_title", false, Component.translatable("fancymenu.helper.editor.edit_menu_title.reset"), null, Boolean.class, (entry, inherited, pass) -> {
					if (this.editor.layout.customMenuTitle != null) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}
					this.editor.layout.customMenuTitle = null;
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.reset.desc")));

			}

			menu.addSeparatorEntry("separator_3", false);

			// RANDOM MODE
			menu.addClickableEntry("random_mode", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.editor.layout.randomMode = !this.editor.layout.randomMode;
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					if (this.editor.layout.randomMode) {
						e.setLabel(Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.on"));
					} else {
						e.setLabel(Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.off"));
					}
				}
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.btn.desc")));

			menu.addClickableEntry("random_mode_group", false, Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), null, Boolean.class, (entry, inherited, pass) -> {
				FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
					if (call != null) {
						if (!MathUtils.isInteger(call)) {
							call = "1";
						}
						if (!call.equalsIgnoreCase(this.editor.layout.randomGroup)) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						this.editor.layout.randomGroup = call;
					}
				});
				if (this.editor.layout.randomGroup != null) {
					pop.setText(this.editor.layout.randomGroup);
				}
				PopupHandler.displayPopup(pop);
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					e.getButton().active = this.editor.layout.randomMode;
				}
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup.btn.desc")));

			menu.addClickableEntry("random_mode_first_time", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.editor.layout.randomOnlyFirstTime = !this.editor.layout.randomOnlyFirstTime;
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					if (this.editor.layout.randomOnlyFirstTime) {
						e.setLabel(Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.on"));
					} else {
						e.setLabel(Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.off"));
					}
				}
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					e.getButton().active = this.editor.layout.randomMode;
				}
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.btn.desc")));

			menu.addSeparatorEntry("separator_4", false);

			// RENDER CUSTOM BEHIND VANILLA
			menu.addClickableEntry("render_custom_behind_vanilla", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.editor.layout.renderElementsBehindVanilla = !this.editor.layout.renderElementsBehindVanilla;
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					if (this.editor.layout.renderElementsBehindVanilla) {
						e.setLabel(Component.translatable("fancymenu.editor.render_custom_behind_vanilla.on"));
					} else {
						e.setLabel(Component.translatable("fancymenu.editor.render_custom_behind_vanilla.off"));
					}
				}
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.render_custom_behind_vanilla.desc")));

			menu.addSeparatorEntry("separator_5", false);

			// AUTO-SCALING
			menu.addClickableEntry("auto_scaling", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				if ((this.editor.layout.autoScalingWidth != 0) && (this.editor.layout.autoScalingHeight != 0)) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					this.editor.layout.autoScalingWidth = 0;
					this.editor.layout.autoScalingHeight = 0;
					this.editor.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
				} else {
					PopupHandler.displayPopup(new AutoScalingPopup(this.editor, (call) -> {
						if (call) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							this.editor.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
						}
					}));
				}
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					if ((this.editor.layout.autoScalingWidth != 0) && (this.editor.layout.autoScalingHeight != 0)) {
						e.setLabel(Component.translatable("fancymenu.helper.editor.properties.autoscale.on"));
					} else {
						e.setLabel(Component.translatable("fancymenu.helper.editor.properties.autoscale.off"));
					}
					if (this.editor.layout.forcedScale != 0) {
						e.getButton().active = true;
						e.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.autoscale.btn.desc")));
					} else {
						e.getButton().active = false;
						e.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.autoscale.forced_scale_needed")));
						e.setLabel(Component.translatable("fancymenu.helper.editor.properties.autoscale.off"));
					}
				}
			});

			// FORCE GUI SCALE
			menu.addClickableEntry("forced_scale", false, Component.translatable("fancymenu.editor.rightclick.scale"), null, Boolean.class, (entry, inherited, pass) -> {
				FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.editor.rightclick.scale"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
					if (call != null) {
						int s = 0;
						if (MathUtils.isInteger(call)) {
							s = Integer.parseInt(call);
						}
						if (s < 0) {
							PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {}, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.rightclick.scale.invalid")));
						} else {
							if (this.editor.layout.forcedScale != s) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.editor.layout.forcedScale = s;
							this.editor.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
						}
					}
				});
				p.setText("" + this.editor.layout.forcedScale);
				PopupHandler.displayPopup(p);
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.scale.btn.desc")));

			menu.addSeparatorEntry("separator_6", false);

			// OPEN-AUDIO
			menu.addClickableEntry("open_audio", false, Component.translatable("fancymenu.editor.open_audio"), null, Boolean.class, (entry, inherited, pass) -> {
				ChooseFilePopup p = new ChooseFilePopup((call) -> {
					if (call != null) {
						if (call.replace(" ", "").length() == 0) {
							if (this.editor.layout.openAudio != null) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.editor.layout.openAudio = null;
						} else {
							File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(call));
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								if (!this.editor.layout.openAudio.equals(call)) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
								}
								this.editor.layout.openAudio = call;
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {}, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.error.invalid_audio")));
							}
						}
					}
				}, "wav");
				if (this.editor.layout.openAudio != null) {
					p.setText(this.editor.layout.openAudio);
				}
				PopupHandler.displayPopup(p);
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.open_audio.desc")));

			// CLOSE-AUDIO
			menu.addClickableEntry("close_audio", false, Component.translatable("fancymenu.editor.close_audio"), null, Boolean.class, (entry, inherited, pass) -> {
				ChooseFilePopup p = new ChooseFilePopup((call) -> {
					if (call != null) {
						if (call.replace(" ", "").length() == 0) {
							if (this.editor.layout.closeAudio != null) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.editor.layout.closeAudio = null;
						} else {
							File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(call));
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								if (!this.editor.layout.closeAudio.equals(call)) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
								}
								this.editor.layout.closeAudio = call;
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {}, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.error.invalid_audio")));
							}
						}
					}
				}, "wav");
				if (this.editor.layout.closeAudio != null) {
					p.setText(this.editor.layout.closeAudio);
				}
				PopupHandler.displayPopup(p);
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.close_audio.desc")));

			menu.addSeparatorEntry("separator_7", false);

			// LOADING REQUIREMENTS [LAYOUT-WIDE]
			menu.addClickableEntry("layout_wide_requirements", false, Component.translatable("fancymenu.editor.loading_requirement.layouts.loading_requirements"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.editor.layout.layoutWideLoadingRequirementContainer, (call) -> {});
				Minecraft.getInstance().setScreen(s);
			}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.layouts.loading_requirements.desc")));

			menu.addSeparatorEntry("separator_8", false);

			// PASTE
			menu.addClickableEntry("paste_elements", false, Component.translatable("fancymenu.editor.edit.paste"), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.editor.pasteElementsFromClipboard();
			}).setTicker((entry) -> {
				if (entry instanceof AdvancedContextMenu.ClickableMenuEntry<?> e) {
					e.getButton().active = LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.size() > 0;
				}
			});

			// NEW ELEMENT
			AdvancedContextMenu newElementMenu = this.buildNewElementContextMenu();
			newElementMenu.getContextMenu().setAutoclose(true);
			menu.addClickableEntry("new_element", false, Component.translatable("fancymenu.editor.layoutproperties.newelement"), newElementMenu, Boolean.class, (entry, inherited, pass) -> {
				newElementMenu.getContextMenu().setParentButton(entry.getButton());
				newElementMenu.openMenu(0, entry.getButton().y);
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return menu;

	}

	@NotNull
	public AdvancedContextMenu buildNewElementContextMenu() {

		AdvancedContextMenu menu = new AdvancedContextMenu();

		try {

			// DUMMY BUTTON: INSTALL AUDIO EXTENSION
			if (!FancyMenu.isAudioExtensionLoaded()) {
				menu.addClickableEntry("install_audio_extension", false, Component.translatable("fancymenu.editor.add.audio"), null, Boolean.class, (entry, inherited, pass) -> {
					ActionExecutor.openWebLink("https://www.curseforge.com/minecraft/mc-mods/audio-extension-for-fancymenu-" + FancyMenu.MOD_LOADER);
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.extension.dummy.audio.btn.desc")));
			}

			// ADD ALL ELEMENT TYPES
			for (ElementBuilder<?,?> builder : ElementRegistry.getBuilders()) {
				Component[] desc = builder.getDescription(null);
				menu.addClickableEntry("element_" + builder.getIdentifier(), false, builder.getDisplayName(null), null, Boolean.class, (entry, inherited, pass) -> {
					this.editor.history.saveSnapshot();
					this.editor.normalEditorElements.add(builder.wrapIntoEditorElementInternal(builder.buildDefaultInstance(), this.editor));
					//TODO remove debug
					LogManager.getLogger().info("ADDING NEW ELEMENT: " + builder.getIdentifier());
				}).setTooltip((desc != null) ? Tooltip.of(desc) : null);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return menu;

	}

	@NotNull
	public AdvancedContextMenu buildHiddenVanillaElementContextMenu() {

		AdvancedContextMenu menu = new AdvancedContextMenu();

		try {

			List<VanillaButtonEditorElement> hiddenVanillaButtons = new ArrayList<>();
			for (VanillaButtonEditorElement e : this.editor.vanillaButtonEditorElements) {
				if (e.isHidden()) {
					hiddenVanillaButtons.add(e);
				}
			}
			List<AbstractDeepEditorElement> hiddenDeepElements = new ArrayList<>();
			for (AbstractDeepEditorElement e : this.editor.deepEditorElements) {
				if (e.isHidden()) {
					hiddenDeepElements.add(e);
				}
			}

			int count = 0;
			for (VanillaButtonEditorElement e : hiddenVanillaButtons) {
				menu.addClickableEntry("element_" + count, false, ((VanillaButtonElement)e.element).button.getMessage(), null, Boolean.class, (entry, inherited, pass) -> {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					e.setHidden(false);
					menu.closeMenu();
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.hidden_vanilla_elements.element.desc")));
				count++;
			}
			for (AbstractDeepEditorElement e : hiddenDeepElements) {
				menu.addClickableEntry("element_" + count, false, e.element.builder.getDisplayName(e.element), null, Boolean.class, (entry, inherited, pass) -> {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					e.setHidden(false);
					menu.closeMenu();
				}).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.hidden_vanilla_elements.element.desc")));
				count++;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return menu;

	}

}
