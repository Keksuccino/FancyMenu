package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.rendering.RenderUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ShapeEditorElement extends AbstractEditorElement {

    public ShapeEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add entries

//        AdvancedButton colorB = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.shape.color"), true, (press) -> {
//
//            FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.items.shape.color") + ":", null, 240, (call) -> {
//                if (call != null) {
//                    if (!call.equals("")) {
//                        Color c = RenderUtils.getColorFromHexString(call);
//                        if (c != null) {
//
//                            if (!this.getObject().getColorString().equalsIgnoreCase(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//
//                            this.getObject().setColor(call);
//
//                        }
//                    } else {
//                        if (!this.getObject().getColorString().equalsIgnoreCase("#ffffff")) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//
//                        this.getObject().setColor("#ffffff");
//                    }
//                }
//
//            });
//
//            t.setText(this.getObject().getColorString());
//
//            PopupHandler.displayPopup(t);
//
//        });
//        colorB.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.items.shape.color.btndesc")));
//        this.rightClickContextMenu.addContent(colorB);

    }

}
