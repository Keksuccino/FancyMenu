package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.konkrete.properties.PropertiesSection;

public abstract class DeepCustomizationElement {

    protected String identifier;
    public DeepCustomizationLayer parentLayer;

    public DeepCustomizationElement(String uniqueIdentifier, DeepCustomizationLayer parentLayer) {
        this.identifier = uniqueIdentifier;
        this.parentLayer = parentLayer;
    }

    public abstract DeepCustomizationItem constructDefaultItemInstance();

    public abstract DeepCustomizationItem constructCustomizedItemInstance(PropertiesSection serializedItem);

    public abstract DeepCustomizationLayoutEditorElement constructEditorElementInstance(DeepCustomizationItem item, LayoutEditorScreen handler);

    public abstract String getDisplayName();

    public String getIdentifier() {
        return this.identifier;
    }

}
