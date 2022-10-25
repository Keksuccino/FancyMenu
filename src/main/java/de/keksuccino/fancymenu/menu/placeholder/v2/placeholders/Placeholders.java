package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders;

import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderHandler;

public class Placeholders {

    public static void registerAll() {

        PlaceholderHandler.registerPlaceholder(new TestPlaceholder1());
        PlaceholderHandler.registerPlaceholder(new TestPlaceholder2());
        PlaceholderHandler.registerPlaceholder(new TestPlaceholder3());

    }

}
