package de.keksuccino.fancymenu.customization.item.v2.items.inputfield;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.customization.customizationgui.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.FMContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;

import java.awt.*;

public class InputFieldLayoutEditorElement extends LayoutEditorElement {

    public InputFieldLayoutEditorElement(InputFieldCustomizationItemContainer parentContainer, InputFieldCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        super.init();

        InputFieldCustomizationItem i = ((InputFieldCustomizationItem)this.object);

        AdvancedButton setVariableButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.editor.set_variable"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.input_field.editor.set_variable"), null, 240, (call) -> {
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
        setVariableButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.editor.set_variable.desc"), "%n%"));
        this.rightclickMenu.addContent(setVariableButton);

        AdvancedButton setMaxLengthButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.editor.set_max_length"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.input_field.editor.set_max_length"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if (MathUtils.isInteger(call)) {
                            int ml = Integer.parseInt(call);
                            if (ml != i.maxTextLength) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.maxTextLength = ml;
                        }
                    } else {
                        if (i.maxTextLength != 10000) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.maxTextLength = 10000;
                    }
                }
            });
            p.setText("" + i.maxTextLength);
            PopupHandler.displayPopup(p);
        });
        this.rightclickMenu.addContent(setMaxLengthButton);

        FMContextMenu setTypeMenu = new FMContextMenu();
        this.rightclickMenu.addChild(setTypeMenu);

        AdvancedButton setTypeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.editor.set_type"), true, (press) -> {
            setTypeMenu.setParentButton((AdvancedButton) press);
            setTypeMenu.openMenuAt(0, press.y);
        });
        setTypeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.editor.set_type.desc"), "%n%"));
        this.rightclickMenu.addContent(setTypeButton);
        for (InputFieldCustomizationItem.InputFieldType t : InputFieldCustomizationItem.InputFieldType.values()) {
            AdvancedButton typeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.type." + t.getName()), (press) -> {
                if (i.type != t) {
                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                }
                i.type = t;
            }) {
                @Override
                public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                    if (i.type == t) {
                        this.setMessage("§a" + Locals.localize("fancymenu.customization.items.input_field.type." + t.getName()));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.customization.items.input_field.type." + t.getName()));
                    }
                    super.render(p_93657_, p_93658_, p_93659_, p_93660_);
                }
            };
            setTypeMenu.addContent(typeButton);
        }

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        InputFieldCustomizationItem i = ((InputFieldCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

        if (i.linkedVariable != null) {
            sec.addEntry("linked_variable", i.linkedVariable);
        }
        sec.addEntry("input_field_type", i.type.getName());
        sec.addEntry("max_text_length", "" + i.maxTextLength);

        return sec;

    }

}
