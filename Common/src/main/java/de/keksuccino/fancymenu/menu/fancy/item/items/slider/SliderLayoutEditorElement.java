package de.keksuccino.fancymenu.menu.fancy.item.items.slider;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SliderLayoutEditorElement extends LayoutEditorElement {

    public SliderLayoutEditorElement(SliderCustomizationItemContainer parentContainer, SliderCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        super.init();

        SliderCustomizationItem i = ((SliderCustomizationItem)this.object);

        AdvancedButton setVariableButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_variable"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.set_variable"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if ((i.linkedVariable == null) || (!i.linkedVariable.equals(call))) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.linkedVariable = call;
                    } else {
                        if (i.linkedVariable != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
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
        this.rightclickMenu.addContent(setVariableButton);

        FMContextMenu setTypeMenu = new FMContextMenu();
        this.rightclickMenu.addChild(setTypeMenu);

        AdvancedButton setTypeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_type"), true, (press) -> {
            setTypeMenu.setParentButton((AdvancedButton) press);
            setTypeMenu.openMenuAt(0, press.y);
        });
        setTypeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.set_type.desc"), "%n%"));
        this.rightclickMenu.addContent(setTypeButton);
        for (SliderCustomizationItem.SliderType t : SliderCustomizationItem.SliderType.values()) {
            AdvancedButton typeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.type." + t.getName()), (press) -> {
                if (i.type != t) {
                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                }
                i.type = t;
                i.initializeSlider();
                rightclickMenu.closeMenu();
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
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.labelPrefix = call;
                    } else {
                        if (i.labelPrefix != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
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
        this.rightclickMenu.addContent(setLabelPrefixButton);

        AdvancedButton setLabelSuffixButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.set_label_suffix"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.set_label_suffix"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if ((i.labelSuffix == null) || (!i.labelSuffix.equals(call))) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.labelSuffix = call;
                    } else {
                        if (i.labelSuffix != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
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
        this.rightclickMenu.addContent(setLabelSuffixButton);

        this.rightclickMenu.addSeparator();

        if (i.type == SliderCustomizationItem.SliderType.RANGE) {

            AdvancedButton setMinRangeValueButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.range.set_min_range_value"), (press) -> {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.range.set_min_range_value"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                    if (call != null) {
                        if (!call.replace(" ", "").equals("")) {
                            if (MathUtils.isInteger(call)) {
                                int min = Integer.parseInt(call);
                                if (i.minRangeValue != min) {
                                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                                }
                                i.minRangeValue = min;
                            }
                        } else {
                            if (i.minRangeValue != 1) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.minRangeValue = 1;
                        }
                        i.initializeSlider();
                    }
                });
                p.setText("" + i.minRangeValue);
                PopupHandler.displayPopup(p);
            });
            this.rightclickMenu.addContent(setMinRangeValueButton);

            AdvancedButton setMaxRangeValueButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.range.set_max_range_value"), (press) -> {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.range.set_max_range_value"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                    if (call != null) {
                        if (!call.replace(" ", "").equals("")) {
                            if (MathUtils.isInteger(call)) {
                                int min = Integer.parseInt(call);
                                if (i.maxRangeValue != min) {
                                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                                }
                                i.maxRangeValue = min;
                            }
                        } else {
                            if (i.maxRangeValue != 1) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.maxRangeValue = 1;
                        }
                        i.initializeSlider();
                    }
                });
                p.setText("" + i.maxRangeValue);
                PopupHandler.displayPopup(p);
            });
            this.rightclickMenu.addContent(setMaxRangeValueButton);

        }
        if (i.type == SliderCustomizationItem.SliderType.LIST) {

            AdvancedButton setListValuesButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values"), (press) -> {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values"), null, 240, (call) -> {
                    if (call != null) {
                        if (!call.replace(" ", "").equals("")) {
                            List<String> newValues = SliderCustomizationItem.deserializeValuesList(call);
                            if (newValues.size() >= 2) {
                                if (!SliderCustomizationItem.serializeValuesList(i.listValues).equals(call)) {
                                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                                }
                                i.listValues = SliderCustomizationItem.deserializeValuesList(call);
                            } else {
                                FMNotificationPopup p2 = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values.error.not_enough_values"), "%n%"));
                                PopupHandler.displayPopup(p2);
                            }
                        } else {
                            List<String> l = new ArrayList<>();
                            l.add("some_value");
                            l.add("another_value");
                            l.add("yet_another_value");
                            if (!SliderCustomizationItem.serializeValuesList(l).equals(SliderCustomizationItem.serializeValuesList(i.listValues))) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.listValues = l;
                        }
                        i.initializeSlider();
                    }
                });
                p.setText(SliderCustomizationItem.serializeValuesList(i.listValues));
                PopupHandler.displayPopup(p);
            });
            setListValuesButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.slider.editor.list.set_list_values.desc"), "%n%"));
            this.rightclickMenu.addContent(setListValuesButton);

        }

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        SliderCustomizationItem i = ((SliderCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

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
        if (i.type == SliderCustomizationItem.SliderType.RANGE) {
            sec.addEntry("min_range_value", "" + i.minRangeValue);
            sec.addEntry("max_range_value", "" + i.maxRangeValue);
        }
        if (i.type == SliderCustomizationItem.SliderType.LIST) {
            sec.addEntry("list_values", "" + SliderCustomizationItem.serializeValuesList(i.listValues));
        }

        return sec;

    }

}
