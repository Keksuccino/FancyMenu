
//Copyright (c) 2022-2023 Keksuccino.
//This code is licensed under DSMSLv2.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public abstract List<String> getValueNames();

    @NotNull
    public abstract String getDisplayName();

    @Nullable
    public abstract List<String> getDescription();

    public abstract String getCategory();

    @NotNull
    public abstract DeserializedPlaceholderString getDefaultPlaceholderString();

    public String getIdentifier() {
        return this.id;
    }

}
