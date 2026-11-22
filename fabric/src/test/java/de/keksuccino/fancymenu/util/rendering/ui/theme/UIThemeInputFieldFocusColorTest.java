package de.keksuccino.fancymenu.util.rendering.ui.theme;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.CherryBlossomUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.CozyCampfireUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.DarkHighContrastUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.DarkUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.LightHighContrastUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.LightUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.PumpkinSoupUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.PurpleVoidUITheme;
import de.keksuccino.fancymenu.util.rendering.ui.theme.themes.SpookySeasonUITheme;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The border color is the only steady sign of which text field has the focus (the cursor blinks and empty fields show no text),
 * so every built-in theme and every theme file that leaves these colors out has to give focused fields a clearly different border.
 */
class UIThemeInputFieldFocusColorTest {

    /** Smallest RGB distance between the focused and the unfocused border that still tells them apart at a glance. */
    private static final double MIN_BORDER_COLOR_DISTANCE = 64.0D;

    /** The themes of UIThemes.DEFAULT_THEMES, created here because UIThemes creates the theme folder of the game directory when it loads. */
    static Stream<UITheme> builtInThemes() {
        return Stream.of(new LightUITheme(), new LightHighContrastUITheme(), new DarkUITheme(), new DarkHighContrastUITheme(), new CherryBlossomUITheme(), new SpookySeasonUITheme(), new PumpkinSoupUITheme(), new CozyCampfireUITheme(), new PurpleVoidUITheme());
    }

    @ParameterizedTest
    @MethodSource("builtInThemes")
    void focusedInputFieldBorderStandsOutFromUnfocusedBorder(@NotNull UITheme theme) {
        assertFocusedBorderStandsOut(theme.getIdentifier(), theme.ui_interface_input_field_border_color_normal, theme.ui_interface_input_field_border_color_focused);
    }

    @ParameterizedTest
    @MethodSource("builtInThemes")
    void focusedInputFieldBorderStandsOutFromUnfocusedBorderInBlurMode(@NotNull UITheme theme) {
        assertFocusedBorderStandsOut(theme.getIdentifier(), theme.ui_blur_interface_input_field_border_color_normal, theme.ui_blur_interface_input_field_border_color_focused);
    }

    @Test
    void themeFileWithoutFocusedBorderColorsFallsBackToVisibleFocusedBorders() {
        UITheme defaults = new UITheme("defaults", "defaults");
        String json = "{"
                + "\"identifier\": \"custom_theme\","
                + "\"display_name\": \"custom_theme\","
                + "\"ui_interface_input_field_border_color_normal\": {\"hex\": \"#3B3B3BFF\"},"
                + "\"ui_blur_interface_input_field_border_color_normal\": {\"hex\": \"#5D616466\"}"
                + "}";

        UITheme theme = UIColorThemeSerializer.deserializeTheme(json);

        assertNotNull(theme);
        assertEquals(defaults.ui_interface_input_field_border_color_focused.getColorInt(), theme.ui_interface_input_field_border_color_focused.getColorInt());
        assertEquals(defaults.ui_blur_interface_input_field_border_color_focused.getColorInt(), theme.ui_blur_interface_input_field_border_color_focused.getColorInt());
        assertFocusedBorderStandsOut(theme.getIdentifier(), theme.ui_interface_input_field_border_color_normal, theme.ui_interface_input_field_border_color_focused);
        assertFocusedBorderStandsOut(theme.getIdentifier(), theme.ui_blur_interface_input_field_border_color_normal, theme.ui_blur_interface_input_field_border_color_focused);
    }

    private static void assertFocusedBorderStandsOut(@NotNull String themeIdentifier, @NotNull DrawableColor normal, @NotNull DrawableColor focused) {
        Color normalColor = normal.getColor();
        Color focusedColor = focused.getColor();
        double distance = Math.sqrt(square(normalColor.getRed() - focusedColor.getRed()) + square(normalColor.getGreen() - focusedColor.getGreen()) + square(normalColor.getBlue() - focusedColor.getBlue()));
        assertTrue(distance >= MIN_BORDER_COLOR_DISTANCE, "Focused border of theme '" + themeIdentifier + "' is too close to its unfocused border: " + normal.getHex() + " vs " + focused.getHex());
        assertTrue(focusedColor.getAlpha() >= normalColor.getAlpha(), "Focused border of theme '" + themeIdentifier + "' is more transparent than its unfocused border: " + normal.getHex() + " vs " + focused.getHex());
    }

    private static double square(int value) {
        return (double) value * value;
    }

}
