package de.keksuccino.fancymenu.customization.element.elements.inputfield;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class InputFieldEditorElement extends AbstractEditorElement {

    public InputFieldEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add entries

//        InputFieldElement i = ((InputFieldElement)this.element);
//
//        AdvancedButton setVariableButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.editor.set_variable"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.input_field.editor.set_variable"), null, 240, (call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        if ((i.linkedVariable == null) || (!i.linkedVariable.equals(call))) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.linkedVariable = call;
//                    } else {
//                        if (i.linkedVariable != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.linkedVariable = null;
//                    }
//                }
//            });
//            if (i.linkedVariable != null) {
//                p.setText(i.linkedVariable);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        setVariableButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.editor.set_variable.desc"), "%n%"));
//        this.rightClickContextMenu.addContent(setVariableButton);
//
//        AdvancedButton setMaxLengthButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.editor.set_max_length"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.input_field.editor.set_max_length"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        if (MathUtils.isInteger(call)) {
//                            int ml = Integer.parseInt(call);
//                            if (ml != i.maxTextLength) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.maxTextLength = ml;
//                        }
//                    } else {
//                        if (i.maxTextLength != 10000) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.maxTextLength = 10000;
//                    }
//                }
//            });
//            p.setText("" + i.maxTextLength);
//            PopupHandler.displayPopup(p);
//        });
//        this.rightClickContextMenu.addContent(setMaxLengthButton);
//
//        ContextMenu setTypeMenu = new ContextMenu();
//        this.rightClickContextMenu.addChild(setTypeMenu);
//
//        AdvancedButton setTypeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.editor.set_type"), true, (press) -> {
//            setTypeMenu.setParentButton((AdvancedButton) press);
//            setTypeMenu.openMenuAt(0, press.y);
//        });
//        setTypeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.input_field.editor.set_type.desc"), "%n%"));
//        this.rightClickContextMenu.addContent(setTypeButton);
//        for (InputFieldElement.InputFieldType t : InputFieldElement.InputFieldType.values()) {
//            AdvancedButton typeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.input_field.type." + t.getName()), (press) -> {
//                if (i.type != t) {
//                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                }
//                i.type = t;
//            }) {
//                @Override
//                public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                    if (i.type == t) {
//                        this.setMessage("§a" + Locals.localize("fancymenu.customization.items.input_field.type." + t.getName()));
//                    } else {
//                        this.setMessage(Locals.localize("fancymenu.customization.items.input_field.type." + t.getName()));
//                    }
//                    super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//                }
//            };
//            setTypeMenu.addContent(typeButton);
//        }

    }

}
