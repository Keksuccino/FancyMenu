package de.keksuccino.fancymenu.rendering.text.color.colors;

import de.keksuccino.fancymenu.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.fancymenu.rendering.text.color.TextColorFormatterRegistry;
import de.keksuccino.fancymenu.rendering.ui.UIBase;

public class TextColorFormatters {

    public static void registerAll() {

        TextColorFormatterRegistry.register("black", new DynamicTextColorFormatter('z', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorBlack));
        TextColorFormatterRegistry.register("dark_blue", new DynamicTextColorFormatter('y', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorDarkBlue));
        TextColorFormatterRegistry.register("dark_green", new DynamicTextColorFormatter('x', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorDarkGreen));
        TextColorFormatterRegistry.register("dark_aqua", new DynamicTextColorFormatter('w', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorDarkAqua));
        TextColorFormatterRegistry.register("dark_red", new DynamicTextColorFormatter('v', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorDarkRed));
        TextColorFormatterRegistry.register("dark_purple", new DynamicTextColorFormatter('u', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorDarkPurple));
        TextColorFormatterRegistry.register("gold", new DynamicTextColorFormatter('t', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorGold));
        TextColorFormatterRegistry.register("gray", new DynamicTextColorFormatter('s', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorGray));
        TextColorFormatterRegistry.register("dark_gray", new DynamicTextColorFormatter('ü', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorDarkGray));
        TextColorFormatterRegistry.register("blue", new DynamicTextColorFormatter('q', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorBlue));
        TextColorFormatterRegistry.register("green", new DynamicTextColorFormatter('p', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorGreen));
        TextColorFormatterRegistry.register("aqua", new DynamicTextColorFormatter('ö', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorAqua));
        TextColorFormatterRegistry.register("red", new DynamicTextColorFormatter('ä', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorRed));
        TextColorFormatterRegistry.register("light_purple", new DynamicTextColorFormatter('ß', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorLightPurple));
        TextColorFormatterRegistry.register("yellow", new DynamicTextColorFormatter('#', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorYellow));
        TextColorFormatterRegistry.register("white", new DynamicTextColorFormatter('*', () -> UIBase.getUIColorScheme().fontRendererTextFormattingColorWhite));

    }

}
