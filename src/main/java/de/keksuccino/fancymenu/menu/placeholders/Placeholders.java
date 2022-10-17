package de.keksuccino.fancymenu.menu.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;

public class Placeholders {

    public static void registerAll() {

        PlaceholderTextRegistry.registerPlaceholder(new GetVariablePlaceholder());
        
        PlaceholderTextRegistry.registerPlaceholder(new JsonPlaceholder());
        PlaceholderTextRegistry.registerPlaceholder(new WebTextPlaceholder());
        //----------------------

    }

}
