package de.keksuccino.fancymenu.util.input;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;

@SuppressWarnings("all")
public class TextValidators {

    public static final ConsumingSupplier<String, Boolean> NO_EMPTY_STRING_TEXT_VALIDATOR = consumes -> {
        return (consumes != null) && !consumes.replace(" ", "").isEmpty();
    };
    public static final ConsumingSupplier<String, Boolean> NO_EMPTY_STRING_SPACES_ALLOWED_TEXT_VALIDATOR = consumes -> {
        return (consumes != null) && !consumes.isEmpty();
    };
    public static final ConsumingSupplier<String, Boolean> BASIC_URL_TEXT_VALIDATOR = consumes -> {
        if ((consumes != null) && !consumes.replace(" ", "").isEmpty()) {
            if ((consumes.startsWith("http://") || consumes.startsWith("https://")) && consumes.contains(".")) return true;
        }
        return false;
    };
    public static final ConsumingSupplier<String, Boolean> HEX_COLOR_TEXT_VALIDATOR = consumes -> {
        return (consumes != null) && !consumes.replace(" ", "").isEmpty() && (DrawableColor.of(consumes) != DrawableColor.EMPTY);
    };

}
