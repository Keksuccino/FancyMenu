//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.items.ticker;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;

import java.awt.*;

//TODO implement Ticker GUI
// - Button in right-click menu to add new action (opens normal button action selection screen)
// - Button in right-click menu to manage actions (remove, edit) (is list screen with all actions in order)

public class TickerLayoutEditorElement extends LayoutEditorElement {

    public TickerLayoutEditorElement(TickerCustomizationItemContainer parentContainer, TickerCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        super.init();

        TickerCustomizationItem i = ((TickerCustomizationItem)this.object);

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
        for (TickerCustomizationItem.InputFieldType t : TickerCustomizationItem.InputFieldType.values()) {
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

        TickerCustomizationItem i = ((TickerCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

        if (i.linkedVariable != null) {
            sec.addEntry("linked_variable", i.linkedVariable);
        }
        sec.addEntry("input_field_type", i.type.getName());
        sec.addEntry("max_text_length", "" + i.maxTextLength);

        return sec;

    }

}
