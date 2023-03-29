package de.keksuccino.fancymenu.menu.placeholder.v1.placeholders;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;


@Deprecated
public class Placeholders {

    public static void registerAll() {

        PlaceholderTextRegistry.registerPlaceholder(new GetVariablePlaceholder());
        
        PlaceholderTextRegistry.registerPlaceholder(new JsonPlaceholder());
        PlaceholderTextRegistry.registerPlaceholder(new WebTextPlaceholder());
        

    }

}
