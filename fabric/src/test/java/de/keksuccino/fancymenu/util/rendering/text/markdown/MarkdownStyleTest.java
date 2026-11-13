package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.BooleanOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.ColorOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.EnumOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FloatOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.Impact;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownStyleTest {

    private static final String TEXT = "# headline\nplain **bold** [link](https://example.invalid) `code`\n\n- item\n\n> quote\n\n| a | b |\n| --- | --- |\n| c | d |\n\n```\ncode\n```\n\n---";

    @Test
    void newStyleHasTheClassicLook() {
        MarkdownStyle style = new MarkdownTestRenderer().getStyle();

        assertEquals(MarkdownLook.CLASSIC, style.getLook());
        for (MarkdownStyleOption<?> option : style.getOptions()) {
            assertOptionHasLookValue(option, MarkdownLook.CLASSIC);
        }
    }

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void applyingALookGivesEveryOptionTheValueOfThatLook(MarkdownLook look) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        MarkdownStyle style = renderer.getStyle();
        // Starts from values no look has, so the look has to set every single option
        for (MarkdownStyleOption<?> option : style.getOptions()) changeValue(option);

        style.applyLook(look);

        assertEquals(look, style.getLook());
        for (MarkdownStyleOption<?> option : style.getOptions()) {
            assertOptionHasLookValue(option, look);
        }
    }

    @Test
    void everyOptionHasAUniqueKeyThatCanGetSaved() {
        MarkdownStyle style = new MarkdownTestRenderer().getStyle();
        Set<String> keys = new HashSet<>();

        for (MarkdownStyleOption<?> option : style.getOptions()) {
            assertTrue(option.getKey().matches("[a-z][a-z0-9_]*"), () -> "key is no plain identifier: " + option.getKey());
            assertTrue(keys.add(option.getKey()), () -> "duplicate key: " + option.getKey());
            assertSame(option, style.getOption(option.getKey()));
            assertNotNull(option.getGroup());
            assertNotNull(option.getType());
        }
        assertNull(style.getOption("no_such_option"));
    }

    @Test
    void changedOptionMakesTheRendererRedoWhatItsImpactSays() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 300);
        renderer.tick();

        for (MarkdownStyleOption<?> option : renderer.getStyle().getOptions()) {
            int parses = renderer.parses;
            int layouts = renderer.layouts;
            int componentVersion = renderer.fragmentStyleVersion;

            changeValue(option);
            renderer.tick();

            Impact impact = option.getImpact();
            assertEquals(parses + ((impact == Impact.PARSE) ? 1 : 0), renderer.parses, () -> "parses after changing " + option.getKey());
            assertEquals(layouts + (((impact == Impact.PARSE) || (impact == Impact.LAYOUT)) ? 1 : 0), renderer.layouts, () -> "layouts after changing " + option.getKey());
            boolean componentsOutdated = (impact == Impact.PARSE) || (impact == Impact.COMPONENTS);
            assertEquals(componentsOutdated, componentVersion != renderer.fragmentStyleVersion, () -> "render components after changing " + option.getKey());
        }
    }

    @Test
    void settingAnOptionToTheValueItHasChangesNothing() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(TEXT, 300);
        renderer.tick();
        int componentVersion = renderer.fragmentStyleVersion;

        for (MarkdownStyleOption<?> option : renderer.getStyle().getOptions()) {
            setSameValue(option);
        }
        renderer.getStyle().applyLook(MarkdownLook.CLASSIC);
        renderer.tick();

        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
        assertEquals(componentVersion, renderer.fragmentStyleVersion);
    }

    @Test
    void applyingALookTellsTheListenerOnceWithItsBiggestImpact() {
        List<Impact> impacts = new ArrayList<>();
        MarkdownStyle style = new MarkdownStyle(impacts::add, () -> new MarkdownTestRenderer().theme);

        style.applyLook(MarkdownLook.PIXEL);

        assertEquals(1, impacts.size());
        Impact biggest = Impact.DRAW;
        for (MarkdownStyleOption<?> option : style.getOptions()) {
            if (!optionHasLookValue(option, MarkdownLook.CLASSIC) && (option.getImpact().compareTo(biggest) > 0)) biggest = option.getImpact();
        }
        assertEquals(biggest, impacts.get(0));
    }

    @Test
    void colorsOfTheNeutralLookFollowTheUiTheme() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("plain [link](https://example.invalid)", 300);
        MarkdownStyle style = renderer.getStyle();
        style.applyLook(MarkdownLook.NEUTRAL);
        renderer.tick();
        MarkdownTextFragment link = renderer.fragmentContaining("link");
        Component component = link.getRenderComponent();
        assertTrue(style.textColor.isFollowing());
        assertEquals(renderer.theme.ui_interface_generic_text_color.getColorInt(), style.textColor.argb());

        renderer.tick();
        assertSame(component, link.getRenderComponent());

        renderer.theme.ui_interface_generic_text_color = DrawableColor.of(1, 2, 3);
        renderer.theme.input_field_suggestions_text_color_selected = DrawableColor.of(4, 5, 6);
        renderer.tick();

        assertEquals(0xFF010203, style.textColor.argb());
        assertEquals(0xFF010203, style.textColor.get().getColorInt());
        assertEquals(0xFF040506, style.hyperlinkColor.argb());
        assertNotSame(component, link.getRenderComponent());
        assertEquals(0x040506, link.getRenderComponent().getStyle().getColor().getValue());
        assertEquals(1, renderer.parses);
        assertEquals(1, renderer.layouts);
    }

    @Test
    void colorThatGetsSetStopsFollowingTheUiTheme() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        MarkdownStyle style = renderer.getStyle();
        style.applyLook(MarkdownLook.NEUTRAL);
        DrawableColor color = DrawableColor.of(9, 8, 7);

        style.textColor.set(color);
        renderer.theme.ui_interface_generic_text_color = DrawableColor.of(1, 2, 3);
        renderer.tick();

        assertFalse(style.textColor.isFollowing());
        assertSame(color, style.textColor.get());
        assertEquals(0xFF090807, style.textColor.argb());
    }

    @Test
    void sizeNeverGoesBelowItsMinimum() {
        MarkdownStyle style = new MarkdownTestRenderer().getStyle();

        style.padding.set(0.5F);

        assertEquals(style.padding.getMinimum(), style.padding.get());
    }

    @Test
    void settersOfTheRendererThatCoverSeveralOptionsSetAllOfThem() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        MarkdownStyle style = renderer.getStyle();
        DrawableColor color = DrawableColor.of(1, 2, 3);

        renderer.setQuoteColor(color);
        renderer.setHeadlineLineColor(color);
        renderer.setBulletListDotColor(color);

        assertSame(color, style.quoteTextColor.get());
        assertSame(color, style.quoteBarColor.get());
        assertSame(color, renderer.getQuoteColor());
        assertSame(color, renderer.getHeadlineUnderlineColor());
        assertSame(color, renderer.getBulletListDotColor());
        for (int level = 1; level <= 3; level++) {
            assertSame(color, style.headline(level).decorationColor.get());
            assertSame(color, style.listLevel(level).markerColor.get());
        }
    }

    private static void assertOptionHasLookValue(MarkdownStyleOption<?> option, MarkdownLook look) {
        assertTrue(optionHasLookValue(option, look), () -> option.getKey() + " has " + option.getValue() + " instead of the " + look + " value " + option.getLookValue(look));
    }

    private static boolean optionHasLookValue(MarkdownStyleOption<?> option, MarkdownLook look) {
        Object value = option.getValue();
        Object lookValue = option.getLookValue(look);
        if ((value instanceof DrawableColor color) && (lookValue instanceof DrawableColor lookColor)) return color.getColorInt() == lookColor.getColorInt();
        return (value == null) ? (lookValue == null) : value.equals(lookValue);
    }

    /**
     * Gives the option a value that differs from its current one and from the values all looks give it.
     */
    private static void changeValue(MarkdownStyleOption<?> option) {
        Object before = option.getValue();
        if (option instanceof BooleanOption bool) {
            bool.set(!bool.get());
        } else if (option instanceof FloatOption size) {
            size.set(size.get() + 1.25F);
        } else if (option instanceof ColorOption color) {
            color.set(DrawableColor.of(12, 34, (color.argb() & 0xFF) ^ 0x55, 200));
        } else if (option instanceof EnumOption<?> mode) {
            setNextValue(mode);
        }
        assertNotEquals(before, option.getValue(), () -> "value of " + option.getKey() + " did not change");
    }

    private static <E extends Enum<E>> void setNextValue(EnumOption<E> option) {
        E[] values = option.getValues();
        option.set(values[(option.get().ordinal() + 1) % values.length]);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setSameValue(MarkdownStyleOption<T> option) {
        Object value = option.getValue();
        if ((option instanceof ColorOption color) && (value != null)) {
            // An equal color in another object is the same value
            color.set(DrawableColor.of((color.argb() >> 16) & 0xFF, (color.argb() >> 8) & 0xFF, color.argb() & 0xFF, color.argb() >>> 24));
        } else {
            option.setValue((T) value);
        }
    }

}
