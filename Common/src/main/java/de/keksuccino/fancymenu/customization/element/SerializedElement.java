package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.konkrete.properties.PropertiesSection;

/**
 * A serialized {@link AbstractElement}.
 */
public class SerializedElement extends PropertiesSection {

    public SerializedElement() {
        super("customization");
    }

    @Override
    public void addEntry(String name, String value) {
        this.getEntries().put(name, value);
    }

}
