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

    /**
     * Only for <b>internal</b> use. Don't touch this if you don't know what you're doing!
     */
    public SerializedElement(@NotNull String type) {
        super(type);
    }

}
