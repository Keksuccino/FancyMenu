package de.keksuccino.drippyloadingscreen.customization.items.bars.generic;

import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarLayoutEditorElement;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.PlaceholderInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;

import java.awt.*;

public class GenericProgressBarLayoutEditorElement extends AbstractProgressBarLayoutEditorElement {

    public GenericProgressBarLayoutEditorElement(CustomizationItemContainer parentContainer, AbstractProgressBarCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, handler);
    }

    @Override
    public void init() {

        super.init();

        GenericProgressBarCustomizationItem i = (GenericProgressBarCustomizationItem) this.object;

        this.rightclickMenu.addSeparator();

        AdvancedButton setProgressSourceButton = new AdvancedButton(0, 0, 0, 0, I18n.get("drippyloadingscreen.items.progress_bar.generic_progress_bar.progress_source"), true, (press) -> {
            PlaceholderInputPopup p = new PlaceholderInputPopup(new Color(0,0,0,0), I18n.get("drippyloadingscreen.items.progress_bar.generic_progress_bar.progress_source"), null, 240, (call) -> {
                if (call != null) {
                    if ((i.progressSourceString == null) || !i.progressSourceString.equals(call)) {
                        this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                    }
                    i.progressSourceString = call;
                }
            });
            if (i.progressSourceString != null) {
                p.setText(i.progressSourceString);
            }
            PopupHandler.displayPopup(p);
        });
        setProgressSourceButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.generic_progress_bar.progress_source.desc"), "\n"));
        this.rightclickMenu.addContent(setProgressSourceButton);

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        GenericProgressBarCustomizationItem i = (GenericProgressBarCustomizationItem) this.object;
        SimplePropertiesSection sec = super.serializeItem();

        if (i.progressSourceString != null) {
            sec.addEntry("progress_source", i.progressSourceString);
        }

        return sec;

    }

}
