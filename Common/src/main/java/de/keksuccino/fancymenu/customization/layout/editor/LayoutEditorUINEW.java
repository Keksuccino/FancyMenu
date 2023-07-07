package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.background.ChooseMenuBackgroundScreen;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ChooseFromStringListScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class LayoutEditorUINEW {

	private static MenuBar grandfatheredMenuBar = null;

	@NotNull
	public static MenuBar buildMenuBar() {

		if (grandfatheredMenuBar != null) {
			MenuBar mb = grandfatheredMenuBar;
			grandfatheredMenuBar = null;
			return  mb;
		}

		MenuBar menuBar = new MenuBar();

		//TODO ---

		return menuBar;

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

			menu.addClickableEntry("edit_menu_title", Component.translatable("fancymenu.helper.editor.edit_menu_title"), (menu1, entry) -> {
				TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.edit_menu_title"), editor, null, (call) -> {
					if (call != null) {
						editor.history.saveSnapshot();
						if (!call.replace(" ", "").isEmpty()) {
							editor.layout.customMenuTitle = call;
						} else {
							editor.layout.customMenuTitle = null;
						}
					}
				});
				s.multilineMode = false;
				if (editor.layout.customMenuTitle != null) {
					s.setText(editor.layout.customMenuTitle);
				}
				Minecraft.getInstance().setScreen(s);
			}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.desc")));

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
						editor.init(Minecraft.getInstance(), Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
					} else {
						cycleAutoScaling.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED, false);
					}
				}));
			} else {
				editor.history.saveSnapshot();
				editor.layout.autoScalingWidth = 0;
				editor.layout.autoScalingHeight = 0;
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

		menu.addClickableEntry("forced_gui_scale", Component.translatable(""), (menu1, entry) -> {
			Minecraft.getInstance().setScreen(TextInputScreen.build(Component.translatable("fancymenu.editor.rightclick.scale"), CharacterFilter.buildIntegerCharacterFiler(), call -> {
				if (call != null) {
					int scale = 0;
					if (MathUtils.isInteger(call)) {
						scale = Integer.parseInt(call);
					}
					if (editor.layout.forcedScale != scale) {
						editor.history.saveSnapshot();
					}
					editor.layout.forcedScale = scale;
				}
				Minecraft.getInstance().setScreen(editor);
			}).setText("" + editor.layout.forcedScale)
					.setTextValidator(consumes -> {
						if (MathUtils.isInteger(consumes.getText())) {
							if (Integer.parseInt(consumes.getText()) < 0) {
								consumes.setTextValidatorUserFeedback(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.rightclick.scale.invalid")));
								return false;
							}
						}
						consumes.setTextValidatorUserFeedback(null);
						return true;
					}));
		}).setTooltipSupplier((menu1, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.scale.btn.desc")));

		menu.addSeparatorEntry("separator_after_forced_scale");

		//TODO hier weiter menü porten ------------

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

}
