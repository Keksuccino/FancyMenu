package de.keksuccino.fancymenu.util.rendering.text.color.colors;

import de.keksuccino.fancymenu.util.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;

public class TextColorFormatters {

    public static void registerAll() {

        TextColorFormatterRegistry.register("orange", new DynamicTextColorFormatter('ü', () -> UIBase.getUIColorTheme().warning_text_color));
        TextColorFormatterRegistry.register("green", new DynamicTextColorFormatter('ö', () -> UIBase.getUIColorTheme().success_text_color));
        TextColorFormatterRegistry.register("red", new DynamicTextColorFormatter('ä', () -> UIBase.getUIColorTheme().error_text_color));

    }

}
