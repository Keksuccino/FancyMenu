package de.keksuccino.drippyloadingscreen.customization.items.bars.loading;

import de.keksuccino.drippyloadingscreen.customization.items.IDrippyCustomizationItemContainer;
import de.keksuccino.drippyloadingscreen.customization.items.bars.AbstractProgressBarCustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.resources.language.I18n;

public class CustomLoadingBarCustomizationItemContainer extends CustomizationItemContainer implements IDrippyCustomizationItemContainer {

    public CustomLoadingBarCustomizationItemContainer() {
        super("drippy_custom_loading_bar");
    }

    @Override
    public CustomizationItem constructDefaultItemInstance() {
        CustomLoadingBarCustomizationItem i = new CustomLoadingBarCustomizationItem(this, new PropertiesSection("dummy"));
        i.width = 200;
        i.height = 20;
        return i;
    }

    @Override
    public CustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem) {
        return new CustomLoadingBarCustomizationItem(this, serializedItem);
    }

    @Override
    public LayoutEditorElement constructEditorElementInstance(CustomizationItem item, LayoutEditorScreen handler) {
        return new CustomLoadingBarLayoutEditorElement(this, (AbstractProgressBarCustomizationItem) item, handler);
    }

    @Override
    public String getDisplayName() {
        return I18n.get("drippyloadingscreen.items.progress_bar.custom_loading_bar");
    }

    @Override
    public String[] getDescription() {
        return StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.custom_loading_bar.desc"), "\n");
    }

}
