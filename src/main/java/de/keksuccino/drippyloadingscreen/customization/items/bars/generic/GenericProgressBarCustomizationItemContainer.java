package de.keksuccino.drippyloadingscreen.customization.items.bars.generic;

import de.keksuccino.drippyloadingscreen.customization.items.IDrippyCustomizationItemContainer;
import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.resources.language.I18n;

public class GenericProgressBarCustomizationItemContainer extends CustomizationItemContainer implements IDrippyCustomizationItemContainer {

    public GenericProgressBarCustomizationItemContainer() {
        super("drippy_generic_progress_bar");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        GenericProgressBarCustomizationItem i = new GenericProgressBarCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 200;
        i.height = 20;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new GenericProgressBarCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new GenericProgressBarLayoutEditorElement(this, (AbstractProgressBarCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.items.progress_bar.generic_progress_bar");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.generic_progress_bar.desc"), "\n");
    }

}
