package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;

public class SliderEditorElement extends AbstractEditorElement<SliderEditorElement, SliderElement> {

    public SliderEditorElement(@NotNull SliderElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "slider_type",
                        Arrays.asList(SliderElement.SliderType.values()),
                        SliderEditorElement.class,
                        consumes -> consumes.element.type,
                        (sliderEditorElement, sliderType) -> {
                            sliderEditorElement.element.type = sliderType;
                            sliderEditorElement.element.buildSlider();
                        },
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setIcon(MaterialIcons.TUNE);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_list_values",
                        SliderEditorElement.class,
                        consumes -> {
                            List<String> values = consumes.element.listValues;
                            StringBuilder s = new StringBuilder();
                            for (String v : values) {
                                if (!s.isEmpty()) s.append("\n");
                                s.append(v);
                            }
                            return s.toString();
                        }, (element1, s) -> {
                            if (s != null) {
                                element1.element.listValues = Arrays.asList(StringUtils.splitLines(s, "\n"));
                                element1.element.buildSlider();
                            }
                        }, null, true, false,
                        Component.translatable("fancymenu.elements.slider.v2.type.list.set_list_values"),
                        false, null,
                        consumes -> {
                            //Check if there are at least two lines and both are at least one character long
                            if ((consumes != null) && consumes.contains("\n")) {
                                String[] lines = consumes.split("\n", 2);
                                return (!lines[0].isEmpty() && !lines[1].isEmpty());
                            }
                            return false;
                        }, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.list.set_list_values.desc")))
                .setIcon(MaterialIcons.FORMAT_LIST_BULLETED)
                .addIsVisibleSupplier((menu, entry) -> (this.element).type == SliderElement.SliderType.LIST);

        this.addDoubleInputContextMenuEntryTo(this.rightClickMenu, "set_min_range_value",
                        SliderEditorElement.class,
                        consumes -> consumes.element.minRangeValue,
                        (element, range) -> {
                            (element.element).minRangeValue = range;
                            (element.element).buildSlider();
                        },
                        Component.translatable("fancymenu.elements.slider.v2.type.range.set_min"),
                        false, 0, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.range.set_min.desc")))
                .setIcon(MaterialIcons.TUNE)
                .addIsVisibleSupplier((menu, entry) -> ((this.element).type == SliderElement.SliderType.DECIMAL_RANGE) || ((this.element).type == SliderElement.SliderType.INTEGER_RANGE));

        this.addDoubleInputContextMenuEntryTo(this.rightClickMenu, "set_max_range_value",
                        SliderEditorElement.class,
                        consumes -> (consumes.element).maxRangeValue,
                        (element, range) -> {
                            (element.element).maxRangeValue = range;
                            (element.element).buildSlider();
                        },
                        Component.translatable("fancymenu.elements.slider.v2.type.range.set_max"),
                        false, 0, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.range.set_max.desc")))
                .setIcon(MaterialIcons.TUNE)
                .addIsVisibleSupplier((menu, entry) -> ((this.element).type == SliderElement.SliderType.DECIMAL_RANGE) || ((this.element).type == SliderElement.SliderType.INTEGER_RANGE));

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "rounding_decimal_place",
                        SliderEditorElement.class,
                        consumes -> (consumes.element).roundingDecimalPlace,
                        (element, range) -> {
                            (element.element).roundingDecimalPlace = range;
                            (element.element).buildSlider();
                        },
                        Component.translatable("fancymenu.elements.slider.v2.type.range.decimal.round"),
                        true, 2, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.range.decimal.round.desc")))
                .setIcon(MaterialIcons.FUNCTIONS)
                .addIsVisibleSupplier((menu, entry) -> (this.element).type == SliderElement.SliderType.DECIMAL_RANGE);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_pre_selected_value",
                        SliderEditorElement.class,
                        consumes -> (consumes.element).preSelectedValue,
                        (sliderEditorElement, s) -> (sliderEditorElement.element).preSelectedValue = s,
                        null, false, true, Component.translatable("fancymenu.elements.slider.v2.pre_selected"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.pre_selected.desc")))
                .setIcon(MaterialIcons.EDIT);

        this.rightClickMenu.addSeparatorEntry("separator_after_set_pre_selected_value");

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.actions.screens.manage_screen.manage"),
                        (menu, entry) -> {
                            ActionScriptEditorWindowBody s = new ActionScriptEditorWindowBody((this.element).getExecutableBlock(), call -> {
                                if (call != null) {
                                    this.editor.history.saveSnapshot();
                                    (this.element).executableBlock = call;
                                }
                            });
                            menu.closeMenuChain();
                            ActionScriptEditorWindowBody.openInWindow(s);
                        })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.manage_actions.desc")))
                .setIcon(MaterialIcons.CODE)
                .setStackable(false);

        this.rightClickMenu.addClickableEntry("widget_active_state_controller", Component.translatable("fancymenu.elements.button.active_state_controller"), (menu, entry) -> {
                    ManageRequirementsScreen s = new ManageRequirementsScreen(this.element.activeStateSupplier.copy(false), (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            this.element.activeStateSupplier = call;
                        }
                    });
                    menu.closeMenuChain();
                    ManageRequirementsScreen.openInWindow(s);
                })
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.active_state_controller.desc")))
                .setIcon(MaterialIcons.CHECKLIST)
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_actions");

        this.addTextureSettings();

        this.rightClickMenu.addSeparatorEntry("separator_after_texture_settings").setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_label",
                        SliderEditorElement.class,
                        consumes -> (consumes.element).label,
                        (sliderEditorElement, s) -> (sliderEditorElement.element).label = s,
                        null, false, true, Component.translatable("fancymenu.elements.slider.v2.label"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.label.desc")))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "underline_label_on_hover",
                        SliderEditorElement.class,
                        consumes -> consumes.element.underlineLabelOnHover,
                        (sliderEditorElement, aBoolean) -> sliderEditorElement.element.underlineLabelOnHover = aBoolean,
                        "fancymenu.elements.widgets.label.underline_on_hover")
                .setStackable(true)
                .setIcon(MaterialIcons.FORMAT_UNDERLINED);

        this.element.labelHoverColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.element.labelBaseColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.PALETTE);

        this.element.labelScale.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.FORMAT_SIZE);

