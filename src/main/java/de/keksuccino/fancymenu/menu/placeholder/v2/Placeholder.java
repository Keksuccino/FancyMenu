package de.keksuccino.fancymenu.menu.placeholder.v2;

import java.util.List;

public abstract class Placeholder {

    protected final String id;

    public Placeholder(String id) {
        this.id = id;
    }

    /**
     * Returns the replacement (actual text) of the given placeholder string.
     *
     * @param ps The deserialized placeholder string with placeholder values.
     */
    public abstract String getReplacementFor(DeserializedPlaceholderString ps);

    public abstract String getDisplayName();

    public abstract List<String> getDescription();

    public abstract String getCategory();

    public String getIdentifier() {
        return this.id;
    }

}
