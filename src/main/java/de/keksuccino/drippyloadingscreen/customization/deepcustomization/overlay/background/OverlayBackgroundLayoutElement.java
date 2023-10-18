package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import org.jetbrains.annotations.NotNull;

public class OverlayBackgroundLayoutElement extends DeepCustomizationLayoutEditorElement {

    public OverlayBackgroundLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public SimplePropertiesSection serializeItem() {

        OverlayBackgroundItem i = (OverlayBackgroundItem) this.object;
        SimplePropertiesSection sec = new SimplePropertiesSection();

        if (i.hexColor != null) {
            sec.addEntry("custom_color_hex", i.hexColorString);
        }

        return sec;

    }

}