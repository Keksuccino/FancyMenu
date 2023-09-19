package de.keksuccino.fancymenu.util.rendering.text.color.colors;

import de.keksuccino.fancymenu.util.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;

public class TextColorFormatters {

    public static void registerAll() {

        TextColorFormatterRegistry.register("orange", new DynamicTextColorFormatter("ü".charAt(0), () -> UIBase.getUIColorTheme().warning_text_color));
        TextColorFormatterRegistry.register("green", new DynamicTextColorFormatter("ö".charAt(0), () -> UIBase.getUIColorTheme().success_text_color));
        TextColorFormatterRegistry.register("red", new DynamicTextColorFormatter("ä".charAt(0), () -> UIBase.getUIColorTheme().error_text_color));

    }

}
