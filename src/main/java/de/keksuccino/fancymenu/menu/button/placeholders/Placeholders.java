package de.keksuccino.fancymenu.menu.button.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;

public class Placeholders {

    public static void registerAll() {

        PlaceholderTextRegistry.registerPlaceholder(new GetVariablePlaceholder());

    }

}
