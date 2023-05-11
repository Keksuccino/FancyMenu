package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.misc.OverrideablePropertiesSection;
import org.jetbrains.annotations.NotNull;

/**
 * A serialized {@link AbstractElement}.
 */
public class SerializedElement extends OverrideablePropertiesSection {

    public SerializedElement() {
        super("element");
    }

    public SerializedElement(@NotNull String type) {
        super(type);
    }

}
