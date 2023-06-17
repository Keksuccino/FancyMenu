package de.keksuccino.fancymenu.rendering.text.color.colors;

import de.keksuccino.fancymenu.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.fancymenu.rendering.text.color.TextColorFormatterRegistry;
import de.keksuccino.fancymenu.rendering.ui.UIBase;

public class TextColorFormatters {

    public static void registerAll() {

        TextColorFormatterRegistry.register("orange", new DynamicTextColorFormatter('ü', () -> UIBase.getUIColorScheme().warningTextColor));
        TextColorFormatterRegistry.register("green", new DynamicTextColorFormatter('ö', () -> UIBase.getUIColorScheme().successTextColor));
        TextColorFormatterRegistry.register("red", new DynamicTextColorFormatter('ä', () -> UIBase.getUIColorScheme().errorTextColor));

    }

}
