package de.keksuccino.fancymenu.misc;

import de.keksuccino.konkrete.properties.PropertiesSection;

public class OverrideablePropertiesSection extends PropertiesSection {

    public OverrideablePropertiesSection(String type) {
        super(type);
    }

    @Override
    public void addEntry(String name, String value) {
        this.getEntries().put(name, value);
    }

}
