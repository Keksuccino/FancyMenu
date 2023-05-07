package de.keksuccino.fancymenu.customization.element.elements.slider;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SliderEditorElement extends AbstractEditorElement {

    public SliderEditorElement(SliderElementBuilder parentContainer, SliderElement customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        super.init();

        SliderElement i = ((SliderElement)this.element);

        AdvancedButton setVariableButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_variable"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.set_variable"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if ((i.linkedVariable == null) || (!i.linkedVariable.equals(call))) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.linkedVariable = call;
                    } else {
                        if (i.linkedVariable != null) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.linkedVariable = null;
                    }
                }
            });
            if (i.linkedVariable != null) {
                p.setText(i.linkedVariable);
            }
            PopupHandler.displayPopup(p);
        });
        setVariableButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.set_variable.desc"), "%n%"));
        this.rightClickContextMenu.addContent(setVariableButton);

        ContextMenu setTypeMenu = new ContextMenu();
        this.rightClickContextMenu.addChild(setTypeMenu);

        AdvancedButton setTypeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_type"), true, (press) -> {
            setTypeMenu.setParentButton((AdvancedButton) press);
            setTypeMenu.openMenuAt(0, press.y);
        });
        setTypeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.set_type.desc"), "%n%"));
        this.rightClickContextMenu.addContent(setTypeButton);
        for (SliderElement.SliderType t : SliderElement.SliderType.values()) {
            AdvancedButton typeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.type." + t.getName()), (press) -> {
                if (i.type != t) {
                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                }
                i.type = t;
                i.initializeSlider();
                rightClickContextMenu.closeMenu();
                this.init();
            }) {
                @Override
                public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                    if (i.type == t) {
                        this.setMessage("§a" + Locals.localize("fancymenu.customization.items.slider.type." + t.getName()));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.customization.items.slider.type." + t.getName()));
                    }
                    super.render(p_93657_, p_93658_, p_93659_, p_93660_);
                }
            };
            setTypeMenu.addContent(typeButton);
        }

        AdvancedButton setLabelPrefixButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_label_prefix"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.set_label_prefix"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if ((i.labelPrefix == null) || (!i.labelPrefix.equals(call))) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.labelPrefix = call;
                    } else {
                        if (i.labelPrefix != null) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.labelPrefix = null;
                    }
                    i.initializeSlider();
                }
            });
            if (i.labelPrefix != null) {
                p.setText(i.labelPrefix);
            }
            PopupHandler.displayPopup(p);
        });
        setLabelPrefixButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.set_label_prefix.desc"), "%n%"));
        this.rightClickContextMenu.addContent(setLabelPrefixButton);

        AdvancedButton setLabelSuffixButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_label_suffix"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.set_label_suffix"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if ((i.labelSuffix == null) || (!i.labelSuffix.equals(call))) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.labelSuffix = call;
                    } else {
                        if (i.labelSuffix != null) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.labelSuffix = null;
                    }
                    i.initializeSlider();
                }
            });
            if (i.labelSuffix != null) {
                p.setText(i.labelSuffix);
            }
            PopupHandler.displayPopup(p);
        });
        setLabelSuffixButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.set_label_suffix.desc"), "%n%"));
        this.rightClickContextMenu.addContent(setLabelSuffixButton);

        this.rightClickContextMenu.addSeparator();

        if (i.type == SliderElement.SliderType.RANGE) {

            AdvancedButton setMinRangeValueButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.range.set_min_range_value"), (press) -> {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.range.set_min_range_value"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                    if (call != null) {
                        if (!call.replace(" ", "").equals("")) {
                            if (MathUtils.isInteger(call)) {
                                int min = Integer.parseInt(call);
                                if (i.minRangeValue != min) {
                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                                }
                                i.minRangeValue = min;
                            }
                        } else {
                            if (i.minRangeValue != 1) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            }
                            i.minRangeValue = 1;
                        }
                        i.initializeSlider();
                    }
                });
                p.setText("" + i.minRangeValue);
                PopupHandler.displayPopup(p);
            });
            this.rightClickContextMenu.addContent(setMinRangeValueButton);

            AdvancedButton setMaxRangeValueButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.range.set_max_range_value"), (press) -> {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.range.set_max_range_value"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                    if (call != null) {
                        if (!call.replace(" ", "").equals("")) {
                            if (MathUtils.isInteger(call)) {
                                int min = Integer.parseInt(call);
                                if (i.maxRangeValue != min) {
                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                                }
                                i.maxRangeValue = min;
                            }
                        } else {
                            if (i.maxRangeValue != 1) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            }
                            i.maxRangeValue = 1;
                        }
                        i.initializeSlider();
                    }
                });
                p.setText("" + i.maxRangeValue);
                PopupHandler.displayPopup(p);
            });
            this.rightClickContextMenu.addContent(setMaxRangeValueButton);

        }
        if (i.type == SliderElement.SliderType.LIST) {

            AdvancedButton setListValuesButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values"), (press) -> {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values"), null, 240, (call) -> {
                    if (call != null) {
                        if (!call.replace(" ", "").equals("")) {
                            List<String> newValues = SliderElement.deserializeValuesList(call);
                            if (newValues.size() >= 2) {
                                if (!SliderElement.serializeValuesList(i.listValues).equals(call)) {
                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                                }
                                i.listValues = SliderElement.deserializeValuesList(call);
                            } else {
                                FMNotificationPopup p2 = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values.error.not_enough_values"), "%n%"));
                                PopupHandler.displayPopup(p2);
                            }
                        } else {
                            List<String> l = new ArrayList<>();
                            l.add("some_value");
                            l.add("another_value");
                            l.add("yet_another_value");
                            if (!SliderElement.serializeValuesList(l).equals(SliderElement.serializeValuesList(i.listValues))) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            }
                            i.listValues = l;
                        }
                        i.initializeSlider();
                    }
                });
                p.setText(SliderElement.serializeValuesList(i.listValues));
                PopupHandler.displayPopup(p);
            });
            setListValuesButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values.desc"), "%n%"));
            this.rightClickContextMenu.addContent(setListValuesButton);

        }

    }

    @Override
    public SerializedElement serializeItem() {

        SliderElement i = ((SliderElement)this.element);

        SerializedElement sec = new SerializedElement();

        if (i.linkedVariable != null) {
            sec.addEntry("linked_variable", i.linkedVariable);
        }
        sec.addEntry("slider_type", i.type.getName());
        if (i.labelPrefix != null) {
            sec.addEntry("label_prefix", i.labelPrefix);
        }
        if (i.labelSuffix != null) {
            sec.addEntry("label_suffix", i.labelSuffix);
        }
        if (i.type == SliderElement.SliderType.RANGE) {
            sec.addEntry("min_range_value", "" + i.minRangeValue);
            sec.addEntry("max_range_value", "" + i.maxRangeValue);
        }
        if (i.type == SliderElement.SliderType.LIST) {
            sec.addEntry("list_values", "" + SliderElement.serializeValuesList(i.listValues));
        }

        return sec;

    }

}
