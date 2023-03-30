

//Copyright (c) 2022 Keksuccino.
//This code is licensed under DSMSL.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.menu.placeholder.v2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class Placeholder {

    protected final String id;

    public Placeholder(String id) {
        this.id = id;
    }

    /**
     * Returns the replacement (actual text) of the given placeholder string.
     *
     * @param dps The deserialized placeholder string with placeholder values.
     */
    public abstract String getReplacementFor(DeserializedPlaceholderString dps);

    
    public abstract List<String> getValueNames();

    @Nonnull
    public abstract String getDisplayName();

    
    public abstract List<String> getDescription();

    public abstract String getCategory();

    @Nonnull
    public abstract DeserializedPlaceholderString getDefaultPlaceholderString();

    public String getIdentifier() {
        return this.id;
    }

}
