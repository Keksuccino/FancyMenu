package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.ResourceFile;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;

public class SliderEditorElement extends AbstractEditorElement {

    public SliderEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "slider_type",
                        Arrays.asList(SliderElement.SliderType.values()),
                        SliderEditorElement.class,
                        consumes -> ((SliderElement)consumes.element).type,
                        (sliderEditorElement, sliderType) -> {
                            ((SliderElement)sliderEditorElement.element).type = sliderType;
                            ((SliderElement)sliderEditorElement.element).buildSlider();
                        },
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setIcon(ContextMenu.IconFactory.getIcon("script"));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_list_values",
                        SliderEditorElement.class,
                        consumes -> {
                            List<String> values = ((SliderElement)consumes.element).listValues;
                            String s = "";
                            for (String v : values) {
                                if (s.length() > 0) s += "\n";
                                s += v;
                            }
                            return s;
                        }, (element1, s) -> {
                            if (s != null) {
                                ((SliderElement)element1.element).listValues = Arrays.asList(StringUtils.splitLines(s, "\n"));
                                ((SliderElement)element1.element).buildSlider();
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
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.list.set_list_values.desc")))
                .setIsVisibleSupplier((menu, entry) -> ((SliderElement)this.element).type == SliderElement.SliderType.LIST)
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        this.addDoubleInputContextMenuEntryTo(this.rightClickMenu, "set_min_range_value",
                        SliderEditorElement.class,
                        consumes -> ((SliderElement)consumes.element).minRangeValue,
                        (element, range) -> {
                            ((SliderElement)element.element).minRangeValue = range;
                            ((SliderElement)element.element).buildSlider();
                        },
                        Component.translatable("fancymenu.elements.slider.v2.type.range.set_min"),
                        false, 0, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.range.set_min")))
                .setIsVisibleSupplier((menu, entry) -> (((SliderElement)this.element).type == SliderElement.SliderType.DECIMAL_RANGE) || (((SliderElement)this.element).type == SliderElement.SliderType.INTEGER_RANGE))
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        this.addDoubleInputContextMenuEntryTo(this.rightClickMenu, "set_max_range_value",
                        SliderEditorElement.class,
                        consumes -> ((SliderElement)consumes.element).maxRangeValue,
                        (element, range) -> {
                            ((SliderElement)element.element).maxRangeValue = range;
                            ((SliderElement)element.element).buildSlider();
                        },
                        Component.translatable("fancymenu.elements.slider.v2.type.range.set_max"),
                        false, 0, null, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.type.range.set_max")))
                .setIsVisibleSupplier((menu, entry) -> (((SliderElement)this.element).type == SliderElement.SliderType.DECIMAL_RANGE) || (((SliderElement)this.element).type == SliderElement.SliderType.INTEGER_RANGE))
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_pre_selected_value",
                        SliderEditorElement.class,
                        consumes -> ((SliderElement)consumes.element).preSelectedValue,
                        (sliderEditorElement, s) -> ((SliderElement)sliderEditorElement.element).preSelectedValue = s,
                        null, false, true, Component.translatable("fancymenu.elements.slider.v2.pre_selected"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.pre_selected.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("edit"));

        this.rightClickMenu.addSeparatorEntry("separator_after_set_pre_selected_value");

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"),
                        (menu, entry) -> {
                            ManageActionsScreen s = new ManageActionsScreen(((SliderElement)this.element).getExecutableBlock(), call -> {
                                if (call != null) {
                                    this.editor.history.saveSnapshot();
                                    ((SliderElement)this.element).executableBlock = call;
                                }
                                Minecraft.getInstance().setScreen(this.editor);
                            });
                            Minecraft.getInstance().setScreen(s);
                        })
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("separator_after_actions");

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("button_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                .setStackable(true);

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        ContextMenu normalBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_normal_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal.alternate.slider"), normalBackMenu)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(normalBackMenu, "normal_background_texture", 
                        SliderEditorElement.class,
                        null,
                        consumes -> (((SliderElement)consumes.element).handleTextureNormal != null) ? ((SliderElement)consumes.element).handleTextureNormal.getShortPath() : null,
                        (element1, s) -> {
                            ((SliderElement)element1.element).handleTextureNormal = (s != null) ? ResourceFile.of(s) : null;
                            ((SliderElement)element1.element).handleAnimationNormal = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        normalBackMenu.addClickableEntry("normal_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((SliderElement)consumes.element).handleAnimationNormal);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((SliderElement)e.element).handleAnimationNormal = call;
                        ((SliderElement)e.element).handleTextureNormal = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        normalBackMenu.addSeparatorEntry("separator_1").setStackable(true);

        normalBackMenu.addClickableEntry("reset_normal_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((SliderElement)e.element).handleTextureNormal = null;
                ((SliderElement)e.element).handleAnimationNormal = null;
            }
        }).setStackable(true);

        ContextMenu hoverBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_hover_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover.alternate.slider"), hoverBackMenu)
                .setStackable(true);

        this.addGenericFileChooserContextMenuEntryTo(hoverBackMenu, "hover_background_texture",
                        consumes -> (consumes instanceof SliderEditorElement),
                        null,
                        consumes -> (((SliderElement)consumes.element).handleTextureHover != null) ? ((SliderElement)consumes.element).handleTextureHover.getShortPath() : null,
                        (element1, s) -> {
                            ((SliderElement)element1.element).handleTextureHover = (s != null) ? ResourceFile.of(s) : null;
                            ((SliderElement)element1.element).handleAnimationHover = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        hoverBackMenu.addClickableEntry("hover_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((SliderElement)consumes.element).handleAnimationHover);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((SliderElement)e.element).handleAnimationHover = call;
                        ((SliderElement)e.element).handleTextureHover = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        hoverBackMenu.addSeparatorEntry("separator_1").setStackable(true);

        hoverBackMenu.addClickableEntry("reset_hover_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((SliderElement)e.element).handleTextureHover = null;
                ((SliderElement)e.element).handleAnimationHover = null;
            }
        }).setStackable(true);

        ContextMenu inactiveBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_inactive_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive.alternate.slider"), inactiveBackMenu)
                .setStackable(true);

        this.addGenericFileChooserContextMenuEntryTo(inactiveBackMenu, "inactive_background_texture",
                        consumes -> (consumes instanceof SliderEditorElement),
                        null,
                        consumes -> (((SliderElement)consumes.element).handleTextureInactive != null) ? ((SliderElement)consumes.element).handleTextureInactive.getShortPath() : null,
                        (element1, s) -> {
                            ((SliderElement)element1.element).handleTextureInactive = (s != null) ? ResourceFile.of(s) : null;
                            ((SliderElement)element1.element).handleAnimationInactive = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        inactiveBackMenu.addClickableEntry("inactive_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"),
                (menu, entry) -> {
                    List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
                    String preSelectedAnimation = null;
                    List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((SliderElement)consumes.element).handleAnimationInactive);
                    if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                        preSelectedAnimation = allAnimations.get(0);
                    }
                    ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                        if (call != null) {
                            this.editor.history.saveSnapshot();
                            for (AbstractEditorElement e : selectedElements) {
                                ((SliderElement)e.element).handleAnimationInactive = call;
                                ((SliderElement)e.element).handleTextureInactive = null;
                            }
                        }
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(s);
                }).setStackable(true);

        inactiveBackMenu.addSeparatorEntry("separator_after_inactive_back_animation").setStackable(true);

        inactiveBackMenu.addClickableEntry("reset_inactive_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"),
                (menu, entry) -> {
                    this.editor.history.saveSnapshot();
                    List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
                    for (AbstractEditorElement e : selectedElements) {
                        ((SliderElement)e.element).handleTextureInactive = null;
                        ((SliderElement)e.element).handleAnimationInactive = null;
                    }
                }).setStackable(true);

        setBackMenu.addSeparatorEntry("separator_before_slider_background_entries");

        ContextMenu normalSliderBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_normal_slider_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.slider.normal"), normalSliderBackMenu)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(normalSliderBackMenu, "normal_slider_background_texture",
                        SliderEditorElement.class,
                        null,
                        consumes -> (((SliderElement)consumes.element).sliderBackgroundTextureNormal != null) ? ((SliderElement)consumes.element).sliderBackgroundTextureNormal.getShortPath() : null,
                        (element1, s) -> {
                            ((SliderElement)element1.element).sliderBackgroundTextureNormal = (s != null) ? ResourceFile.of(s) : null;
                            ((SliderElement)element1.element).sliderBackgroundAnimationNormal = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        normalSliderBackMenu.addClickableEntry("normal_slider_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((SliderElement)consumes.element).sliderBackgroundAnimationNormal);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((SliderElement)e.element).sliderBackgroundAnimationNormal = call;
                        ((SliderElement)e.element).sliderBackgroundTextureNormal = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        normalSliderBackMenu.addSeparatorEntry("separator_1").setStackable(true);

        normalSliderBackMenu.addClickableEntry("reset_normal_slider_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((SliderElement)e.element).sliderBackgroundTextureNormal = null;
                ((SliderElement)e.element).sliderBackgroundAnimationNormal = null;
            }
        }).setStackable(true);

        ContextMenu highlightedSliderBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_highlighted_slider_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.slider.highlighted"), highlightedSliderBackMenu)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.buttons.buttonbackground.slider.highlighted.desc")))
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(highlightedSliderBackMenu, "highlighted_slider_background_texture",
                        SliderEditorElement.class,
                        null,
                        consumes -> (((SliderElement)consumes.element).sliderBackgroundTextureHighlighted != null) ? ((SliderElement)consumes.element).sliderBackgroundTextureHighlighted.getShortPath() : null,
                        (element1, s) -> {
                            ((SliderElement)element1.element).sliderBackgroundTextureHighlighted = (s != null) ? ResourceFile.of(s) : null;
                            ((SliderElement)element1.element).sliderBackgroundAnimationHighlighted = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        highlightedSliderBackMenu.addClickableEntry("highlighted_slider_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((SliderElement)consumes.element).sliderBackgroundAnimationHighlighted);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((SliderElement)e.element).sliderBackgroundAnimationHighlighted = call;
                        ((SliderElement)e.element).sliderBackgroundTextureHighlighted = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        highlightedSliderBackMenu.addSeparatorEntry("separator_1").setStackable(true);

        highlightedSliderBackMenu.addClickableEntry("reset_highlighted_slider_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SliderEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((SliderElement)e.element).sliderBackgroundTextureHighlighted = null;
                ((SliderElement)e.element).sliderBackgroundAnimationHighlighted = null;
            }
        }).setStackable(true);

        buttonBackgroundMenu.addSeparatorEntry("separator_1").setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(buttonBackgroundMenu, "loop_animation",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).loopBackgroundAnimations,
                        (element1, s) -> ((SliderElement)element1.element).loopBackgroundAnimations = s,
                        "fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation")
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(buttonBackgroundMenu, "restart_animation_on_hover",
                        consumes -> (consumes instanceof SliderEditorElement),
                        consumes -> ((SliderElement)consumes.element).restartBackgroundAnimationsOnHover,
                        (element1, s) -> ((SliderElement)element1.element).restartBackgroundAnimationsOnHover = s,
                        "fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_after_texture_settings").setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_label",
                        SliderEditorElement.class,
                        consumes -> ((SliderElement)consumes.element).label,
                        (sliderEditorElement, s) -> ((SliderElement)sliderEditorElement.element).label = s,
                        null, false, true, Component.translatable("fancymenu.elements.slider.v2.label"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.slider.v2.label.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        this.rightClickMenu.addSeparatorEntry("separator_after_set_label").setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        SliderEditorElement.class,
                        consumes -> {
                            String t = ((SliderElement)consumes.element).tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element1, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            ((SliderElement)element1.element).tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.editor.items.button.btndescription"),
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.button.btndescription.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

    }

}
