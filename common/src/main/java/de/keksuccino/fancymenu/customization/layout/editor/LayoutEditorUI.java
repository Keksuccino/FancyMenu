package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.background.ChooseMenuBackgroundScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.ManageLayoutsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlayUI;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.NonStackableOverlayUI;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringListChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class LayoutEditorUI {

	private static final PngTexture CLOSE_EDITOR_TEXTURE = PngTexture.location(ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/menubar/icons/close.png"));

	private static MenuBar grandfatheredMenuBar = null;

	@NotNull
	public static MenuBar buildMenuBar(LayoutEditorScreen editor, boolean expanded) {

		if (grandfatheredMenuBar != null) {
			MenuBar mb = grandfatheredMenuBar;
			grandfatheredMenuBar = null;
			return  mb;
		}

		MenuBar menuBar = new MenuBar();
		menuBar.setExpanded(expanded);

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
		}).setIcon(ContextMenu.IconFactory.getIcon("add"));

		layoutMenu.addSubMenuEntry("open_layout", Component.translatable("fancymenu.editor.layout.open"), buildOpenLayoutContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("open"));

		layoutMenu.addSeparatorEntry("separator_after_open_layout");

		layoutMenu.addClickableEntry("save_layout", Component.translatable("fancymenu.editor.layout.save"), (menu, entry) -> {
					menu.closeMenu();
					editor.saveLayout();
				}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.save"))
				.setIcon(ContextMenu.IconFactory.getIcon("save"));

		layoutMenu.addClickableEntry("save_layout_as", Component.translatable("fancymenu.editor.layout.saveas"), (menu, entry) -> {
			menu.closeMenu();
			editor.saveLayoutAs();
		}).setIcon(ContextMenu.IconFactory.getIcon("save_as"));

		layoutMenu.addSeparatorEntry("separator_after_save_as");

		layoutMenu.addSubMenuEntry("layout_settings", Component.translatable("fancymenu.editor.layout.properties"), buildRightClickContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("settings"))
				.setHoverAction((menu, entry, isPost) -> {
					if (!isPost) {
						int menuBarHeight = (int)((float)menuBar.getHeight() * UIBase.calculateFixedScale(menuBar.getScale()));
						editor.rightClickMenuOpenPosX = 20;
						editor.rightClickMenuOpenPosY = menuBarHeight + 20;
					}
				});

		layoutMenu.addSeparatorEntry("separator_after_layout_settings");

		layoutMenu.addClickableEntry("close_editor", Component.translatable("fancymenu.editor.close"), (menu, entry) -> {
			displayUnsavedWarning(call -> {
				if (call) {
					editor.closeEditor();
				} else {
					Minecraft.getInstance().setScreen(editor);
				}
			});
		}).setIcon(ContextMenu.IconFactory.getIcon("close"));

		//EDIT
		ContextMenu editMenu = new ContextMenu();
		menuBar.addContextMenuEntry("edit_tab", Component.translatable("fancymenu.editor.edit"), editMenu);

		editMenu.addClickableEntry("undo_action", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
					editor.history.stepBack();
				}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"))
				.setIcon(ContextMenu.IconFactory.getIcon("undo"));

		editMenu.addClickableEntry("redo_action", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
					editor.history.stepForward();
				}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"))
				.setIcon(ContextMenu.IconFactory.getIcon("redo"));

		editMenu.addSeparatorEntry("separator_after_redo");

		editMenu.addClickableEntry("copy_selected_elements", Component.translatable("fancymenu.editor.edit.copy"), (menu, entry) -> {
					editor.copyElementsToClipboard(editor.getSelectedElements().toArray(new AbstractEditorElement[0]));
				}).setIsActiveSupplier((menu, entry) -> !editor.getSelectedElements().isEmpty())
				.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"))
				.setIcon(ContextMenu.IconFactory.getIcon("copy"));

		editMenu.addClickableEntry("paste_copied_elements", Component.translatable("fancymenu.editor.edit.paste"), (menu, entry) -> {
					editor.history.saveSnapshot();
					editor.pasteElementsFromClipboard();
				}).setIsActiveSupplier((menu, entry) -> !LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.isEmpty())
				.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.paste"))
				.setIcon(ContextMenu.IconFactory.getIcon("paste"));

		editMenu.addSeparatorEntry("separator_after_paste_copied");

		editMenu.addClickableEntry("select_all_elements", Component.translatable("fancymenu.editor.menu_bar.edit.select_all"), (menu, entry) -> {
					editor.selectAllElements();
				}).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.select_all"))
				.setIcon(ContextMenu.IconFactory.getIcon("select"));

		//ELEMENT
		ContextMenu elementMenu = new ContextMenu();
		menuBar.addContextMenuEntry("element_tab", Component.translatable("fancymenu.editor.element"), elementMenu);

		elementMenu.addSubMenuEntry("new_element", Component.translatable("fancymenu.editor.element.new"), buildElementContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("add"));

		elementMenu.addSubMenuEntry("manage_hidden_vanilla_elements", Component.translatable("fancymenu.fancymenu.editor.element.deleted_vanilla_elements"), buildHiddenVanillaElementsContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("delete"));

		//WINDOW
		ContextMenu windowMenu = new ContextMenu();
		menuBar.addContextMenuEntry("window_tab", Component.translatable("fancymenu.editor.menu_bar.window"), windowMenu);

		windowMenu.addSubMenuEntry("editor_widgets", Component.translatable("fancymenu.editor.widgets"), buildEditorWidgetsContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("widget"));

		windowMenu.addSeparatorEntry("separator_after_editor_widgets");

		LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> gridToggleCycle = CommonCycles.cycleEnabledDisabled("fancymenu.editor.menu_bar.window.grid", FancyMenu.getOptions().showLayoutEditorGrid.getValue());
		gridToggleCycle.addCycleListener(cycle -> FancyMenu.getOptions().showLayoutEditorGrid.setValue(cycle.getAsBoolean()));
		windowMenu.addValueCycleEntry("enable_grid", gridToggleCycle)
				.setTickAction((menu, entry, isPost) -> {
					//This is to correct the displayed value in case the grid got toggled via shortcut
					if (FancyMenu.getOptions().showLayoutEditorGrid.getValue() != gridToggleCycle.current().getAsBoolean()) {
						gridToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.getByBoolean(FancyMenu.getOptions().showLayoutEditorGrid.getValue()), false);
					}
				})
				.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.grid"))
				.setIcon(ContextMenu.IconFactory.getIcon("grid"));

		int preSelectedGridSize = FancyMenu.getOptions().layoutEditorGridSize.getValue();
		if ((preSelectedGridSize != 10) && (preSelectedGridSize != 20) && (preSelectedGridSize != 30) && (preSelectedGridSize != 40)) {
			preSelectedGridSize = 10;
		}
		windowMenu.addValueCycleEntry("grid_size", CommonCycles.cycle("fancymenu.editor.menu_bar.window.grid_size", ListUtils.of(10,20,30,40), preSelectedGridSize)
						.addCycleListener(integer -> {
							FancyMenu.getOptions().layoutEditorGridSize.setValue(integer);
						}))
				.setIcon(ContextMenu.IconFactory.getIcon("measure"));

		windowMenu.addSeparatorEntry("separator_after_grid_size");

		windowMenu.addValueCycleEntry("anchor_overlay_visibility_mode",
				AnchorPointOverlay.AnchorOverlayVisibilityMode.ALWAYS.cycle(editor.anchorPointOverlay.getVisibilityMode())
						.addCycleListener(anchorOverlayVisibilityMode -> FancyMenu.getOptions().anchorOverlayVisibilityMode.setValue(anchorOverlayVisibilityMode.getName())));

		windowMenu.addValueCycleEntry("show_all_anchor_connections",
				CommonCycles.cycleEnabledDisabled("fancymenu.editor.anchor_overlay.show_all_anchor_connections",
								FancyMenu.getOptions().showAllAnchorOverlayConnections.getValue())
						.addCycleListener(cycle -> FancyMenu.getOptions().showAllAnchorOverlayConnections.setValue(cycle.getAsBoolean())));

		windowMenu.addSeparatorEntry("separator_after_show_all_anchor_connections");

		windowMenu.addValueCycleEntry("anchor_area_hovering",
						CommonCycles.cycleEnabledDisabled("fancymenu.editor.anchor_overlay.change_anchor_on_area_hover",
										FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.getValue())
								.addCycleListener(cycle -> FancyMenu.getOptions().anchorOverlayChangeAnchorOnAreaHover.setValue(cycle.getAsBoolean())))
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.change_anchor_on_area_hover.desc")));

		windowMenu.addValueCycleEntry("anchor_element_hovering",
						CommonCycles.cycleEnabledDisabled("fancymenu.editor.anchor_overlay.change_anchor_on_element_hover",
										FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.getValue())
								.addCycleListener(cycle -> FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.setValue(cycle.getAsBoolean())))
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.change_anchor_on_element_hover.desc")));

		NonStackableOverlayUI.addRangeSliderInputContextMenuEntryTo(windowMenu, "anchor_overlay_hover_charging_time",
						Component.translatable("fancymenu.editor.anchor_overlay.charging_time"),
						() -> FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getValue(),
						aDouble -> FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.setValue(aDouble),
						true, FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getDefaultValue(),
						1.0D, 20.0D, consumes -> Component.translatable("fancymenu.editor.anchor_overlay.charging_time.slider_label", consumes))
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.charging_time.desc")));

		windowMenu.addSeparatorEntry("separator_after_anchor_overlay_hover_charging_time");

		windowMenu.addValueCycleEntry("invert_anchor_overlay_colors",
						CommonCycles.cycleEnabledDisabled("fancymenu.editor.anchor_overlay.invert_colors",
										FancyMenu.getOptions().invertAnchorOverlayColor.getValue())
								.addCycleListener(cycle -> FancyMenu.getOptions().invertAnchorOverlayColor.setValue(cycle.getAsBoolean())))
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.invert_colors.desc")));

		NonStackableOverlayUI.addInputContextMenuEntryTo(windowMenu, "custom_anchor_overlay_base_color",
						Component.translatable("fancymenu.editor.anchor_overlay.overlay_color_base"),
						() -> FancyMenu.getOptions().anchorOverlayColorBaseOverride.getValue(),
						s -> FancyMenu.getOptions().anchorOverlayColorBaseOverride.setValue(s),
						true, FancyMenu.getOptions().anchorOverlayColorBaseOverride.getDefaultValue(),
						null, false, false, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null, null)
				.setIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

		NonStackableOverlayUI.addInputContextMenuEntryTo(windowMenu, "custom_anchor_overlay_border_color",
						Component.translatable("fancymenu.editor.anchor_overlay.overlay_color_border"),
						() -> FancyMenu.getOptions().anchorOverlayColorBorderOverride.getValue(),
						s -> FancyMenu.getOptions().anchorOverlayColorBorderOverride.setValue(s),
						true, FancyMenu.getOptions().anchorOverlayColorBorderOverride.getDefaultValue(),
						null, false, false, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null, null)
				.setIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

		windowMenu.addSeparatorEntry("separator_after_custom_anchor_overlay_border_color");

		NonStackableOverlayUI.addRangeSliderInputContextMenuEntryTo(windowMenu, "anchor_overlay_opacity_normal",
						Component.translatable("fancymenu.editor.anchor_overlay.opacity_normal"),
						() -> Double.valueOf(FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getValue()),
						aDouble -> FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.setValue(aDouble.floatValue()),
						true, (double) FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getDefaultValue(),
						0.0D, 1.0D, consumes -> Component.translatable("fancymenu.editor.anchor_overlay.opacity_normal.slider_label", ((int)(consumes * 100.0D)) + "%"))
				.setIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

		NonStackableOverlayUI.addRangeSliderInputContextMenuEntryTo(windowMenu, "anchor_overlay_opacity_busy",
						Component.translatable("fancymenu.editor.anchor_overlay.opacity_busy"),
						() -> Double.valueOf(FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getValue()),
						aDouble -> FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.setValue(aDouble.floatValue()),
						true, (double) FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getDefaultValue(),
						0.0D, 1.0D, consumes -> Component.translatable("fancymenu.editor.anchor_overlay.opacity_busy.slider_label", ((int)(consumes * 100.0D)) + "%"))
				.setIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

		//USER INTERFACE
		CustomizationOverlayUI.buildUITabAndAddTo(menuBar);

		//HELP
		CustomizationOverlayUI.buildHelpTabAndAddTo(menuBar);

		//CLOSE EDITOR BUTTON
		menuBar.addClickableEntry(MenuBar.Side.RIGHT, "close_editor", Component.empty(), (bar, entry) -> {
					displayUnsavedWarning(call -> {
						if (call) {
							editor.closeEditor();
						} else {
							Minecraft.getInstance().setScreen(editor);
						}
					});
				}).setIconTexture(CLOSE_EDITOR_TEXTURE)
				.setIconTextureColor(() -> UIBase.getUIColorTheme().layout_editor_close_icon_color);

		return menuBar;

	}

	protected static void displayUnsavedWarning(@NotNull Consumer<Boolean> callback) {
		Minecraft.getInstance().setScreen(ConfirmationScreen.warning(callback, LocalizationUtils.splitLocalizedLines("fancymenu.editor.warning.unsaved")));
	}

	@NotNull
	public static ContextMenu buildEditorWidgetsContextMenu(@NotNull LayoutEditorScreen editor) {
		ContextMenu menu = new ContextMenu();
		int i = 0;
		for (AbstractLayoutEditorWidget w : editor.layoutEditorWidgets) {
			menu.addClickableEntry("widget_" + i, w.getDisplayLabel(), (menu1, entry) -> {
				w.setVisible(true);
			});
			i++;
		}
		return menu;
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
				Minecraft.getInstance().setScreen(new StringListChooserScreen(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), editor.layout.universalLayoutMenuBlacklist, s1 -> {
					if (s1 != null) {
						Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
							if (call2) {
								editor.history.saveSnapshot();
								editor.layout.universalLayoutMenuBlacklist.remove(s1);
							}
							Minecraft.getInstance().setScreen(editor);
						}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist.confirm")));
					} else {
						Minecraft.getInstance().setScreen(editor);
					}
				}));
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
				Minecraft.getInstance().setScreen(new StringListChooserScreen(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), editor.layout.universalLayoutMenuWhitelist, s1 -> {
					if (s1 != null) {
						Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call2) -> {
							if (call2) {
								editor.history.saveSnapshot();
								editor.layout.universalLayoutMenuWhitelist.remove(s1);
							}
							Minecraft.getInstance().setScreen(editor);
						}, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist.confirm")));
					} else {
						Minecraft.getInstance().setScreen(editor);
					}
				}));
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
				}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.backgroundoptions.setbackground.btn.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("image"));

		menu.addValueCycleEntry("keep_background_aspect_ratio", CommonCycles.cycleEnabledDisabled("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect", editor.layout.preserveBackgroundAspectRatio).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.preserveBackgroundAspectRatio = cycle.getAsBoolean();
		})).setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"));

		menu.addValueCycleEntry("show_overlay_on_custom_background", CommonCycles.cycleEnabledDisabled("fancymenu.editor.background.show_overlay_on_custom_background", editor.layout.showScreenBackgroundOverlayOnCustomBackground).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.showScreenBackgroundOverlayOnCustomBackground = cycle.getAsBoolean();
		})).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.background.show_overlay_on_custom_background.desc")));

		//TODO übernehmen
		menu.addValueCycleEntry("apply_vanilla_background_blur", CommonCycles.cycleEnabledDisabled("fancymenu.editor.background.blur_background", editor.layout.applyVanillaBackgroundBlur).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.applyVanillaBackgroundBlur = cycle.getAsBoolean();
		})).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.background.blur_background.desc")));

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
					.setTooltipSupplier((menu1, entry) -> !(editor.layoutTargetScreen instanceof CustomGuiBaseScreen) ? Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.desc")) : Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.custom_gui.desc")))
					.setIcon(ContextMenu.IconFactory.getIcon("text"))
					.setIsActiveSupplier((menu1, entry) -> !(editor.layoutTargetScreen instanceof CustomGuiBaseScreen));

			menu.addSeparatorEntry("separator_after_edit_menu_title");

		}

		menu.addSubMenuEntry("scroll_list_customizations", Component.translatable("fancymenu.customization.scroll_lists"), buildScrollListCustomizationsContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("scroll_edit"))
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.scroll_lists.desc")))
				.setIsActiveSupplier((menu1, entry) -> !(editor.layoutTargetScreen instanceof CustomGuiBaseScreen));

		menu.addSeparatorEntry("separator_after_scroll_list_customizations");

		menu.addClickableEntry("layout_index", Component.translatable("fancymenu.editor.layout.index"), (menu1, entry) -> {
					TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.editor.layout.index"), CharacterFilter.buildIntegerFiler(), s1 -> {
						if ((s1 != null) && MathUtils.isInteger(s1)) {
							editor.history.saveSnapshot();
							editor.layout.layoutIndex = Integer.parseInt(s1);
						}
						Minecraft.getInstance().setScreen(editor);
					});
					s.setTextValidator(consumes -> TextValidators.INTEGER_TEXT_VALIDATOR.get(consumes.getText()));
					s.setText("" + editor.layout.layoutIndex);
					Minecraft.getInstance().setScreen(s);
				}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.layout.index.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("stack"));

		menu.addSeparatorEntry("separator_after_layout_index");

		menu.addValueCycleEntry("random_mode", CommonCycles.cycleEnabledDisabled("fancymenu.fancymenu.editor.layoutoptions.randommode", editor.layout.randomMode).addCycleListener(cycle -> {
					editor.history.saveSnapshot();
					editor.layout.randomMode = cycle.getAsBoolean();
				})).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.btn.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("random"));

		menu.addClickableEntry("random_mode_group", Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), (menu1, entry) -> {
					Minecraft.getInstance().setScreen(TextInputScreen.build(Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), CharacterFilter.buildIntegerFiler(), call -> {
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
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup.btn.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("group"));

		menu.addValueCycleEntry("random_mode_first_time", CommonCycles.cycleEnabledDisabled("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime", editor.layout.randomOnlyFirstTime).addCycleListener(cycle -> {
					editor.history.saveSnapshot();
					editor.layout.randomOnlyFirstTime = cycle.getAsBoolean();
				})).setIsActiveSupplier((menu1, entry) -> editor.layout.randomMode)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.btn.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("random_once"));

		menu.addSeparatorEntry("separator_after_random_mode_first_time");

		menu.addValueCycleEntry("render_custom_elements_behind_vanilla", CommonCycles.cycleEnabledDisabled("fancymenu.editor.render_custom_behind_vanilla", editor.layout.renderElementsBehindVanilla).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.renderElementsBehindVanilla = cycle.getAsBoolean();
		})).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.render_custom_behind_vanilla.desc")));

		menu.addSeparatorEntry("separator_after_render_custom_behind_vanilla");

		LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> cycleAutoScaling = CommonCycles.cycleEnabledDisabled("fancymenu.helper.editor.properties.autoscale", ((editor.layout.autoScalingWidth != 0) && (editor.layout.autoScalingHeight != 0)));
		cycleAutoScaling.addCycleListener(cycle -> {
			if (cycle.getAsBoolean()) {
				menu.closeMenu();
				Minecraft.getInstance().setScreen(new AutoScalingScreen(editor, call -> {
					if (!call) {
						cycleAutoScaling.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED, false);
					}
					Minecraft.getInstance().setScreen(editor);
				}));
			} else {
				editor.history.saveSnapshot();
				editor.layout.autoScalingWidth = 0;
				editor.layout.autoScalingHeight = 0;
				menu.closeMenu();
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
				})
				.setIcon(ContextMenu.IconFactory.getIcon("measure"));

		NonStackableOverlayUI.addIntegerInputContextMenuEntryTo(menu, "forced_gui_scale", Component.translatable("fancymenu.editor.rightclick.scale"),
						() -> (int) editor.layout.forcedScale,
						integer -> {
							editor.history.saveSnapshot();
							editor.layout.forcedScale = integer;
							if (integer == 0) {
								editor.layout.autoScalingWidth = 0;
								editor.layout.autoScalingHeight = 0;
							}
							menu.closeMenu();
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
						}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.scale.btn.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("measure"))
				.setIsActiveSupplier((menu1, entry) -> editor.layout.autoScalingWidth == 0)
				.setTooltipSupplier((menu1, entry) -> entry.isActive() ? null : Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.auto_scaling.disable_forced_scale_first")));

		menu.addSeparatorEntry("separator_after_forced_scale");

		NonStackableOverlayUI.addAudioResourceChooserContextMenuEntryTo(menu, "open_audio",
						null,
						() -> editor.layout.openAudio,
						iAudioResourceSupplier -> {
							editor.history.saveSnapshot();
							editor.layout.openAudio = iAudioResourceSupplier;
						},
						Component.translatable("fancymenu.editor.open_audio"),
						true, null, true, true, true)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.open_audio.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("sound"));

		NonStackableOverlayUI.addAudioResourceChooserContextMenuEntryTo(menu, "close_audio",
						null,
						() -> editor.layout.closeAudio,
						iAudioResourceSupplier -> {
							editor.history.saveSnapshot();
							editor.layout.closeAudio = iAudioResourceSupplier;
						},
						Component.translatable("fancymenu.editor.close_audio"),
						true, null, true, true, true)
				.setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.close_audio.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("sound"));

		menu.addSeparatorEntry("separator_after_close_audio");

		menu.addClickableEntry("layout_wide_requirements", Component.translatable("fancymenu.editor.loading_requirement.layouts.loading_requirements"), (menu1, entry) -> {
					Minecraft.getInstance().setScreen(new ManageRequirementsScreen(editor.layout.layoutWideLoadingRequirementContainer.copy(false), (call) -> {
						if (call != null) {
							editor.layout.layoutWideLoadingRequirementContainer = call;
						}
						Minecraft.getInstance().setScreen(editor);
					}));
				}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.layouts.loading_requirements.desc")))
				.setIcon(ContextMenu.IconFactory.getIcon("check_list"));

		menu.addSeparatorEntry("separator_after_layout_wide_requirements");

		menu.addClickableEntry("paste_elements", Component.translatable("fancymenu.editor.edit.paste"), (menu1, entry) -> {
					editor.history.saveSnapshot();
					editor.pasteElementsFromClipboard();
				}).setIsActiveSupplier((menu1, entry) -> !LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.isEmpty())
				.setIcon(ContextMenu.IconFactory.getIcon("paste"));

		menu.addSeparatorEntry("separator_after_paste_elements");

		menu.addSubMenuEntry("add_element", Component.translatable("fancymenu.editor.layoutproperties.newelement"), buildElementContextMenu(editor))
				.setIcon(ContextMenu.IconFactory.getIcon("add"));

		return menu;

	}

	@NotNull
	public static ContextMenu buildScrollListCustomizationsContextMenu(@NotNull LayoutEditorScreen editor) {

		ContextMenu menu = new ContextMenu();

		NonStackableOverlayUI.addImageResourceChooserContextMenuEntryTo(menu, "header_texture",
						null,
						() -> editor.layout.scrollListHeaderTexture,
						iTextureResourceSupplier -> {
							editor.history.saveSnapshot();
							editor.layout.scrollListHeaderTexture = iTextureResourceSupplier;
						},
						Component.translatable("fancymenu.customization.scroll_lists.header_texture"),
						true, null, true, true, true)
				.setIcon(ContextMenu.IconFactory.getIcon("image"));

		NonStackableOverlayUI.addImageResourceChooserContextMenuEntryTo(menu, "footer_texture",
						null,
						() -> editor.layout.scrollListFooterTexture,
						iTextureResourceSupplier -> {
							editor.history.saveSnapshot();
							editor.layout.scrollListFooterTexture = iTextureResourceSupplier;
						},
						Component.translatable("fancymenu.customization.scroll_lists.footer_texture"),
						true, null, true, true, true)
				.setIcon(ContextMenu.IconFactory.getIcon("image"));

		menu.addSeparatorEntry("separator_after_footer_texture");

		menu.addValueCycleEntry("repeat_header_texture", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.repeat_header", editor.layout.repeatScrollListHeaderTexture).addCycleListener(cycleEnabledDisabled -> {
			editor.history.saveSnapshot();
			editor.layout.repeatScrollListHeaderTexture = cycleEnabledDisabled.getAsBoolean();
		})).setIsActiveSupplier((menu1, entry) -> !editor.layout.preserveScrollListHeaderFooterAspectRatio);

		menu.addValueCycleEntry("repeat_footer_texture", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.repeat_footer", editor.layout.repeatScrollListFooterTexture).addCycleListener(cycleEnabledDisabled -> {
			editor.history.saveSnapshot();
			editor.layout.repeatScrollListFooterTexture = cycleEnabledDisabled.getAsBoolean();
		})).setIsActiveSupplier((menu1, entry) -> !editor.layout.preserveScrollListHeaderFooterAspectRatio);

		menu.addSeparatorEntry("separator_after_header_footer_repeat_texture");

		menu.addValueCycleEntry("preserve_header_footer_aspect_ratio", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.preserve_header_footer_aspect_ratio", editor.layout.preserveScrollListHeaderFooterAspectRatio).addCycleListener(cycle -> {
					editor.history.saveSnapshot();
					editor.layout.preserveScrollListHeaderFooterAspectRatio = cycle.getAsBoolean();
				})).setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"))
				.setIsActiveSupplier((menu1, entry) -> (!editor.layout.repeatScrollListHeaderTexture && !editor.layout.repeatScrollListFooterTexture));

		menu.addSeparatorEntry("separator_after_preserve_aspect_ratio");

		menu.addValueCycleEntry("show_header_footer_preview", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.show_preview", editor.layout.showScrollListHeaderFooterPreviewInEditor).addCycleListener(cycle -> {
			editor.history.saveSnapshot();
			editor.layout.showScrollListHeaderFooterPreviewInEditor = cycle.getAsBoolean();
		})).setIcon(ContextMenu.IconFactory.getIcon("eye"));

		return menu;

	}

	@NotNull
	public static ContextMenu buildElementContextMenu(@NotNull LayoutEditorScreen editor) {

		ContextMenu menu = new ContextMenu();

		int i = 0;
		for (ElementBuilder<?,?> builder : ElementRegistry.getBuilders()) {
			if ((LayoutEditorScreen.getCurrentInstance() != null) && !builder.shouldShowUpInEditorElementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
			if (!builder.isDeprecated()) {
				ContextMenu.ClickableContextMenuEntry<?> entry = menu.addClickableEntry("element_" + i, builder.getDisplayName(null), (menu1, entry1) -> {
					AbstractEditorElement editorElement = builder.wrapIntoEditorElementInternal(builder.buildDefaultInstance(), editor);
					if (editorElement != null) {
						editor.history.saveSnapshot();
						editor.normalEditorElements.add(editorElement);
						if ((editor.rightClickMenuOpenPosX != -1000) && (editor.rightClickMenuOpenPosY != -1000)) {
							//Add new element at right-click menu coordinates
							editorElement.setAnchorPoint(editorElement.element.anchorPoint, true, editor.rightClickMenuOpenPosX, editor.rightClickMenuOpenPosY, true);
							editor.deselectAllElements();
							editorElement.setSelected(true);
							editor.rightClickMenuOpenPosX = -1000;
							editor.rightClickMenuOpenPosY = -1000;
						}
						for (AbstractLayoutEditorWidget w : editor.layoutEditorWidgets) {
							w.editorElementAdded(editorElement);
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

				List<VanillaWidgetEditorElement> hiddenVanillaButtons = new ArrayList<>();
				for (VanillaWidgetEditorElement e : editor.vanillaWidgetEditorElements) {
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
				for (VanillaWidgetEditorElement e : hiddenVanillaButtons) {
					AbstractWidget w = ((VanillaWidgetElement)e.element).getWidget();
					this.addClickableEntry("element_" + i, (w != null) ? w.getMessage() : Component.empty(), (menu1, entry) -> {
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

			List<Layout> allLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true);
			int allLayoutsCount = allLayouts.size();
			int i = 0;
			for (Layout l : LayoutHandler.sortLayoutListByLastEdited(allLayouts, true, 8)) {
				if (l.getLayoutName().equals(editor.layout.getLayoutName())) continue; //Don't show the current layout in the list
				menu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(editor, l))
						.setLabelSupplier((menu1, entry) -> {
							Style style = l.getStatus().getValueComponentStyle();
							MutableComponent c = Component.literal(l.getLayoutName());
							c.append(Component.literal(" (").setStyle(style));
							c.append(l.getStatus().getValueComponent());
							c.append(Component.literal(")").setStyle(style));
							return c;
						});
				i++;
			}
			if (allLayoutsCount > 8) {
				String moreLayoutCount = "" + (allLayoutsCount-8);
				menu.addClickableEntry("x_more_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.more_layouts", moreLayoutCount), (menu1, entry) -> {
					displayUnsavedWarning(call -> {
						if (call) {
							editor.saveWidgetSettings();
							Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), editor.layoutTargetScreen, layouts -> {
								Minecraft.getInstance().setScreen(editor);
							}));
						} else {
							Minecraft.getInstance().setScreen(editor);
						}
					});
				});
			}

			menu.addSeparatorEntry("separator_after_recent_layouts");

			menu.addClickableEntry("all_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu1, entry) -> {
				displayUnsavedWarning(call -> {
					if (call) {
						editor.saveWidgetSettings();
						Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), editor.layoutTargetScreen, layouts -> {
							Minecraft.getInstance().setScreen(editor);
						}));
					} else {
						Minecraft.getInstance().setScreen(editor);
					}
				});
			});

		} else if (editor.layout.screenIdentifier != null) {

			List<Layout> allLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(editor.layout.screenIdentifier, false);
			int allLayoutsCount = allLayouts.size();
			int i = 0;
			for (Layout l : LayoutHandler.sortLayoutListByLastEdited(allLayouts, true, 8)) {
				if (l.getLayoutName().equals(editor.layout.getLayoutName())) continue; //Don't show the current layout in the list
				menu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(editor, l))
						.setLabelSupplier((menu1, entry) -> {
							Style style = l.getStatus().getValueComponentStyle();
							MutableComponent c = Component.literal(l.getLayoutName());
							c.append(Component.literal(" (").setStyle(style));
							c.append(l.getStatus().getValueComponent());
							c.append(Component.literal(")").setStyle(style));
							return c;
						});
				i++;
			}
			if (allLayoutsCount > 8) {
				String moreLayoutCount = "" + (allLayoutsCount-8);
				menu.addClickableEntry("x_more_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.more_layouts", moreLayoutCount), (menu1, entry) -> {
					displayUnsavedWarning(call -> {
						if (call) {
							editor.saveWidgetSettings();
							Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(editor.layout.screenIdentifier, false), editor.layoutTargetScreen, layouts -> {
								Minecraft.getInstance().setScreen(editor);
							}));
						} else {
							Minecraft.getInstance().setScreen(editor);
						}
					});
				});
			}

			menu.addSeparatorEntry("separator_after_recent_layouts");

			menu.addClickableEntry("all_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu1, entry) -> {
				displayUnsavedWarning(call -> {
					if (call) {
						editor.saveWidgetSettings();
						Minecraft.getInstance().setScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(editor.layout.screenIdentifier, false), editor.layoutTargetScreen, layouts -> {
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
		}).setIcon(ContextMenu.IconFactory.getIcon("edit"));

		menu.addClickableEntry("edit_in_system_text_editor", Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (menu1, entry) -> {
			if (layout.layoutFile != null) {
				FileUtils.openFile(layout.layoutFile);
			}
		});

		return menu;

	}

}
