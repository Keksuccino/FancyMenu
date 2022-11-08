package de.keksuccino.fancymenu.menu.placeholder.v1.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;

//TODO übernehmen (move 'de.keksuccino.fancymenu.menu.placeholders' to new v1 placeholder package)
@Deprecated
public class Placeholders {

    public static void registerAll() {

        PlaceholderTextRegistry.registerPlaceholder(new GetVariablePlaceholder());
        
        PlaceholderTextRegistry.registerPlaceholder(new JsonPlaceholder());
        PlaceholderTextRegistry.registerPlaceholder(new WebTextPlaceholder());
        //----------------------

    }

}