        this.rightClickMenu.addSeparatorEntry("separator_after_set_label").setStackable(true);

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        SliderEditorElement.class,
                        null,
                        consumes -> consumes.element.hoverSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.element.hoverSound = supplier,
                        Component.translatable("fancymenu.elements.button.hoversound"), true, null, true, true, true)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.element.unhoverAudio.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.VOLUME_UP);

        this.rightClickMenu.addSeparatorEntry("separator_after_hover_sound").setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        SliderEditorElement.class,
                        consumes -> {
                            String t = (consumes.element).tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element1, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            (element1.element).tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.elements.button.tooltip"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.button.tooltip.desc")))
                .setIcon(MaterialIcons.CHAT);

        this.rightClickMenu.addSeparatorEntry("separator_before_navigatable");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "toggle_navigatable", SliderEditorElement.class,
                        consumes -> consumes.element.navigatable,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.element.navigatable = aBoolean,
                        "fancymenu.elements.widgets.generic.navigatable")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.widgets.generic.navigatable.desc")))
                .setIcon(MaterialIcons.MOUSE);

    }

    protected void addTextureSettings() {

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("button_background", Component.translatable("fancymenu.elements.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                .setIcon(MaterialIcons.IMAGE)
                .setStackable(true);

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.elements.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_background_texture",
                SliderEditorElement.class,
                null,
                consumes -> consumes.element.handleTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.handleTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.normal.alternate.slider"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "hover_background_texture",
                SliderEditorElement.class,
                null,
                consumes -> consumes.element.handleTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.handleTextureHover = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.hover.alternate.slider"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "inactive_background_texture",
                SliderEditorElement.class,
                null,
                consumes -> consumes.element.handleTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.handleTextureInactive = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.inactive.alternate.slider"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        setBackMenu.addSeparatorEntry("separator_before_slider_background_entries");

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "normal_slider_background_texture",
                SliderEditorElement.class,
                null,
                consumes -> consumes.element.sliderBackgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.element.sliderBackgroundTextureNormal = iTextureResourceSupplier;
                }, Component.translatable("fancymenu.elements.buttons.buttonbackground.slider.normal"), true, null, true, true, true)
                .setIcon(MaterialIcons.IMAGE);

        this.addImageResourceChooserContextMenuEntryTo(setBackMenu, "highlighted_slider_background_texture",
                        SliderEditorElement.class,
                        null,
                        consumes -> consumes.element.sliderBackgroundTextureHighlighted,
                        (buttonEditorElement, iTextureResourceSupplier) -> {
                            buttonEditorElement.element.sliderBackgroundTextureHighlighted = iTextureResourceSupplier;
                        }, Component.translatable("fancymenu.elements.buttons.buttonbackground.slider.highlighted"), true, null, true, true, true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.buttons.buttonbackground.slider.highlighted.desc")))
                .setIcon(MaterialIcons.IMAGE);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_set_texture").setStackable(true);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "transparent_background",
                        SliderEditorElement.class,
                        consumes -> consumes.element.transparentBackground,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.element.transparentBackground = aBoolean,
                        "fancymenu.elements.buttons.textures.transparent_background")
                .setStackable(true)
                .setIcon(MaterialIcons.BACKGROUND_REPLACE);

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "restart_animated_on_hover",
                        SliderEditorElement.class,
                        consumes -> consumes.element.restartBackgroundAnimationsOnHover,
                        (buttonEditorElement, aBoolean) -> buttonEditorElement.element.restartBackgroundAnimationsOnHover = aBoolean,
                        "fancymenu.elements.buttons.textures.restart_animated_on_hover")
                .setStackable(true)
                .setIcon(MaterialIcons.REPLAY);

        buttonBackgroundMenu.addSeparatorEntry("separator_after_restart_animation_on_hover");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_background", SliderEditorElement.class,
                consumes -> consumes.element.nineSliceCustomBackground,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.nineSliceCustomBackground = aBoolean,
                "fancymenu.elements.buttons.textures.nine_slice")
                .setIcon(MaterialIcons.GRID_GUIDES);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_x", SliderEditorElement.class,
                consumes -> consumes.element.nineSliceBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceBorderX = integer,
                Component.translatable("fancymenu.elements.buttons.textures.nine_slice.border_x"), true, 5, null, null)
                .setIcon(MaterialIcons.BORDER_HORIZONTAL);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_border_y", SliderEditorElement.class,
                consumes -> consumes.element.nineSliceBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceBorderY = integer,
                Component.translatable("fancymenu.elements.buttons.textures.nine_slice.border_y"), true, 5, null, null)
                .setIcon(MaterialIcons.BORDER_VERTICAL);

        buttonBackgroundMenu.addSeparatorEntry("separator_before_nine_slider_slider_handle_settings");

        this.addToggleContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle", SliderEditorElement.class,
                consumes -> consumes.element.nineSliceSliderHandle,
                (buttonEditorElement, aBoolean) -> buttonEditorElement.element.nineSliceSliderHandle = aBoolean,
                "fancymenu.elements.slider.v2.handle.textures.nine_slice")
                .setIcon(MaterialIcons.DRAG_HANDLE);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_x", SliderEditorElement.class,
                consumes -> consumes.element.nineSliceSliderHandleBorderX,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceSliderHandleBorderX = integer,
                Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_x"), true, 5, null, null)
                .setIcon(MaterialIcons.BORDER_HORIZONTAL);

        this.addIntegerInputContextMenuEntryTo(buttonBackgroundMenu, "nine_slice_slider_handle_border_y", SliderEditorElement.class,
                consumes -> consumes.element.nineSliceSliderHandleBorderY,
                (buttonEditorElement, integer) -> buttonEditorElement.element.nineSliceSliderHandleBorderY = integer,
                Component.translatable("fancymenu.elements.slider.v2.handle.textures.nine_slice.border_y"), true, 5, null, null)
                .setIcon(MaterialIcons.BORDER_VERTICAL);

    }

}
