package de.keksuccino.fancymenu.customization.layout.editor;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.ManageLayoutsScreen;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlay;
import de.keksuccino.fancymenu.customization.overlay.CustomizationOverlayUI;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringListChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class LayoutEditorUI implements ContextMenuBuilder<LayoutEditorUI> {

    private static final PngTexture CLOSE_EDITOR_TEXTURE = PngTexture.location(ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/menubar/icons/close.png"));

    @Nullable
    private MenuBar grandfatheredMenuBar = null;
    @NotNull
    private final LayoutEditorScreen editor;
    private final List<ContextMenuScreenOpenProcessor> contextMenuScreenOpenProcessors = new ArrayList<>();

    public LayoutEditorUI(@NotNull LayoutEditorScreen editor) {
        this.editor = Objects.requireNonNull(editor);
        this.addContextMenuScreenOpenProcessor(this.editor::beforeOpenChildScreen);
    }

    @NotNull
    public MenuBar buildMenuBar(boolean expanded) {

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
                }
            });
        }).setIcon(ContextMenu.IconFactory.getIcon("add"));

        layoutMenu.addSubMenuEntry("open_layout", Component.translatable("fancymenu.editor.layout.open"), buildOpenLayoutContextMenu())
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

        layoutMenu.addSubMenuEntry("layout_settings", Component.translatable("fancymenu.editor.layout.properties"), buildRightClickContextMenu())
                .setIcon(ContextMenu.IconFactory.getIcon("settings"))
                .setHoverAction((menu, entry, isPost) -> {
                    if (!isPost) {
                        int menuBarHeight = (int)((float)MenuBar.HEIGHT * MenuBar.getRenderScale());
                        editor.rightClickMenuOpenPosX = 20;
                        editor.rightClickMenuOpenPosY = menuBarHeight + 20;
                    }
                });

        layoutMenu.addSeparatorEntry("separator_after_layout_settings");

        layoutMenu.addClickableEntry("close_editor", Component.translatable("fancymenu.editor.close"), (menu, entry) -> {
            displayUnsavedWarning(call -> {
                if (call) {
                    editor.closeEditor();
                }
            });
        }).setIcon(ContextMenu.IconFactory.getIcon("close"));

        //EDIT
        ContextMenu editMenu = new ContextMenu();
        menuBar.addContextMenuEntry("edit_tab", Component.translatable("fancymenu.editor.edit"), editMenu);

        editMenu.addClickableEntry("undo_action", Component.translatable("fancymenu.editor.edit.undo"), (menu, entry) -> {
                    grandfatheredMenuBar = menuBar;
                    editor.history.stepBack();
                    editor.resize(Minecraft.getInstance(), editor.width, editor.height);
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.undo"))
                .setIcon(ContextMenu.IconFactory.getIcon("undo"));

        editMenu.addClickableEntry("redo_action", Component.translatable("fancymenu.editor.edit.redo"), (menu, entry) -> {
                    grandfatheredMenuBar = menuBar;
                    editor.history.stepForward();
                    editor.resize(Minecraft.getInstance(), editor.width, editor.height);
                }).setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.redo"))
                .setIcon(ContextMenu.IconFactory.getIcon("redo"));

        editMenu.addSeparatorEntry("separator_after_redo");

        editMenu.addClickableEntry("copy_selected_elements", Component.translatable("fancymenu.editor.edit.copy"), (menu, entry) -> {
                    editor.copyElementsToClipboard(editor.getSelectedElements().toArray(new AbstractEditorElement[0]));
                }).addIsActiveSupplier((menu, entry) -> !editor.getSelectedElements().isEmpty())
                .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"))
                .setIcon(ContextMenu.IconFactory.getIcon("copy"));

        editMenu.addClickableEntry("paste_copied_elements", Component.translatable("fancymenu.editor.edit.paste"), (menu, entry) -> {
                    editor.history.saveSnapshot();
                    editor.pasteElementsFromClipboard();
                }).addIsActiveSupplier((menu, entry) -> !LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.isEmpty())
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

        elementMenu.addSubMenuEntry("new_element", Component.translatable("fancymenu.editor.element.new"), buildElementContextMenu())
                .setIcon(ContextMenu.IconFactory.getIcon("add"));

        elementMenu.addSubMenuEntry("manage_hidden_vanilla_elements", Component.translatable("fancymenu.fancymenu.editor.element.deleted_vanilla_elements"), buildHiddenVanillaElementsContextMenu())
                .setIcon(ContextMenu.IconFactory.getIcon("delete"));

        //WINDOW
        ContextMenu windowMenu = new ContextMenu();
        menuBar.addContextMenuEntry("window_tab", Component.translatable("fancymenu.editor.menu_bar.window"), windowMenu);

        windowMenu.addSubMenuEntry("editor_widgets", Component.translatable("fancymenu.editor.widgets"), buildEditorWidgetsContextMenu())
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

        List<Integer> gridSizes = List.of(10,20,30,40,50,60,70,80,90,100);
        int preSelectedGridSize = FancyMenu.getOptions().layoutEditorGridSize.getValue();
        if (!gridSizes.contains(preSelectedGridSize)) {
            preSelectedGridSize = 10;
            FancyMenu.getOptions().layoutEditorGridSize.setValue(preSelectedGridSize);
        }
        windowMenu.addValueCycleEntry("grid_size", CommonCycles.cycle("fancymenu.editor.menu_bar.window.grid_size", gridSizes, preSelectedGridSize)
                        .addCycleListener(integer -> {
                            FancyMenu.getOptions().layoutEditorGridSize.setValue(integer);
                        }))
                .setIcon(ContextMenu.IconFactory.getIcon("measure"));

        windowMenu.addValueCycleEntry("grid_snapping", CommonCycles.cycleEnabledDisabled("fancymenu.layout_editor.grid.snapping", FancyMenu.getOptions().layoutEditorGridSnapping.getValue())
                        .addCycleListener(cycleEnabledDisabled -> {
                            FancyMenu.getOptions().layoutEditorGridSnapping.setValue(cycleEnabledDisabled.getAsBoolean());
                        }))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.layout_editor.grid.snapping.desc")));

        List<Float> gridSnappingStrengths = List.of(0.5f,0.75f,1.0f,1.5f,2.0f,3.0f,5.0f);
        float preSelectedGridSnappingStrength = FancyMenu.getOptions().layoutEditorGridSnappingStrength.getValue();
        if (!gridSnappingStrengths.contains(preSelectedGridSnappingStrength)) {
            preSelectedGridSnappingStrength = 1.0f;
            FancyMenu.getOptions().layoutEditorGridSnappingStrength.setValue(preSelectedGridSnappingStrength);
        }
        windowMenu.addValueCycleEntry("grid_snapping_strength", CommonCycles.cycle("fancymenu.layout_editor.grid.snapping.strength", gridSnappingStrengths, preSelectedGridSnappingStrength)
                        .setValueNameSupplier(strength -> {
                            if (strength == 0.5f) return I18n.get("fancymenu.layout_editor.grid.snapping.strength.pixel_perfect");
                            if (strength == 0.75f) return I18n.get("fancymenu.layout_editor.grid.snapping.strength.high_precision");
                            if (strength == 1.5f) return I18n.get("fancymenu.layout_editor.grid.snapping.strength.moderate");
                            if (strength == 2.0f) return I18n.get("fancymenu.layout_editor.grid.snapping.strength.magnetic");
                            if (strength == 3.0f) return I18n.get("fancymenu.layout_editor.grid.snapping.strength.strong_magnetic");
                            if (strength == 5.0f) return I18n.get("fancymenu.layout_editor.grid.snapping.strength.maximum");
                            return I18n.get("fancymenu.layout_editor.grid.snapping.strength.standard"); // 1.0f
                        })
                        .setValueComponentStyleSupplier(consumes -> LocalizedCycleEnum.WARNING_TEXT_STYLE.get())
                        .addCycleListener(strength -> {
                            FancyMenu.getOptions().layoutEditorGridSnappingStrength.setValue(strength);
                        }))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.layout_editor.grid.snapping.strength.desc")));

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
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.change_anchor_on_area_hover.desc")));

        windowMenu.addValueCycleEntry("anchor_element_hovering",
                        CommonCycles.cycleEnabledDisabled("fancymenu.editor.anchor_overlay.change_anchor_on_element_hover",
                                        FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.getValue())
                                .addCycleListener(cycle -> FancyMenu.getOptions().anchorOverlayChangeAnchorOnElementHover.setValue(cycle.getAsBoolean())))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.change_anchor_on_element_hover.desc")));

        ContextMenuBuilder<LayoutEditorUI> editorSettingsMenuBuilder = this.buildEditorSettingsContextMenuBuilder();
        ConsumingSupplier<LayoutEditorUI, Boolean> layoutEditorFilter = consumes -> true;

        editorSettingsMenuBuilder.addRangeSliderInputContextMenuEntryTo(windowMenu, "anchor_overlay_hover_charging_time",
                        layoutEditorFilter,
                        consumes -> FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getValue(),
                        (consumes, value) -> FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.setValue(value),
                        Component.translatable("fancymenu.editor.anchor_overlay.charging_time"),
                        true, FancyMenu.getOptions().anchorOverlayHoverChargingTimeSeconds.getDefaultValue(),
                        1.0D, 20.0D, value -> Component.translatable("fancymenu.editor.anchor_overlay.charging_time.slider_label", value))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.charging_time.desc")));

        windowMenu.addSeparatorEntry("separator_after_anchor_overlay_hover_charging_time");

        windowMenu.addValueCycleEntry("invert_anchor_overlay_colors",
                        CommonCycles.cycleEnabledDisabled("fancymenu.editor.anchor_overlay.invert_colors",
                                        FancyMenu.getOptions().invertAnchorOverlayColor.getValue())
                                .addCycleListener(cycle -> FancyMenu.getOptions().invertAnchorOverlayColor.setValue(cycle.getAsBoolean())))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.anchor_overlay.invert_colors.desc")));

        editorSettingsMenuBuilder.addInputContextMenuEntryTo(windowMenu, "custom_anchor_overlay_base_color",
                        layoutEditorFilter,
                        consumes -> FancyMenu.getOptions().anchorOverlayColorBaseOverride.getValue(),
                        (consumes, value) -> FancyMenu.getOptions().anchorOverlayColorBaseOverride.setValue(value),
                        null, false, false,
                        Component.translatable("fancymenu.editor.anchor_overlay.overlay_color_base"),
                        true, FancyMenu.getOptions().anchorOverlayColorBaseOverride.getDefaultValue(),
                        TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .addIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

        editorSettingsMenuBuilder.addInputContextMenuEntryTo(windowMenu, "custom_anchor_overlay_border_color",
                        layoutEditorFilter,
                        consumes -> FancyMenu.getOptions().anchorOverlayColorBorderOverride.getValue(),
                        (consumes, value) -> FancyMenu.getOptions().anchorOverlayColorBorderOverride.setValue(value),
                        null, false, false,
                        Component.translatable("fancymenu.editor.anchor_overlay.overlay_color_border"),
                        true, FancyMenu.getOptions().anchorOverlayColorBorderOverride.getDefaultValue(),
                        TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
                .addIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

        windowMenu.addSeparatorEntry("separator_after_custom_anchor_overlay_border_color");

        editorSettingsMenuBuilder.addRangeSliderInputContextMenuEntryTo(windowMenu, "anchor_overlay_opacity_normal",
                        layoutEditorFilter,
                        consumes -> Double.valueOf(FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getValue()),
                        (consumes, value) -> FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.setValue(value.floatValue()),
                        Component.translatable("fancymenu.editor.anchor_overlay.opacity_normal"),
                        true, (double) FancyMenu.getOptions().anchorOverlayOpacityPercentageNormal.getDefaultValue(),
                        0.0D, 1.0D, value -> Component.translatable("fancymenu.editor.anchor_overlay.opacity_normal.slider_label", ((int)(value * 100.0D)) + "%"))
                .addIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

        editorSettingsMenuBuilder.addRangeSliderInputContextMenuEntryTo(windowMenu, "anchor_overlay_opacity_busy",
                        layoutEditorFilter,
                        consumes -> Double.valueOf(FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getValue()),
                        (consumes, value) -> FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.setValue(value.floatValue()),
                        Component.translatable("fancymenu.editor.anchor_overlay.opacity_busy"),
                        true, (double) FancyMenu.getOptions().anchorOverlayOpacityPercentageBusy.getDefaultValue(),
                        0.0D, 1.0D, value -> Component.translatable("fancymenu.editor.anchor_overlay.opacity_busy.slider_label", ((int)(value * 100.0D)) + "%"))
                .addIsActiveSupplier((menu, entry) -> !FancyMenu.getOptions().invertAnchorOverlayColor.getValue());

        windowMenu.addSeparatorEntry("separator_after_anchor_overlay_opacity");

        LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> rotationControlsToggleCycle = CommonCycles.cycleEnabledDisabled("fancymenu.editor.settings.enable_rotation_grabbers", FancyMenu.getOptions().enableElementRotationControls.getValue());
        rotationControlsToggleCycle.addCycleListener(cycle -> FancyMenu.getOptions().enableElementRotationControls.setValue(cycle.getAsBoolean()));
        windowMenu.addValueCycleEntry("toggle_element_rotation_controls", rotationControlsToggleCycle)
                .setTickAction((menu, entry, isPost) -> {
                    //This is to correct the displayed value in case the grid got toggled via shortcut
                    if (FancyMenu.getOptions().enableElementRotationControls.getValue() != rotationControlsToggleCycle.current().getAsBoolean()) {
                        rotationControlsToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.getByBoolean(FancyMenu.getOptions().enableElementRotationControls.getValue()), false);
                    }
                })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.settings.enable_rotation_grabbers.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("reload"));

        LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> tiltingControlsToggleCycle = CommonCycles.cycleEnabledDisabled("fancymenu.editor.settings.enable_element_tilting_controls", FancyMenu.getOptions().enableElementTiltingControls.getValue());
        tiltingControlsToggleCycle.addCycleListener(cycle -> FancyMenu.getOptions().enableElementTiltingControls.setValue(cycle.getAsBoolean()));
        windowMenu.addValueCycleEntry("toggle_element_tilting_controls", tiltingControlsToggleCycle)
                .setTickAction((menu, entry, isPost) -> {
                    //This is to correct the displayed value in case the grid got toggled via shortcut
                    if (FancyMenu.getOptions().enableElementTiltingControls.getValue() != tiltingControlsToggleCycle.current().getAsBoolean()) {
                        tiltingControlsToggleCycle.setCurrentValue(CommonCycles.CycleEnabledDisabled.getByBoolean(FancyMenu.getOptions().enableElementTiltingControls.getValue()), false);
                    }
                })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.settings.enable_element_tilting_controls.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("arrow_horizontal"));

        windowMenu.addSeparatorEntry("separator_after_rotation_tilting_controls");

        //USER INTERFACE
        CustomizationOverlayUI.buildUITabAndAddTo(menuBar);

        //HELP
        CustomizationOverlayUI.buildHelpTabAndAddTo(menuBar);

        //CLOSE EDITOR BUTTON
        menuBar.addClickableEntry(MenuBar.Side.RIGHT, "close_editor", Component.empty(), (bar, entry) -> {
                    displayUnsavedWarning(call -> {
                        if (call) {
                            editor.closeEditor();
                        }
                    });
                }).setIconTexture(CLOSE_EDITOR_TEXTURE)
                .setIconTextureColor(() -> UIBase.getUITheme().layout_editor_close_icon_color);

        menuBar.addClickableEntry(MenuBar.Side.RIGHT, "unsaved_indicator", Component.empty(), (bar, entry) -> {
                }).setLabelSupplier((bar, entry) ->
                        editor.unsavedChanges ? Component.translatable("fancymenu.editor.menu_bar.unsaved_warning").withStyle(Style.EMPTY.withBold(true).withColor(UIBase.getUITheme().warning_text_color.getColorInt())) : Component.empty())
                .setActive(false);

        return menuBar;

    }

    @NotNull
    private ContextMenuBuilder<LayoutEditorUI> buildEditorSettingsContextMenuBuilder() {
        ContextMenuBuilder<LayoutEditorUI> builder = ContextMenuBuilder.createStandalone(
                () -> this.editor,
                filter -> List.of(this),
                () -> this,
                null,
                null,
                null
        );
        for (ContextMenuScreenOpenProcessor processor : this.getContextMenuScreenOpenProcessors()) {
            builder.addContextMenuScreenOpenProcessor(processor);
        }
        return builder;
    }

    protected void displayUnsavedWarning(@NotNull Consumer<Boolean> callback) {
        if (!editor.unsavedChanges) {
            callback.accept(true);
            return;
        }
        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.editor.warning.unsaved"), MessageDialogStyle.WARNING, callback);
//        this.openContextMenuScreen(ConfirmationScreen.warning(callback, LocalizationUtils.splitLocalizedLines("fancymenu.editor.warning.unsaved")));
    }

    @NotNull
    public ContextMenu buildEditorWidgetsContextMenu() {
        ContextMenu menu = new ContextMenu();
        int i = 0;
        for (AbstractLayoutEditorWidget w : editor.layoutEditorWidgets) {
            ContextMenu.ClickableContextMenuEntry<?> entry = menu.addClickableEntry("widget_" + i, Component.empty(), (menu1, entry1) -> {
                w.setVisible(!w.isVisible());
            });
            entry.setLabelSupplier((menu1, entry1) -> buildEditorWidgetToggleLabel(w));
            i++;
        }
        return menu;
    }

    private MutableComponent buildEditorWidgetToggleLabel(@NotNull AbstractLayoutEditorWidget widget) {
        boolean visible = widget.isVisible();
        int color = visible ? UIBase.getUITheme().success_text_color.getColorInt() : UIBase.getUITheme().error_text_color.getColorInt();
        String symbol = visible ? "✔" : "✖";
        MutableComponent label = Component.empty();
        label.append(Component.literal(symbol).withStyle(Style.EMPTY.withColor(color)));
        label.append(Component.literal(" "));
        label.append(widget.getDisplayLabel().copy());
        return label;
    }

    @NotNull
    public ContextMenu buildRightClickContextMenu() {

        ContextMenu menu = new ContextMenu();
        ConsumingSupplier<LayoutEditorUI, Boolean> layoutEditorFilter = consumes -> true;

        if (editor.layout.isUniversalLayout()) {

            ContextMenu universalLayoutMenu = new ContextMenu();
            menu.addSubMenuEntry("universal_layout_settings", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options"), universalLayoutMenu);

            universalLayoutMenu.addClickableEntry("add_blacklist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist"), (menu1, entry) -> {
                TextInputWindowBody s = new TextInputWindowBody(null, call -> {
                    if (call != null) {
                        editor.history.saveSnapshot();
                        if (!editor.layout.universalLayoutMenuBlacklist.contains(call)) {
                            editor.layout.universalLayoutMenuBlacklist.add(call);
                        }
                    }
                });
                Dialogs.openGeneric(s,
                        Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"),
                        ContextMenu.IconFactory.getIcon("text"), TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT);
                menu1.closeMenuChain();
            }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_blacklist.desc")));

            universalLayoutMenu.addClickableEntry("remove_blacklist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist"), (menu1, entry) -> {
                this.openContextMenuScreen(new StringListChooserScreen(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), editor.layout.universalLayoutMenuBlacklist, s1 -> {
                    if (s1 != null) {
                        this.openContextMenuScreen(editor);
                        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_blacklist.confirm"), MessageDialogStyle.WARNING, call2 -> {
                            if (call2) {
                                editor.history.saveSnapshot();
                                editor.layout.universalLayoutMenuBlacklist.remove(s1);
                            }
                        });
                    } else {
                        this.openContextMenuScreen(editor);
                    }
                }));
            });

            universalLayoutMenu.addClickableEntry("clear_blacklist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist"), (menu1, entry) -> {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_blacklist.confirm"), MessageDialogStyle.WARNING, call2 -> {
                    if (call2) {
                        editor.history.saveSnapshot();
                        editor.layout.universalLayoutMenuBlacklist.clear();
                    }
                });
            });

            universalLayoutMenu.addSeparatorEntry("separator_after_clear_blacklist");

            universalLayoutMenu.addClickableEntry("add_whitelist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist"), (menu1, entry) -> {
                TextInputWindowBody s = new TextInputWindowBody(null, call -> {
                    if (call != null) {
                        editor.history.saveSnapshot();
                        if (!editor.layout.universalLayoutMenuWhitelist.contains(call)) {
                            editor.layout.universalLayoutMenuWhitelist.add(call);
                        }
                    }
                });
                Dialogs.openGeneric(s,
                        Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.input_menu_identifier"),
                        ContextMenu.IconFactory.getIcon("text"), TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT);
                menu1.closeMenuChain();
            }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.layoutoptions.universal_layout.options.add_whitelist.desc")));

            universalLayoutMenu.addClickableEntry("remove_whitelist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist"), (menu1, entry) -> {
                this.openContextMenuScreen(new StringListChooserScreen(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.choose_menu_identifier"), editor.layout.universalLayoutMenuWhitelist, s1 -> {
                    if (s1 != null) {
                        this.openContextMenuScreen(editor);
                        Dialogs.openMessageWithCallback(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.remove_whitelist.confirm"), MessageDialogStyle.WARNING, call2 -> {
                            if (call2) {
                                editor.history.saveSnapshot();
                                editor.layout.universalLayoutMenuWhitelist.remove(s1);
                            }
                        });
                    } else {
                        this.openContextMenuScreen(editor);
                    }
                }));
            });

            universalLayoutMenu.addClickableEntry("clear_whitelist", Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist"), (menu1, entry) -> {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.helper.editor.layoutoptions.universal_layout.options.clear_whitelist.confirm"), MessageDialogStyle.WARNING, call2 -> {
                    if (call2) {
                        editor.history.saveSnapshot();
                        editor.layout.universalLayoutMenuWhitelist.clear();
                    }
                });
            });

        }

        menu.addSeparatorEntry("separator_after_universal_layout_menu");

        buildMenuBackgroundsMenuAndAddTo(menu);

        menu.addSeparatorEntry("separator_after_menu_backgrounds");

        menu.addSubMenuEntry("decoration_overlays", Component.translatable("fancymenu.editor.decoration_overlays"), buildDecorationOverlaysMenu())
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.editor.decoration_overlays.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("decoration_overlay"));

        menu.addSeparatorEntry("separator_after_decoration_overlays");

        if ((editor.layoutTargetScreen != null) && !editor.layout.isUniversalLayout()) {

            this.addInputContextMenuEntryTo(menu, "edit_menu_title",
                            layoutEditorFilter,
                            consumes -> editor.layout.customMenuTitle,
                            (consumes, value) -> editor.layout.customMenuTitle = value,
                            null, false, true,
                            Component.translatable("fancymenu.helper.editor.edit_menu_title"),
                            true, null,
                            consumes -> !consumes.isEmpty(),
                            consumes -> consumes.isEmpty() ? UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.reset.invalid_title")) : null)
                    .setTooltipSupplier((menu1, entry) -> !(editor.layoutTargetScreen instanceof CustomGuiBaseScreen) ? UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.desc")) : UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.edit_menu_title.custom_gui.desc")))
                    .setIcon(ContextMenu.IconFactory.getIcon("text"))
                    .addIsActiveSupplier((menu1, entry) -> !(editor.layoutTargetScreen instanceof CustomGuiBaseScreen));

            menu.addSeparatorEntry("separator_after_edit_menu_title");

        }

        menu.addSubMenuEntry("scroll_list_customizations", Component.translatable("fancymenu.customization.scroll_lists"), buildScrollListCustomizationsContextMenu())
                .setIcon(ContextMenu.IconFactory.getIcon("scroll_edit"))
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.customization.scroll_lists.desc")))
                .addIsActiveSupplier((menu1, entry) -> !(editor.layoutTargetScreen instanceof CustomGuiBaseScreen));

        menu.addSeparatorEntry("separator_after_scroll_list_customizations");

        menu.addClickableEntry("layout_index", Component.translatable("fancymenu.editor.layout.index"), (menu1, entry) -> {
                    TextInputWindowBody s = new TextInputWindowBody(CharacterFilter.buildIntegerFilter(), s1 -> {
                        if ((s1 != null) && MathUtils.isInteger(s1)) {
                            editor.history.saveSnapshot();
                            editor.layout.layoutIndex = Integer.parseInt(s1);
                        }
                    });
                    s.setTextValidator(consumes -> TextValidators.INTEGER_TEXT_VALIDATOR.get(consumes.getText()));
                    Dialogs.openGeneric(s,
                            Component.translatable("fancymenu.editor.layout.index"),
                            ContextMenu.IconFactory.getIcon("text"), TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT);
                    s.setText("" + editor.layout.layoutIndex);
                    menu1.closeMenuChain();
                }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.layout.index.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("stack"));

        menu.addSeparatorEntry("separator_after_layout_index");

        menu.addValueCycleEntry("random_mode", CommonCycles.cycleEnabledDisabled("fancymenu.fancymenu.editor.layoutoptions.randommode", editor.layout.randomMode).addCycleListener(cycle -> {
                    editor.history.saveSnapshot();
                    editor.layout.randomMode = cycle.getAsBoolean();
                })).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("random"));

        menu.addClickableEntry("random_mode_group", Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"), (menu1, entry) -> {
                    TextInputWindowBody s = new TextInputWindowBody(CharacterFilter.buildIntegerFilter(), call -> {
                        if (call != null) {
                            if (!MathUtils.isInteger(call)) {
                                call = "1";
                            }
                            editor.history.saveSnapshot();
                            editor.layout.randomGroup = call;
                        }
                    });
                    Dialogs.openGeneric(s,
                            Component.translatable("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup"),
                            ContextMenu.IconFactory.getIcon("text"), TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT);
                    s.setText(editor.layout.randomGroup);
                    menu1.closeMenuChain();
                }).addIsActiveSupplier((menu1, entry) -> editor.layout.randomMode)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.setgroup.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("group"));

        menu.addValueCycleEntry("random_mode_first_time", CommonCycles.cycleEnabledDisabled("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime", editor.layout.randomOnlyFirstTime).addCycleListener(cycle -> {
                    editor.history.saveSnapshot();
                    editor.layout.randomOnlyFirstTime = cycle.getAsBoolean();
                })).addIsActiveSupplier((menu1, entry) -> editor.layout.randomMode)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.fancymenu.editor.layoutoptions.randommode.onlyfirsttime.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("random_once"));

        menu.addSeparatorEntry("separator_after_random_mode_first_time");

        menu.addValueCycleEntry("render_custom_elements_behind_vanilla", CommonCycles.cycleEnabledDisabled("fancymenu.editor.render_custom_behind_vanilla", editor.layout.renderElementsBehindVanilla).addCycleListener(cycle -> {
            editor.history.saveSnapshot();
            editor.layout.renderElementsBehindVanilla = cycle.getAsBoolean();
        })).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.render_custom_behind_vanilla.desc")));

        menu.addSeparatorEntry("separator_after_render_custom_behind_vanilla");

        LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> cycleAutoScaling = CommonCycles.cycleEnabledDisabled("fancymenu.helper.editor.properties.autoscale", ((editor.layout.autoScalingWidth != 0) && (editor.layout.autoScalingHeight != 0)));
        cycleAutoScaling.addCycleListener(cycle -> {
            if (cycle.getAsBoolean()) {
                menu.closeMenu();
                this.openContextMenuScreen(new AutoScalingScreen(editor, call -> {
                    if (!call) {
                        cycleAutoScaling.setCurrentValue(CommonCycles.CycleEnabledDisabled.DISABLED, false);
                    }
                    this.openContextMenuScreen(editor);
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
                .addIsActiveSupplier((menu1, entry) -> (editor.layout.forcedScale != 0))
                .setTooltipSupplier((menu1, entry) -> {
                    if (entry.isActive()) {
                        return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.autoscale.desc"));
                    }
                    return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.autoscale.forced_scale_needed"));
                })
                .setIcon(ContextMenu.IconFactory.getIcon("measure"));

        this.addGenericIntegerInputContextMenuEntryTo(menu, "forced_gui_scale",
                        null,
                        consumes -> (int) editor.layout.forcedScale,
                        (consumes, integer) -> {
                            editor.layout.forcedScale = integer;
                            if (integer == 0) {
                                editor.layout.autoScalingWidth = 0;
                                editor.layout.autoScalingHeight = 0;
                            }
                            ScreenCustomization.reInitCurrentScreen();
                        },
                        Component.translatable("fancymenu.editor.rightclick.scale"),
                        true, 0, consumes -> {
                            if (MathUtils.isInteger(consumes)) {
                                return Integer.parseInt(consumes) >= 0;
                            }
                            return false;
                        }, consumes -> {
                            if (MathUtils.isInteger(consumes)) {
                                if (Integer.parseInt(consumes) < 0) {
                                    return UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.rightclick.scale.invalid"));
                                }
                            }
                            return null;
                        }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.properties.scale.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("measure"))
                .addIsActiveSupplier((menu1, entry) -> editor.layout.autoScalingWidth == 0)
                .setTooltipSupplier((menu1, entry) -> entry.isActive() ? null : UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.auto_scaling.disable_forced_scale_first")));

        menu.addSeparatorEntry("separator_after_forced_scale");

        this.addAudioResourceChooserContextMenuEntryTo(menu, "open_audio",
                        layoutEditorFilter,
                        null,
                        consumes -> editor.layout.openAudio,
                        (consumes, value) -> editor.layout.openAudio = value,
                        Component.translatable("fancymenu.editor.open_audio"),
                        true, null, true, true, true)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.open_audio.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(menu, "close_audio",
                        layoutEditorFilter,
                        null,
                        consumes -> editor.layout.closeAudio,
                        (consumes, value) -> editor.layout.closeAudio = value,
                        Component.translatable("fancymenu.editor.close_audio"),
                        true, null, true, true, true)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.close_audio.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        menu.addSeparatorEntry("separator_after_close_audio");

        menu.addClickableEntry("layout_wide_requirements", Component.translatable("fancymenu.requirements.layouts.loading_requirements"), (menu1, entry) -> {
                    ManageRequirementsScreen s = new ManageRequirementsScreen(editor.layout.layoutWideRequirementContainer.copy(false), (call) -> {
                        if (call != null) {
                            editor.layout.layoutWideRequirementContainer = call;
                        }
                    });
                    menu1.closeMenuChain();
                    ManageRequirementsScreen.openInWindow(s);
                }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.layouts.loading_requirements.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("check_list"));

        menu.addSeparatorEntry("separator_after_layout_wide_requirements");

        menu.addClickableEntry("manage_open_screen_actions", Component.translatable("fancymenu.layout.editor.edit_open_screen_action_script"), (menu1, entry) -> {
                    ActionScriptEditorWindowBody s = new ActionScriptEditorWindowBody(editor.layout.openScreenExecutableBlocks.isEmpty() ? new GenericExecutableBlock() : editor.layout.openScreenExecutableBlocks.getFirst().copy(false), (call) -> {
                        if (call != null) {
                            editor.history.saveSnapshot();
                            editor.layout.openScreenExecutableBlocks.clear();
                            editor.layout.openScreenExecutableBlocks.add(call);
                        }
                    });
                    menu1.closeMenuChain();
                    ActionScriptEditorWindowBody.openInWindow(s);
                }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.layout.editor.edit_open_screen_action_script.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        menu.addClickableEntry("manage_close_screen_actions", Component.translatable("fancymenu.layout.editor.edit_close_screen_action_script"), (menu1, entry) -> {
                    ActionScriptEditorWindowBody s = new ActionScriptEditorWindowBody(editor.layout.closeScreenExecutableBlocks.isEmpty() ? new GenericExecutableBlock() : editor.layout.closeScreenExecutableBlocks.getFirst().copy(false), (call) -> {
                        if (call != null) {
                            editor.history.saveSnapshot();
                            editor.layout.closeScreenExecutableBlocks.clear();
                            editor.layout.closeScreenExecutableBlocks.add(call);
                        }
                    });
                    menu1.closeMenuChain();
                    ActionScriptEditorWindowBody.openInWindow(s);
                }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.layout.editor.edit_close_screen_action_script.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        menu.addSeparatorEntry("separator_after_manage_actions");

        menu.addClickableEntry("paste_elements", Component.translatable("fancymenu.editor.edit.paste"), (menu1, entry) -> {
                    editor.history.saveSnapshot();
                    editor.pasteElementsFromClipboard();
                }).addIsActiveSupplier((menu1, entry) -> !LayoutEditorScreen.COPIED_ELEMENTS_CLIPBOARD.isEmpty())
                .setIcon(ContextMenu.IconFactory.getIcon("paste"));

        menu.addSeparatorEntry("separator_after_paste_elements");

        menu.addSubMenuEntry("add_element", Component.translatable("fancymenu.editor.layout.settings.new_element"), buildElementContextMenu())
                .setIcon(ContextMenu.IconFactory.getIcon("add"));

        return menu;

    }

    public void buildMenuBackgroundsMenuAndAddTo(@NotNull ContextMenu addTo) {

        ContextMenu backgroundsMenu = new ContextMenu();
        addTo.addSubMenuEntry("menu_backgrounds", Component.translatable("fancymenu.backgrounds.general.backgrounds"), backgroundsMenu)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));
        // The backgrounds list always holds exactly one instance of each background type
        editor.layout.menuBackgrounds.forEach(background -> {
            var entry = backgroundsMenu.addSubMenuEntry("menu_background_" + background.builder.getIdentifier(), background.builder.getDisplayName(), background._initConfigMenu(editor));
            var desc = background.builder.getDescription();
            if (desc != null) entry.setTooltipSupplier((menu1, entry1) -> UITooltip.of(desc));
        });

        backgroundsMenu.addSeparatorEntry("separator_after_background_types");

        backgroundsMenu.addValueCycleEntry("keep_background_aspect_ratio", CommonCycles.cycleEnabledDisabled("fancymenu.helper.editor.layoutoptions.backgroundoptions.keepaspect", editor.layout.preserveBackgroundAspectRatio).addCycleListener(cycle -> {
            editor.history.saveSnapshot();
            editor.layout.preserveBackgroundAspectRatio = cycle.getAsBoolean();
        })).setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"));

        backgroundsMenu.addValueCycleEntry("show_overlay_on_custom_background", CommonCycles.cycleEnabledDisabled("fancymenu.editor.background.show_overlay_on_custom_background", editor.layout.showScreenBackgroundOverlayOnCustomBackground).addCycleListener(cycle -> {
            editor.history.saveSnapshot();
            editor.layout.showScreenBackgroundOverlayOnCustomBackground = cycle.getAsBoolean();
        })).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.background.show_overlay_on_custom_background.desc")));

        backgroundsMenu.addValueCycleEntry("apply_vanilla_background_blur", CommonCycles.cycleEnabledDisabled("fancymenu.editor.background.blur_background", editor.layout.applyVanillaBackgroundBlur).addCycleListener(cycle -> {
            editor.history.saveSnapshot();
            editor.layout.applyVanillaBackgroundBlur = cycle.getAsBoolean();
        })).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.background.blur_background.desc")));

    }

    @NotNull
    public ContextMenu buildDecorationOverlaysMenu() {

        ContextMenu menu = new ContextMenu();

        // Normal layouts always have one instance of each overlay type, so doing this is fine
        editor.layout.decorationOverlays.forEach(pair -> {
            var entry = menu.addSubMenuEntry("overlay_" + pair.getFirst().getIdentifier(), pair.getFirst().getDisplayName(), pair.getSecond()._initConfigMenu(editor));
            var desc = pair.getFirst().getDescription();
            if (desc != null) entry.setTooltipSupplier((menu1, entry1) -> UITooltip.of(desc));
        });

        return menu;

    }

    @NotNull
    public ContextMenu buildScrollListCustomizationsContextMenu() {

        ContextMenu menu = new ContextMenu();
        ConsumingSupplier<LayoutEditorUI, Boolean> layoutEditorFilter = consumes -> true;

        this.addImageResourceChooserContextMenuEntryTo(menu, "header_texture",
                        layoutEditorFilter,
                        null,
                        consumes -> editor.layout.scrollListHeaderTexture,
                        (consumes, value) -> editor.layout.scrollListHeaderTexture = value,
                        Component.translatable("fancymenu.customization.scroll_lists.header_texture"),
                        true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));

        this.addImageResourceChooserContextMenuEntryTo(menu, "footer_texture",
                        layoutEditorFilter,
                        null,
                        consumes -> editor.layout.scrollListFooterTexture,
                        (consumes, value) -> editor.layout.scrollListFooterTexture = value,
                        Component.translatable("fancymenu.customization.scroll_lists.footer_texture"),
                        true, null, true, true, true)
                .setIcon(ContextMenu.IconFactory.getIcon("image"));

        menu.addSeparatorEntry("separator_after_footer_texture");

        menu.addValueCycleEntry("repeat_header_texture", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.repeat_header", editor.layout.repeatScrollListHeaderTexture).addCycleListener(cycleEnabledDisabled -> {
            editor.history.saveSnapshot();
            editor.layout.repeatScrollListHeaderTexture = cycleEnabledDisabled.getAsBoolean();
        })).addIsActiveSupplier((menu1, entry) -> !editor.layout.preserveScrollListHeaderFooterAspectRatio);

        menu.addValueCycleEntry("repeat_footer_texture", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.repeat_footer", editor.layout.repeatScrollListFooterTexture).addCycleListener(cycleEnabledDisabled -> {
            editor.history.saveSnapshot();
            editor.layout.repeatScrollListFooterTexture = cycleEnabledDisabled.getAsBoolean();
        })).addIsActiveSupplier((menu1, entry) -> !editor.layout.preserveScrollListHeaderFooterAspectRatio);

        menu.addSeparatorEntry("separator_after_header_footer_repeat_texture");

        menu.addValueCycleEntry("preserve_header_footer_aspect_ratio", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.preserve_header_footer_aspect_ratio", editor.layout.preserveScrollListHeaderFooterAspectRatio).addCycleListener(cycle -> {
                    editor.history.saveSnapshot();
                    editor.layout.preserveScrollListHeaderFooterAspectRatio = cycle.getAsBoolean();
                })).setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"))
                .addIsActiveSupplier((menu1, entry) -> (!editor.layout.repeatScrollListHeaderTexture && !editor.layout.repeatScrollListFooterTexture));

        menu.addSeparatorEntry("separator_after_preserve_aspect_ratio");

        menu.addValueCycleEntry("show_header_footer_preview", CommonCycles.cycleEnabledDisabled("fancymenu.customization.scroll_lists.show_preview", editor.layout.showScrollListHeaderFooterPreviewInEditor).addCycleListener(cycle -> {
            editor.history.saveSnapshot();
            editor.layout.showScrollListHeaderFooterPreviewInEditor = cycle.getAsBoolean();
        })).setIcon(ContextMenu.IconFactory.getIcon("eye"));

        return menu;

    }

    @NotNull
    public ContextMenu buildElementContextMenu() {

        ContextMenu menu = new ContextMenu();

        int i = 0;
        for (ElementBuilder<?,?> builder : ElementRegistry.getBuilders()) {
            if ((LayoutEditorScreen.getCurrentInstance() != null) && !builder.shouldShowUpInEditorElementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
            if (!builder.isDeprecated()) {
                ContextMenu.ClickableContextMenuEntry<?> entry = menu.addClickableEntry("element_" + i, builder.getDisplayName(null), (menu1, entry1) -> {
                    AbstractEditorElement<?,?> editorElement = builder.wrapIntoEditorElementInternal(builder.buildDefaultInstance(), editor);
                    if (editorElement != null) {
                        editorElement.element.afterConstruction();
                        editor.history.saveSnapshot();
                        editor.normalEditorElements.add(editorElement);
                        if ((editor.rightClickMenuOpenPosX != -1000) && (editor.rightClickMenuOpenPosY != -1000)) {
                            //Add new element at right-click menu coordinates
                            editorElement.setAnchorPoint(editorElement.element.anchorPoint, editor.rightClickMenuOpenPosX, editor.rightClickMenuOpenPosY, true);
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
                    entry.setTooltipSupplier((menu1, entry1) -> UITooltip.of(desc));
                }
                i++;
            }
        }

        return menu;

    }

    @NotNull
    public ContextMenu buildHiddenVanillaElementsContextMenu() {

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

                int i = 0;
                for (VanillaWidgetEditorElement e : hiddenVanillaButtons) {
                    AbstractWidget w = e.element.getWidget();
                    this.addClickableEntry("element_" + i, (w != null) ? w.getMessage() : Component.empty(), (menu1, entry) -> {
                        editor.history.saveSnapshot();
                        e.setHidden(false);
                        MainThreadTaskExecutor.executeInMainThread(() -> menu1.removeEntry(entry.getIdentifier()), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                    }).setTooltipSupplier((menu1, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.hidden_vanilla_elements.element.desc")));
                    i++;
                }

                return super.openMenuAt(x, y);

            }

        };

    }

    public ContextMenu buildOpenLayoutContextMenu() {

        ContextMenu menu = new ContextMenu();

        if (editor.layout.isUniversalLayout()) {

            List<Layout> allLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true);
            int allLayoutsCount = allLayouts.size();
            int i = 0;
            for (Layout l : LayoutHandler.sortLayoutListByLastEdited(allLayouts, true, 8)) {
                if (l.getLayoutName().equals(editor.layout.getLayoutName())) continue; //Don't show the current layout in the list
                menu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(l))
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
							editor.layout.decorationOverlays.forEach(pair -> pair.getSecond().onCloseScreen(null, null));
							this.openContextMenuScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), editor.layoutTargetScreen, layouts -> {
								this.openContextMenuScreen(editor);
							}));
						}
                    });
                });
            }

            menu.addSeparatorEntry("separator_after_recent_layouts");

            menu.addClickableEntry("all_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu1, entry) -> {
                displayUnsavedWarning(call -> {
                    if (call) {
                        editor.saveWidgetSettings();
                        this.openContextMenuScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(Layout.UNIVERSAL_LAYOUT_IDENTIFIER, true), editor.layoutTargetScreen, layouts -> {
                            this.openContextMenuScreen(editor);
                        }));
                    }
                });
            });

        } else if (editor.layout.screenIdentifier != null) {

            List<Layout> allLayouts = LayoutHandler.getAllLayoutsForScreenIdentifier(editor.layout.screenIdentifier, false);
            int allLayoutsCount = allLayouts.size();
            int i = 0;
            for (Layout l : LayoutHandler.sortLayoutListByLastEdited(allLayouts, true, 8)) {
                if (l.getLayoutName().equals(editor.layout.getLayoutName())) continue; //Don't show the current layout in the list
                menu.addSubMenuEntry("layout_" + i, Component.empty(), buildManageLayoutSubMenu(l))
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
                            this.openContextMenuScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(editor.layout.screenIdentifier, false), editor.layoutTargetScreen, layouts -> {
                                this.openContextMenuScreen(editor);
                            }));
                        }
                    });
                });
            }

            menu.addSeparatorEntry("separator_after_recent_layouts");

            menu.addClickableEntry("all_layouts", Component.translatable("fancymenu.overlay.menu_bar.customization.layout.manage.all"), (menu1, entry) -> {
                displayUnsavedWarning(call -> {
                    if (call) {
                        editor.saveWidgetSettings();
                        this.openContextMenuScreen(new ManageLayoutsScreen(LayoutHandler.getAllLayoutsForScreenIdentifier(editor.layout.screenIdentifier, false), editor.layoutTargetScreen, layouts -> {
                            this.openContextMenuScreen(editor);
                        }));
                    }
                });
            });

        }

        return menu;

    }

    @NotNull
    protected ContextMenu buildManageLayoutSubMenu(Layout layout) {

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

    @Override
    public @NotNull List<ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
        return this.contextMenuScreenOpenProcessors;
    }

    @Override
    public @Nullable Screen getContextMenuCallbackScreen() {
        return this.editor;
    }

    @Override
    public @NotNull LayoutEditorUI self() {
        return this;
    }

    @Override
    public void saveSnapshot() {
        this.editor.history.saveSnapshot();
    }

    @Override
    public void saveSnapshot(@NotNull Object snapshot) {
        this.editor.history.saveSnapshot((LayoutEditorHistory.Snapshot)snapshot);
    }

    @Override
    public @Nullable Object createSnapshot() {
        return this.editor.history.createSnapshot();
    }

    @Override
    public @NotNull List<LayoutEditorUI> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<LayoutEditorUI, Boolean> filter) {
        return List.of(this);
    }

}
