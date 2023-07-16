package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.background.ChooseMenuBackgroundScreen;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.ManageLayoutsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlayUI;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedValueCycle;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.NonStackableOverlayUI;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ChooseFromStringListScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resources.texture.WrappedTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class LayoutEditorUI {

	private static final WrappedTexture CLOSE_EDITOR_TEXTURE = WrappedTexture.of(new ResourceLocation("fancymenu", "textures/menubar/icons/close.png"));

	private static MenuBar grandfatheredMenuBar = null;

	@NotNull
	public static MenuBar buildMenuBar(LayoutEditorScreen editor) {

		if (grandfatheredMenuBar != null) {
			MenuBar mb = grandfatheredMenuBar;
			grandfatheredMenuBar = null;
			return  mb;
		}

		MenuBar menuBar = new MenuBar();

		//FANCYMENU ICON
		CustomizationOverlayUI.buildFMIconTabAndAddTo(menuBar);

		//LAYOUT
		ContextMenu layoutMenu = new ContextMenu();
		menuBar.addContextMenuEntry("layout_tab", Component.translatable("fancymenu.editor.layout"), layoutMenu);

		layoutMenu.addClickableEntry("new_layout", Component.translatable("fancymenu.editor.layout.new"), (menu, entry) -> {
			displayUnsavedWarning(call -> {
				if (call) {
					editor.saveWidgetSettings();
					if (editor.layout.isUniversalLayout()) {
						LayoutHandler.openLayoutEditor(Layout.buildUniversal(), null);
					} else {
						LayoutHandler.openLayoutEditor(Layout.buildForScreen(Objects.requireNonNull(editor.layoutTargetScreen)), editor.layoutTargetScreen);
					}
				} else {
					Minecraft.getInstance().setScreen(editor);
				}
			});
		});

		layoutMenu.addSubMenuEntry("open_layout", Component.translatable("fancymenu.editor.layout.open"), buildOpenLayoutContextMenu(editor));

		layoutMenu.addSeparatorEntry("separator_after_open_layout");

		layoutMenu.addClickableEntry("save_layout", Component.translatable("fancymenu.editor.layout.save"), (menu, entry) -> {
			menu.closeMenu();
			editor.saveLayout();
		}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.save"));

		layoutMenu.addClickableEntry("save_layout_as", Component.translatable("fancymenu.editor.layout.saveas"), (menu, entry) -> {
			menu.closeMenu();
			editor.saveLayoutAs();
		});

		layoutMenu.addSeparatorEntry("separator_after_save_as");

		layoutMenu.addSubMenuEntry("layout_settings", Component.translatable("fancymenu.editor.layout.properties"), buildRightClickContextMenu(editor));

		//EDIT
		ContextMenu editMenu = new ContextMenu();
		menuBar.addContextMenuEntry("edit_tab", Component.translatable("fancymenu.editor.edit"), editMenu);

		editMenu.addClickableEntry("undo_action", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
			editor.history.stepBack();
		}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"));

		editMenu.addClickableEntry("redo_action", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
			editor.history.stepForward();
		}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"));

		editMenu.addSeparatorEntry("separator_after_redo");

		editMenu.addClickableEntry("copy_selected_elements", Component.translatable("fancymenu.editor.edit.copy"), (menu, entry) -> {
			editor.copyElementsToClipboard(editor.getSelectedElements().toArray(new AbstractEditorElement[0]));
		}).setIsActiveSupplier((menu, entry) -> !editor.getSelectedElements().isEmpty())
				.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"));

		editMenu.addClickableEntry("paste_copied_elements", Component.translatable("fancymenu.editor.edit.paste"), (menu, entry) -> {
			editor.history.saveSnapshot();
			editor.pasteElementsFromClipboard();
		}).setIsActiveSupplier((menu, entry) -> !LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.isEmpty())
				.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.paste"));

		editMenu.addSeparatorEntry("separator_after_paste_copied");

		editMenu.addClickableEntry("select_all_elements", Component.translatable("fancymenu.editor.menu_bar.edit.select_all"), (menu, entry) -> {
			editor.selectAllElements();
		}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.select_all"));

		//ELEMENT
		ContextMenu elementMenu = new ContextMenu();
		menuBar.addContextMenuEntry("element_tab", Component.translatable("fancymenu.editor.element"), elementMenu);

		elementMenu.addSubMenuEntry("new_element", Component.translatable("fancymenu.editor.element.new"), buildElementContextMenu(editor));

		elementMenu.addSubMenuEntry("manage_hidden_vanilla_elements", Component.translatable("fancymenu.fancymenu.editor.element.deleted_vanilla_elements"), buildHiddenVanillaElementsContextMenu(editor));

		//WINDOW
		ContextMenu windowMenu = new ContextMenu();
		menuBar.addContextMenuEntry("window_tab", Component.translatable("fancymenu.editor.menu_bar.window"), windowMenu);

		windowMenu.addValueCycleEntry("enable_grid", CommonCycles.cycleEnabledDisabled("fancymenu.editor.menu_bar.window.grid", FancyMenu.getOptions().showLayoutEditorGrid.getValue()).addCycleListener(cycle -> {
			FancyMenu.getOptions().showLayoutEditorGrid.setValue(cycle.getAsBoolean());
		})).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.grid"));

		int preSelectedGridSize = FancyMenu.getOptions().layoutEditorGridSize.getValue();
		if ((preSelectedGridSize != 10) && (preSelectedGridSize != 20) && (preSelectedGridSize != 30) && (preSelectedGridSize != 40)) {
			preSelectedGridSize = 10;
		}
		windowMenu.addValueCycleEntry("grid_size", CommonCycles.cycle("fancymenu.editor.menu_bar.window.grid_size", ListUtils.build(10,20,30,40), preSelectedGridSize)
				.addCycleListener(integer -> {
					FancyMenu.getOptions().layoutEditorGridSize.setValue(integer);
				}));

		windowMenu.addSeparatorEntry("separator_after_grid_size");

		windowMenu.addValueCycleEntry("show_anchor_overlay", CommonCycles.cycleEnabledDisabled("fancymenu.editor.menu_bar.window.show_anchor_overlay", FancyMenu.getOptions().showAnchorOverlay.getValue()).addCycleListener(cycle -> {
			FancyMenu.getOptions().showAnchorOverlay.setValue(cycle.getAsBoolean());
		}));

		windowMenu.addValueCycleEntry("always_show_anchor_overlay", CommonCycles.cycleEnabledDisabled("fancymenu.editor.menu_bar.window.always_show_anchor_overlay", FancyMenu.getOptions().alwaysShowAnchorOverlay.getValue()).addCycleListener(cycle -> {
			FancyMenu.getOptions().alwaysShowAnchorOverlay.setValue(cycle.getAsBoolean());
		}));

		windowMenu.addValueCycleEntry("show_all_anchor_connections", CommonCycles.cycleEnabledDisabled("fancymenu.editor.menu_bar.window.show_all_anchor_connections", FancyMenu.getOptions().showAllAnchorConnections.getValue()).addCycleListener(cycle -> {
			FancyMenu.getOptions().showAllAnchorConnections.setValue(cycle.getAsBoolean());
		}));

		windowMenu.addValueCycleEntry("change_anchor_on_hover", CommonCycles.cycleEnabledDisabled("fancymenu.editor.menu_bar.window.change_anchor_on_hover", FancyMenu.getOptions().changeAnchorOnHover.getValue()).addCycleListener(cycle -> {
			FancyMenu.getOptions().changeAnchorOnHover.setValue(cycle.getAsBoolean());
		}));

		//USER INTERFACE
		CustomizationOverlayUI.buildUITabAndAddTo(menuBar);

		//HELP
		CustomizationOverlayUI.buildHelpTabAndAddTo(menuBar);

		//CLOSE EDITOR BUTTON
		menuBar.addClickableEntry(MenuBar.Side.RIGHT, "close_editor", Component.empty(), (bar, entry) -> {
			displayUnsavedWarning(call -> {
				if (call) {
					editor.saveWidgetSettings();
					Minecraft.getInstance().setScreen(editor.layoutTargetScreen);
				} else {
					Minecraft.getInstance().setScreen(editor);
				}
			});
		}).setIconTexture(CLOSE_EDITOR_TEXTURE)
				.setIconTextureColor(() -> UIBase.getUIColorScheme().layout_editor_close_icon_color);

		return menuBar;

	}

	protected static void displayUnsavedWarning(@NotNull Consumer<Boolean> callback) {
		Minecraft.getInstance().setScreen(ConfirmationScreen.warning(callback, LocalizationUtils.splitLocalizedLines("fancymenu.editor.warning.unsaved")));
	}

	@NotNull
	public static ContextMenu buildRightClickContextMenu(@NotNull LayoutEditorScreen editor) {

		ContextMenu menu = new ContextMenu();

		if (editor.layout.isUniversalLayout()) {

			ContextMenu universalLayoutMenu = new ContextMenu();
			menu.addSubMenuEntry("universal_layout_settings", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options"), universalLayoutMenu);

			universalLayoutMenu.addClickableEntry("add_blacklist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist"), (menu1, entry) -> {
				TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, call -> {
					if (call != null) {
						editor.history.saveSnapshot();
						if (!editor.layout.universalLayoutMenuBlacklist.contains(call)) {
							editor.layout.universalLayoutMenuBlacklist.add(call);
						}
					}
					Minecraft.getInstance().setScreen(editor);
				});
				Minecraft.getInstance().setScreen(s);
			}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist.desc")));

			universalLayoutMenu.addClickableEntry("remove_blacklist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist"), (menu1, entry) -> {
				ChooseFromStringListScreen s = new ChooseFromStringListScreen(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), editor, editor.layout.universalLayoutMenuBlacklist, (call) -> {
					if (call != null) {
						Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
							if (call2) {
								editor.history.saveSnapshot();
								editor.layout.universalLayoutMenuBlacklist.remove(call);
							}
							Minecraft.getInstance().setScreen(editor);
						}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist.confirm")));
					}
				});
				Minecraft.getInstance().setScreen(s);
			});

			universalLayoutMenu.addClickableEntry("clear_blacklist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist"), (menu1, entry) -> {
				Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
					if (call2) {
						editor.history.saveSnapshot();
						editor.layout.universalLayoutMenuBlacklist.clear();
					}
					Minecraft.getInstance().setScreen(editor);
				}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist.confirm")));
			});

			universalLayoutMenu.addSeparatorEntry("separator_after_clear_blacklist");

			universalLayoutMenu.addClickableEntry("add_whitelist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist"), (menu1, entry) -> {
				TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"), null, call -> {
					if (call != null) {
						editor.history.saveSnapshot();
						if (!editor.layout.universalLayoutMenuWhitelist.contains(call)) {
							editor.layout.universalLayoutMenuWhitelist.add(call);
						}
					}
					Minecraft.getInstance().setScreen(editor);
				});
				Minecraft.getInstance().setScreen(s);
			}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist.desc")));

			universalLayoutMenu.addClickableEntry("remove_whitelist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist"), (menu1, entry) -> {
				ChooseFromStringListScreen s = new ChooseFromStringListScreen(I18n.get("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), editor, editor.layout.universalLayoutMenuWhitelist, (call) -> {
					if (call != null) {
						Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
							if (call2) {
								editor.history.saveSnapshot();
								editor.layout.universalLayoutMenuWhitelist.remove(call);
							}
							Minecraft.getInstance().setScreen(editor);
						}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist.confirm")));
					}
				});
				Minecraft.getInstance().setScreen(s);
			});

			universalLayoutMenu.addClickableEntry("clear_whitelist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist"), (menu1, entry) -> {
				Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
					if (call2) {
						editor.history.saveSnapshot();
						editor.layout.universalLayoutMenuWhitelist.clear();
					}
					Minecraft.getInstance().setScreen(editor);
				}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist.confirm")));
			});

		}

		menu.addSeparatorEntry("separator_after_universal_layout_menu");

		menu.addClickableEntry("menu_background_settings", Component.translatable("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground"), (menu1, entry) -> {
			ChooseMenuBackgroundScreen s = new ChooseMenuBackgroundScreen(editor.layout.menuBackground, true, (call) -> {
				if (call != null) {
					editor.history.saveSnapshot();
					editor.layout.menuBackground = (call != ChooseMenuBackgroundScreen.NO_BACKGROUND) ? call : null;
				}
				Minecraft.getInstance().setScreen(editor);
			});
			Minecraft.getInstance().setScreen(s);
		}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground.btn.desc")));

		menu.addValueCycleEntry("keep_background_aspect_ratio", CommonCycles.cycleEnabledDisabled("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect", editor.layout.keepBackgroundAspectRatio).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.keepBackgroundAspectRatio = cycle.getAsBoolean();
		}));

		menu.addSeparatorEntry("separator_after_keep_background_aspect");

		if ((editor.layoutTargetScreen != null) && !editor.layout.isUniversalLayout()) {

			NonStackableOverlayUI.addInputContextMenuEntryTo(menu, "edit_menu_title", Component.translatable("fancymenu.helper.editor.edit_menu_title"),
							() -> editor.layout.customMenuTitle,
							s -> {
								editor.history.saveSnapshot();
								editor.layout.customMenuTitle = s;
							},
							true, null, null, false, true,
							consumes -> !consumes.isEmpty(),
							consumes -> consumes.isEmpty() ? Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.reset.invalid_title")) : null)
					.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.desc")));

			menu.addSeparatorEntry("separator_after_edit_menu_title");

		}

		menu.addValueCycleEntry("random_mode", CommonCycles.cycleEnabledDisabled("fancymenu.fancymenu.editor.layoutoptions.randommode", editor.layout.randomMode).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.randomMode = cycle.getAsBoolean();
		})).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.btn.desc")));

		menu.addClickableEntry("random_mode_group", Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), (menu1, entry) -> {
			Minecraft.getInstance().setScreen(TextInputScreen.build(Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), CharacterFilter.buildIntegerCharacterFiler(), call -> {
				if (call != null) {
					if (!MathUtils.isInteger(call)) {
						call = "1";
					}
					editor.history.saveSnapshot();
					editor.layout.randomGroup = call;
				}
				Minecraft.getInstance().setScreen(editor);
			}).setText(editor.layout.randomGroup));
		}).setIsActiveSupplier((menu1, entry) -> editor.layout.randomMode)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup.btn.desc")));

		menu.addValueCycleEntry("random_mode_first_time", CommonCycles.cycleEnabledDisabled("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime", editor.layout.randomOnlyFirstTime).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.randomOnlyFirstTime = cycle.getAsBoolean();
		})).setIsActiveSupplier((menu1, entry) -> editor.layout.randomMode)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.btn.desc")));

		menu.addSeparatorEntry("separator_after_random_mode_first_time");

		menu.addValueCycleEntry("render_custom_elements_behind_vanilla", CommonCycles.cycleEnabledDisabled("fancymenu.editor.render_custom_behind_vanilla", editor.layout.renderElementsBehindVanilla).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.renderElementsBehindVanilla = cycle.getAsBoolean();
		})).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.render_custom_behind_vanilla.desc")));

		menu.addSeparatorEntry("separator_after_render_custom_behind_vanilla");

		LocalizedValueCycle<CommonCycles.CycleEnabledDisabled> cycleAutoScaling = CommonCycles.cycleEnabledDisabled("fancymenu.helper.editor.properties.autoscale", ((editor.layout.autoScalingWidth != 0) && (editor.layout.autoScalingHeight != 0)));
		cycleAutoScaling.addCycleListener(cycle -> {
			if (cycle.getAsBoolean()) {
				menu.closeMenu();
				//TODO durch screen ersetzen
				PopupHandler.displayPopup(new AutoScalingPopup(editor, (call) -> {
					if (call) {
						editor.history.saveSnapshot();
						editor.init();
					} else {
						cycleAutoScaling.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED, false);
					}
				}));
			} else {
				editor.history.saveSnapshot();
				editor.layout.autoScalingWidth = 0;
				editor.layout.autoScalingHeight = 0;
				editor.init();
			}
		});
		menu.addValueCycleEntry("auto_scaling", cycleAutoScaling)
				.setIsActiveSupplier((menu1, entry) -> (editor.layout.forcedScale != 0))
				.setTooltipSupplier((menu1, entry) -> {
					if (entry.isActive()) {
						return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.autoscale.btn.desc"));
					}
					return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.autoscale.forced_scale_needed"));
				});

		NonStackableOverlayUI.addIntegerInputContextMenuEntryTo(menu, "forced_gui_scale", Component.translatable("fancymenu.editor.rightclick.scale"),
				() -> (int) editor.layout.forcedScale,
				integer -> {
					editor.history.saveSnapshot();
					editor.layout.forcedScale = integer;
					editor.init();
				},
				true, 0, consumes -> {
					if (MathUtils.isInteger(consumes)) {
						return Integer.parseInt(consumes) >= 0;
					}
					return false;
				}, consumes -> {
					if (MathUtils.isInteger(consumes)) {
						if (Integer.parseInt(consumes) < 0) {
							return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.rightclick.scale.invalid"));
						}
					}
					return null;
				}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.scale.btn.desc")));

		menu.addSeparatorEntry("separator_after_forced_scale");

		NonStackableOverlayUI.addFileChooserContextMenuEntryTo(menu, "open_audio", Component.translatable("fancymenu.editor.open_audio"),
						() -> editor.layout.openAudio,
						s -> {
							editor.history.saveSnapshot();
							editor.layout.openAudio = s;
						}, true, null, FileFilter.WAV_AUDIO_FILE_FILTER)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.open_audio.desc")));

		NonStackableOverlayUI.addFileChooserContextMenuEntryTo(menu, "close_audio", Component.translatable("fancymenu.editor.close_audio"),
						() -> editor.layout.closeAudio,
						s -> {
							editor.history.saveSnapshot();
							editor.layout.closeAudio = s;
						}, true, null, FileFilter.WAV_AUDIO_FILE_FILTER)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.close_audio.desc")));

		menu.addSeparatorEntry("separator_after_close_audio");

		menu.addClickableEntry("layout_wide_requirements", Component.translatable("fancymenu.editor.loading_requirement.layouts.loading_requirements"), (menu1, entry) -> {
			Minecraft.getInstance().setScreen(new ManageRequirementsScreen(editor.layout.layoutWideLoadingRequirementContainer.copy(true), (call) -> {
				if (call != null) {
					editor.layout.layoutWideLoadingRequirementContainer = call;
				}
				Minecraft.getInstance().setScreen(editor);
			}));
		}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.layouts.loading_requirements.desc")));

		menu.addSeparatorEntry("separator_after_layout_wide_requirements");

		menu.addClickableEntry("paste_elements", Component.translatable("fancymenu.editor.edit.paste"), (menu1, entry) -> {
			editor.history.saveSnapshot();
			editor.pasteElementsFromClipboard();
		}).setIsActiveSupplier((menu1, entry) -> !LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.isEmpty());

		menu.addSeparatorEntry("separator_after_paste_elements");

		menu.addSubMenuEntry("add_element", Component.translatable("fancymenu.editor.layoutproperties.newelement"), buildElementContextMenu(editor));

		return menu;

	}

	@NotNull
	public static ContextMenu buildElementContextMenu(@NotNull LayoutEditorScreen editor) {

		ContextMenu menu = new ContextMenu();

		if (!FancyMenu.isAudioExtensionLoaded()) {
			menu.addClickableEntry("dummy_audio_entry", Component.translatable("fancymenu.editor.add.audio"), (menu1, entry) -> {
				WebUtils.openWebLink("https://www.curseforge.com/minecraft/mc-mods/audio-extension-for-fancymenu-" + FancyMenu.MOD_LOADER);
			}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.extension.dummy.audio.btn.desc")));
		}

		int i = 0;
		for (ElementBuilder<?,?> builder : ElementRegistry.getBuilders()) {
			ContextMenu.ClickableContextMenuEntry<?> entry = menu.addClickableEntry("element_" + i, builder.getDisplayName(null), (menu1, entry1) -> {
				AbstractEditorElement editorElement = builder.wrapIntoEditorElementInternal(builder.buildDefaultInstance(), editor);
				if (editorElement != null) {
					editor.history.saveSnapshot();
					editor.normalEditorElements.add(editorElement);
					if ((editor.rightClickMenuOpenPosX != -1000) && (editor.rightClickMenuOpenPosY != -1000)) {
						//Add new element at right-click menu coordinates
						editorElement.setAnchorPoint(editorElement.element.anchorPoint, true, editor.rightClickMenuOpenPosX, editor.rightClickMenuOpenPosY, true);
					}
					menu.closeMenu();
				}
			});
			Component[] desc = builder.getDescription(null);
			if ((desc != null) && (desc.length > 0)) {
				entry.setTooltipSupplier((menu1, entry1) -> Tooltip.of(desc));
			}
			i++;
		}

		return menu;

	}

	@NotNull
	public static ContextMenu buildHiddenVanillaElementsContextMenu(LayoutEditorScreen editor) {

		return new ContextMenu() {

			//This allows the menu to update itself on open, so it doesn't need to get rebuilt everytime the hidden elements change
			@Override
			public ContextMenu openMenuAt(float x, float y) {

				this.entries.clear();

				List<VanillaButtonEditorElement> hiddenVanillaButtons = new ArrayList<>();
				for (VanillaButtonEditorElement e : editor.vanillaButtonEditorElements) {
					if (e.isHidden()) {
						hiddenVanillaButtons.add(e);
					}
				}
				List<AbstractDeepEditorElement> hiddenDeepElements = new ArrayList<>();
				for (AbstractDeepEditorElement e : editor.deepEditorElements) {
					if (e.isHidden()) {
						hiddenDeepElements.add(e);
					}
				}

				int i = 0;
				for (VanillaButtonEditorElement e : hiddenVanillaButtons) {
					this.addClickableEntry("element_" + i, ((VanillaButtonElement)e.element).button.getMessage(), (menu1, entry) -> {
						editor.history.saveSnapshot();
						e.setHidden(false);
						MainThreadTaskExecutor.executeInMainThread(() -> menu1.removeEntry(entry.getIdentifier()), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
					}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.hidden_vanilla_elements.element.desc")));
					i++;
				}
				for (AbstractDeepEditorElement e : hiddenDeepElements) {
					this.addClickableEntry("element_" + i, e.element.builder.getDisplayName(e.element), (menu1, entry) -> {
						editor.history.saveSnapshot();
						e.setHidden(false);
						MainThreadTaskExecutor.executeInMainThread(() -> menu1.removeEntry(entry.getIdentifier()), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
					}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.hidden_vanilla_elements.element.desc")));
					i++;
				}

				return super.openMenuAt(x, y);

			}

		};

	}

	public static ContextMenu buildOpenLayoutContextMenu(LayoutEditorScreen editor) {

		ContextMenu menu = new ContextMenu();

		if (editor.layout.isUniversalLayout()) {

			int i = 0;
			for (Layout l : LayoutHandler.sortLayoutListByLastEdited(LayoutHandler.getAllLayoutsForMenuIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), true, 8)) {
				if (l.getLayoutName().equals(editor.layout.getLayoutName())) continue; //Don't show the current layout in the list
				menu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(editor, l))
						.setLabelSupplier((menu1, entry) -> {
							Style style = l.getStatus().getEntryComponentStyle();
							MutableComponent c = Component.literal(l.getLayoutName());
							c.append(Component.literal(" (").setStyle(style));
							c.append(l.getStatus().getEntryComponent());
							c.append(Component.literal(")").setStyle(style));
							return c;
						});
				i++;
			}

			menu.addSeparatorEntry("separator_after_recent_layouts");

			menu.addClickableEntry("all_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu1, entry) -> {
				displayUnsavedWarning(call -> {
					if (call) {
						editor.saveWidgetSettings();
						Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForMenuIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), editor.layoutTargetScreen, layouts -> {
							Minecraft.getInstance().setScreen(editor);
						}));
					} else {
						Minecraft.getInstance().setScreen(editor);
					}
				});
			});

		} else {

			int i = 0;
			for (Layout l : LayoutHandler.sortLayoutListByLastEdited(LayoutHandler.getAllLayoutsForMenuIdentifier(editor.layout.menuIdentifier, false), true, 8)) {
				if (l.getLayoutName().equals(editor.layout.getLayoutName())) continue; //Don't show the current layout in the list
				menu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(editor, l))
						.setLabelSupplier((menu1, entry) -> {
							Style style = l.getStatus().getEntryComponentStyle();
							MutableComponent c = Component.literal(l.getLayoutName());
							c.append(Component.literal(" (").setStyle(style));
							c.append(l.getStatus().getEntryComponent());
							c.append(Component.literal(")").setStyle(style));
							return c;
						});
				i++;
			}

			menu.addSeparatorEntry("separator_after_recent_layouts");

			menu.addClickableEntry("all_layouts", Component.translatable("fancymenu.overlay.menu_bar.layout.manage.all"), (menu1, entry) -> {
				displayUnsavedWarning(call -> {
					if (call) {
						editor.saveWidgetSettings();
						Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForMenuIdentifier(editor.layout.menuIdentifier, false), editor.layoutTargetScreen, layouts -> {
							Minecraft.getInstance().setScreen(editor);
						}));
					} else {
						Minecraft.getInstance().setScreen(editor);
					}
				});
			});

		}

		return menu;

	}

	@NotNull
	protected static ContextMenu buildManageLayoutSubMenu(LayoutEditorScreen editor, Layout layout) {

		ContextMenu menu = new ContextMenu();

		menu.addClickableEntry("toggle_layout_status", Component.empty(), (menu1, entry) -> {
			MainThreadTaskExecutor.executeInMainThread(() -> {
				grandfatheredMenuBar = CustomizationOverlay.getCurrentMenuBarInstance();
				layout.setEnabled(!layout.isEnabled(), true);
			}, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
		}).setLabelSupplier((menu1, entry) -> layout.getStatus().getCycleComponent());

		menu.addClickableEntry("edit_layout", Component.translatable("fancymenu.layout.manage.edit"), (menu1, entry) -> {
			displayUnsavedWarning(call -> {
				if (call) {
					editor.saveWidgetSettings();
					MainThreadTaskExecutor.executeInMainThread(() -> LayoutHandler.openLayoutEditor(layout, layout.isUniversalLayout() ? null : editor.layoutTargetScreen), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
				} else {
					Minecraft.getInstance().setScreen(editor);
				}
			});
		});

		menu.addClickableEntry("edit_in_system_text_editor", Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (menu1, entry) -> {
			if (layout.layoutFile != null) {
				menu1.closeMenu();
				FileUtils.openFile(layout.layoutFile);
			}
		});

		return menu;

	}

}
