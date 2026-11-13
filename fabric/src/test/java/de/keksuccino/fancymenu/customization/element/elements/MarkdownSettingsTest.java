package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTestRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.BooleanOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.ColorOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.EnumOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FloatOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FontOption;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class MarkdownSettingsTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void everyStyleOptionHasAPropertyOfItsOwn() {
        TestElement element = new TestElement();
        List<String> keys = new ArrayList<>();

        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            Property<?> property = element.settings.getProperty(option);
            assertSame(property, element.getProperty(MarkdownSettings.getPropertyKey(option)));
            assertTrue(property.getKey().startsWith(MarkdownSettings.STYLE_KEY_PREFIX), property::getKey);
            assertFalse(keys.contains(property.getKey()), property::getKey);
            keys.add(property.getKey());
        }
        assertEquals(element.style().getOptions().size(), keys.size());
    }

    @Test
    void newSettingsAreTheClassicLookWithoutOptionalSyntax() {
        TestElement element = new TestElement();
        MarkdownTestRenderer reference = new MarkdownTestRenderer();

        element.settings.tick();

        assertEquals(MarkdownLook.CLASSIC, element.settings.getLook());
        assertStylesEqual(reference.getStyle(), element.style());
        assertFalse(element.renderer.isExtendedBulletListMarkersEnabled());
        assertFalse(element.renderer.isNumberedListsEnabled());
        assertFalse(element.renderer.isCalloutsEnabled());
        assertTrue(element.renderer.isParseMarkdown());
        assertEquals(1.0F, element.renderer.getTextBaseScale());
    }

    @Test
    void newElementsStartInThePixelLookWithAllOptionalSyntax() {
        TestElement element = new TestElement();
        MarkdownTestRenderer reference = new MarkdownTestRenderer();
        reference.getStyle().applyLook(MarkdownLook.PIXEL);

        element.settings.applyDefaultsOfNewElements();
        element.settings.tick();

        assertEquals(MarkdownLook.PIXEL, element.settings.getLook());
        assertStylesEqual(reference.getStyle(), element.style());
        assertTrue(element.renderer.isExtendedBulletListMarkersEnabled());
        assertTrue(element.renderer.isNumberedListsEnabled());
        assertTrue(element.renderer.isCalloutsEnabled());
    }

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void applyingALookGivesEveryOptionTheValueOfThatLook(MarkdownLook look) {
        TestElement element = new TestElement();
        changeEveryStyleProperty(element);
        MarkdownTestRenderer reference = new MarkdownTestRenderer();
        reference.getStyle().applyLook(look);

        element.settings.applyLook(look);
        element.settings.tick();

        assertEquals(look, element.settings.getLook());
        assertStylesEqual(reference.getStyle(), element.style());
    }

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void appliedLookSurvivesSavingAndLoading(MarkdownLook look) {
        TestElement element = new TestElement();
        element.settings.applyLook(look);
        element.settings.tick();

        TestElement loaded = load(saveThroughLayoutFile(element));

        assertEquals(look, loaded.settings.getLook());
        assertStylesEqual(element.style(), loaded.style());
    }

    @Test
    void everyOptionSurvivesSavingAndLoadingWithAValueNoLookHas() {
        TestElement element = new TestElement();
        changeEveryStyleProperty(element);
        element.settings.textScale.set(1.75F);
        element.settings.parseMarkdown.set(false);
        element.settings.extendedListMarkers.set(true);
        element.settings.numberedLists.set(true);
        element.settings.callouts.set(true);
        element.settings.look.set(MarkdownLook.NEUTRAL);
        element.settings.tick();
        MarkdownTestRenderer classic = new MarkdownTestRenderer();
        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            assertFalse(haveSameValue(Objects.requireNonNull(classic.getStyle().getOption(option.getKey())), option), () -> "value did not change: " + option.getKey());
        }

        TestElement loaded = load(saveThroughLayoutFile(element));

        assertStylesEqual(element.style(), loaded.style());
        assertEquals(MarkdownLook.NEUTRAL, loaded.settings.getLook());
        assertEquals(1.75F, loaded.renderer.getTextBaseScale());
        assertFalse(loaded.renderer.isParseMarkdown());
        assertTrue(loaded.renderer.isExtendedBulletListMarkersEnabled());
        assertTrue(loaded.renderer.isNumberedListsEnabled());
        assertTrue(loaded.renderer.isCalloutsEnabled());
    }

    @Test
    void savedDataHoldsEveryOptionSoItNeedsNoLookToGetLoaded() {
        TestElement element = new TestElement();
        element.settings.applyLook(MarkdownLook.PIXEL);

        PropertyContainer saved = saveThroughLayoutFile(element);

        // Options without a color or a font get saved too, because a missing value would become whatever the classic look says
        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            assertTrue(saved.hasProperty(MarkdownSettings.getPropertyKey(option)), option::getKey);
        }
        assertTrue(saved.hasProperty(MarkdownSettings.LOOK_KEY));
    }

    @Test
    void savedValuesWinOverTheLookTheElementNames() {
        TestElement element = new TestElement();
        element.settings.applyLook(MarkdownLook.PIXEL);
        PropertyContainer saved = saveThroughLayoutFile(element);
        // What a saved element looks like after a later version of the mod gave the pixel look other values
        saved.putProperty(MarkdownSettings.STYLE_KEY_PREFIX + "list_indent", "31.5");
        saved.putProperty(MarkdownSettings.STYLE_KEY_PREFIX + "text_color", "#01020304");
        saved.putProperty(MarkdownSettings.STYLE_KEY_PREFIX + "headline_1_color", "");

        TestElement loaded = load(saved);

        assertEquals(MarkdownLook.PIXEL, loaded.settings.getLook());
        assertEquals(31.5F, loaded.style().listIndent.get());
        assertEquals(DrawableColor.of("#01020304").getColorInt(), loaded.style().textColor.argb());
        assertFalse(loaded.style().headline(1).color.isSet());
    }

    @Test
    void valuesMissingInSavedDataAreTheOnesOfTheClassicLook() {
        MarkdownTestRenderer classic = new MarkdownTestRenderer();
        SerializedElement nameOfALookOnly = new SerializedElement();
        nameOfALookOnly.putProperty(MarkdownSettings.LOOK_KEY, "pixel");

        TestElement fromNothing = load(new SerializedElement());
        TestElement fromLookName = load(nameOfALookOnly);

        assertEquals(MarkdownLook.CLASSIC, fromNothing.settings.getLook());
        assertStylesEqual(classic.getStyle(), fromNothing.style());
        assertFalse(fromNothing.renderer.isNumberedListsEnabled());
        assertFalse(fromNothing.renderer.isExtendedBulletListMarkersEnabled());
        assertFalse(fromNothing.renderer.isCalloutsEnabled());
        assertEquals(MarkdownLook.PIXEL, fromLookName.settings.getLook());
        assertStylesEqual(classic.getStyle(), fromLookName.style());
    }

    @Test
    void colorsOfTheNeutralLookStayWhenTheUiThemeChangesLater() {
        TestElement element = new TestElement();
        element.settings.applyLook(MarkdownLook.NEUTRAL);
        element.settings.tick();
        int textColor = element.style().textColor.argb();
        assertEquals(element.renderer.theme.ui_interface_generic_text_color.getColorInt(), textColor);

        element.renderer.theme.ui_interface_generic_text_color = DrawableColor.of(12, 34, 56);
        element.settings.tick();
        // Rounding to screen pixels asks for the render scale, which only the running game knows otherwise
        element.renderer.setParentRenderScale(2.0F);
        element.renderer.tick();

        assertEquals(textColor, element.style().textColor.argb());
        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            if (option instanceof ColorOption color) assertFalse(color.isFollowing(), option::getKey);
        }
    }

    @Test
    void settingAPropertyReachesTheRendererWithTheNextTick() {
        TestElement element = new TestElement();
        element.settings.tick();

        element.property(element.style().listIndent).forceSet(21.0F);
        element.property(element.style().quoteItalic).forceSet(true);
        element.property(element.style().separatorColor).forceSet("#102030");
        element.settings.numberedLists.set(true);
        element.settings.textScale.set(2.5F);
        assertEquals(8.0F, element.style().listIndent.get());
        element.settings.tick();

        assertEquals(21.0F, element.style().listIndent.get());
        assertTrue(element.style().quoteItalic.get());
        assertEquals(0xFF102030, element.style().separatorColor.argb());
        assertTrue(element.renderer.isNumberedListsEnabled());
        assertEquals(2.5F, element.renderer.getTextBaseScale());
    }

    @Test
    void ticksLeaveTheRendererAloneWhileNoPropertyChanges() {
        TestElement element = new TestElement();
        element.settings.tick();

        // If ticks pushed the values of the properties again and again, these values would not survive them
        element.style().listIndent.set(40.0F);
        element.style().textShadow.set(false);
        element.style().textColor.set(DrawableColor.of(1, 2, 3));
        element.renderer.setTextBaseScale(3.0F);
        element.settings.tick();
        element.settings.tick();

        assertEquals(40.0F, element.style().listIndent.get());
        assertFalse(element.style().textShadow.get());
        assertEquals(0xFF010203, element.style().textColor.argb());
        assertEquals(3.0F, element.renderer.getTextBaseScale());
    }

    @Test
    void valuesOfPlaceholdersFollowThemWithEveryTick() {
        String number = "markdown_settings_number";
        String color = "markdown_settings_color";
        String toggle = "markdown_settings_toggle";
        TestPlaceholders.setValue(number, "4.5");
        TestPlaceholders.setValue(color, "#AABBCC");
        TestPlaceholders.setValue(toggle, "true");
        TestElement element = new TestElement();
        ((Property.FloatProperty) element.property(element.style().lineSpacing)).setManualInput(TestPlaceholders.value(number));
        element.property(element.style().quoteBarColor).forceSet(TestPlaceholders.value(color));
        ((Property.BooleanProperty) element.property(element.style().quoteItalic)).setManualInput(TestPlaceholders.value(toggle));
        element.settings.numberedLists.setManualInput(TestPlaceholders.value(toggle));
        element.settings.textScale.setManualInput(TestPlaceholders.value(number));

        element.settings.tick();

        assertEquals(4.5F, element.style().lineSpacing.get());
        assertEquals(0xFFAABBCC, element.style().quoteBarColor.argb());
        assertTrue(element.style().quoteItalic.get());
        assertTrue(element.renderer.isNumberedListsEnabled());
        assertEquals(4.5F, element.renderer.getTextBaseScale());

        TestPlaceholders.setValue(number, "7");
        TestPlaceholders.setValue(color, "#01020380");
        TestPlaceholders.setValue(toggle, "false");
        element.settings.tick();

        assertEquals(7.0F, element.style().lineSpacing.get());
        assertEquals(0x80010203, element.style().quoteBarColor.argb());
        assertFalse(element.style().quoteItalic.get());
        assertFalse(element.renderer.isNumberedListsEnabled());
        assertEquals(7.0F, element.renderer.getTextBaseScale());
    }

    @Test
    void onlyPropertiesWithPlaceholdersGetReadAgainByLaterTicks() {
        String number = "markdown_settings_only_dynamic";
        TestPlaceholders.setValue(number, "6");
        TestElement element = new TestElement();
        ((Property.FloatProperty) element.property(element.style().lineSpacing)).setManualInput(TestPlaceholders.value(number));
        element.settings.tick();
        int evaluations = TestPlaceholders.evaluations(number);

        element.style().listIndent.set(40.0F);
        element.settings.tick();
        element.settings.tick();

        assertEquals(evaluations + 2, TestPlaceholders.evaluations(number));
        assertEquals(40.0F, element.style().listIndent.get());
        assertEquals(6.0F, element.style().lineSpacing.get());

        // A property that got its placeholder taken away is done with getting read every tick
        ((Property.FloatProperty) element.property(element.style().lineSpacing)).set(3.0F);
        element.settings.tick();
        int evaluationsWithoutPlaceholder = TestPlaceholders.evaluations(number);
        element.settings.tick();

        assertEquals(evaluationsWithoutPlaceholder, TestPlaceholders.evaluations(number));
        assertEquals(3.0F, element.style().lineSpacing.get());
    }

    @Test
    void valuesOfPlaceholdersSurviveSavingAndLoading() {
        String number = "markdown_settings_saved_placeholder";
        TestPlaceholders.setValue(number, "9");
        TestElement element = new TestElement();
        ((Property.FloatProperty) element.property(element.style().quoteIndent)).setManualInput(TestPlaceholders.value(number));

        TestElement loaded = load(saveThroughLayoutFile(element));

        assertEquals(9.0F, loaded.style().quoteIndent.get());
        TestPlaceholders.setValue(number, "11");
        loaded.settings.tick();
        assertEquals(11.0F, loaded.style().quoteIndent.get());
    }

    @Test
    void numbersBelowTheMinimumOfTheirOptionBecomeTheMinimum() {
        TestElement element = new TestElement();
        Property<?> padding = element.property(element.style().padding);

        padding.forceSet(0.5F);
        element.settings.tick();

        assertEquals(element.style().padding.getMinimum(), padding.get());
        assertEquals(element.style().padding.getMinimum(), element.style().padding.get());
    }

    @Test
    void numbersThatAreNoRealNumbersGetIgnored() {
        TestElement element = new TestElement();
        element.property(element.style().lineSpacing).forceSet(5.0F);
        element.settings.tick();

        ((Property.FloatProperty) element.property(element.style().lineSpacing)).setManualInput("NaN");
        element.settings.tick();
        assertEquals(5.0F, element.style().lineSpacing.get());

        ((Property.FloatProperty) element.property(element.style().lineSpacing)).setManualInput("Infinity");
        element.settings.tick();
        assertEquals(5.0F, element.style().lineSpacing.get());
    }

    @Test
    void optionalColorsCanBeLeftAwayAndOtherColorsCanNot() {
        TestElement element = new TestElement();
        element.property(element.style().boldTextColor).forceSet("#112233");
        element.settings.tick();
        assertEquals(0xFF112233, element.style().boldTextColor.argb());

        element.property(element.style().boldTextColor).forceSet(Property.ColorProperty.NO_COLOR);
        element.property(element.style().textColor).forceSet(Property.ColorProperty.NO_COLOR);
        element.settings.tick();

        assertFalse(element.style().boldTextColor.isSet());
        assertTrue(element.style().textColor.isSet());
        assertEquals(DrawableColor.EMPTY.getColorInt(), element.style().textColor.argb());
    }

    @Test
    void textThatIsNoColorBecomesWhite() {
        TestElement element = new TestElement();

        element.property(element.style().quoteTextColor).forceSet("no color at all");
        element.settings.tick();

        assertEquals(DrawableColor.EMPTY.getColorInt(), element.style().quoteTextColor.argb());
    }

    @Test
    void fontGetsReadFromItsIdentifierAndNothingElseIsAFont() {
        TestElement element = new TestElement();
        Property<?> font = element.property(element.style().codeFont);

        font.forceSet("minecraft:uniform");
        element.settings.tick();
        assertEquals(new ResourceLocation("minecraft:uniform"), element.style().codeFont.get());

        font.forceSet("Not A Font!");
        element.settings.tick();
        assertNull(element.style().codeFont.get());

        font.forceSet(" fancymenu:markdown_code ");
        element.settings.tick();
        assertEquals(MarkdownStyle.PIXEL_CODE_FONT, element.style().codeFont.get());

        font.forceSet("");
        element.settings.tick();
        assertNull(element.style().codeFont.get());
    }

    @Test
    void stylePropertiesResetToTheValuesOfTheLookOnceAskedTo() {
        TestElement element = new TestElement();
        element.settings.applyLook(MarkdownLook.PIXEL);
        PropertyContainer saved = saveThroughLayoutFile(element);
        float classicSpace = element.style().headline(1).spaceAbove.getLook(MarkdownLook.CLASSIC);
        float pixelSpace = element.style().headline(1).spaceAbove.getLook(MarkdownLook.PIXEL);
        assertNotEquals(classicSpace, pixelSpace);

        // An applied look is what its properties reset to right away
        assertEquals(pixelSpace, element.property(element.style().headline(1).spaceAbove).getDefault());
        assertEquals(toHex(element.style().textColor.getLookValue(MarkdownLook.PIXEL)), element.property(element.style().textColor).getDefault());
        assertEquals(MarkdownStyle.PIXEL_CODE_FONT.toString(), element.property(element.style().codeFont).getDefault());

        // A loaded element still has the classic values to reset to, which are what missing values have to become while it loads
        TestElement loaded = load(saved);
        assertEquals(classicSpace, loaded.property(loaded.style().headline(1).spaceAbove).getDefault());
        loaded.settings.updateResetValues();
        assertEquals(pixelSpace, loaded.property(loaded.style().headline(1).spaceAbove).getDefault());
        assertEquals(pixelSpace, loaded.property(loaded.style().headline(1).spaceAbove).get());
        // The pixel look leaves this optional color away
        assertEquals(Property.ColorProperty.NO_COLOR, loaded.property(loaded.style().calloutTextColor).getDefault());
    }

    @Test
    void lookListenerGetsToldAboutEveryAppliedLook() {
        TestElement element = new TestElement();
        List<MarkdownLook> looks = new ArrayList<>();
        element.settings.setLookListener(looks::add);

        element.settings.applyLook(MarkdownLook.PIXEL);
        element.settings.applyLook(MarkdownLook.NEUTRAL);
        element.settings.look.set(MarkdownLook.CLASSIC);
        element.settings.applyDefaultsOfNewElements();

        assertEquals(List.of(MarkdownLook.PIXEL, MarkdownLook.NEUTRAL, MarkdownLook.PIXEL), looks);
    }

    @Test
    void optionsOfAllLevelsAndTypesShareTheTextsOfTheirGroup() {
        TestElement element = new TestElement();
        MarkdownStyle style = element.style();

        assertEquals(element.property(style.headline(1).scale).getContextMenuEntryLocalizationKeyBase(), element.property(style.headline(3).scale).getContextMenuEntryLocalizationKeyBase());
        assertEquals(element.property(style.listLevel(1).markerShape).getContextMenuEntryLocalizationKeyBase(), element.property(style.listLevel(2).markerShape).getContextMenuEntryLocalizationKeyBase());
        assertEquals(element.property(style.callout(0).backgroundColor).getContextMenuEntryLocalizationKeyBase(), element.property(style.callout(4).backgroundColor).getContextMenuEntryLocalizationKeyBase());
        assertNotEquals(element.property(style.headline(1).scale).getContextMenuEntryLocalizationKeyBase(), element.property(style.headline(1).color).getContextMenuEntryLocalizationKeyBase());
        assertNotEquals(element.property(style.calloutTextColor).getContextMenuEntryLocalizationKeyBase(), element.property(style.callout(1).color).getContextMenuEntryLocalizationKeyBase());
        assertNotEquals(element.property(style.headlineLeaderGap).getContextMenuEntryLocalizationKeyBase(), element.property(style.headlineRuleFadeLength).getContextMenuEntryLocalizationKeyBase());
    }

    /**
     * Gives every style property a value that no look has for it.
     */
    private static void changeEveryStyleProperty(@NotNull TestElement element) {
        int index = 0;
        for (MarkdownStyleOption<?> option : element.style().getOptions()) {
            Property<?> property = element.property(option);
            index++;
            if (option instanceof BooleanOption booleanOption) {
                property.forceSet(!booleanOption.getLook(MarkdownLook.CLASSIC));
            } else if (option instanceof FloatOption floatOption) {
                property.forceSet(Math.max(0.0F, floatOption.getLook(MarkdownLook.CLASSIC)) + 3.25F + index);
            } else if (option instanceof ColorOption) {
                property.forceSet(String.format("#%02X%02X%02X%02X", (index * 7) % 256, (index * 13 + 5) % 256, (index * 29 + 11) % 256, 100 + (index % 100)));
            } else if (option instanceof EnumOption<?> enumOption) {
                Enum<?>[] values = enumOption.getValues();
                property.forceSet(values[(enumOption.getLookValue(MarkdownLook.CLASSIC).ordinal() + 1) % values.length]);
            } else if (option instanceof FontOption) {
                property.forceSet("minecraft:alt");
            }
        }
    }

    /**
     * Saves the properties of the element the way layouts do, and reads them back from the text of the layout file.
     */
    @NotNull
    static PropertyContainer saveThroughLayoutFile(@NotNull TestElement element) {
        SerializedElement serialized = new SerializedElement();
        element.getPropertyMap().forEach((key, property) -> property.serialize(serialized));
        PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
        set.putContainer(serialized);
        PropertyContainerSet readSet = PropertiesParser.deserializeSetFromFancyString(PropertiesParser.serializeSetToFancyString(set));
        assertNotNull(readSet);
        return Objects.requireNonNull(readSet.getFirstContainerOfType("element"));
    }

    /**
     * Loads saved data into a new element the way element builders do, and gives its renderer the loaded values.
     */
    @NotNull
    static TestElement load(@NotNull PropertyContainer serialized) {
        TestElement element = new TestElement();
        LegacyMarkdownSettings.upgradeTextElement(serialized);
        element.getPropertyMap().forEach((key, property) -> property.deserialize(serialized));
        element.settings.tick();
        return element;
    }

    static void assertStylesEqual(@NotNull MarkdownStyle expected, @NotNull MarkdownStyle actual) {
        for (MarkdownStyleOption<?> option : expected.getOptions()) {
            MarkdownStyleOption<?> actualOption = Objects.requireNonNull(actual.getOption(option.getKey()));
            assertTrue(haveSameValue(option, actualOption), () -> option.getKey() + ": expected " + describe(option) + " but was " + describe(actualOption));
        }
    }

    private static boolean haveSameValue(@NotNull MarkdownStyleOption<?> first, @NotNull MarkdownStyleOption<?> second) {
        if (first instanceof ColorOption firstColor) {
            ColorOption secondColor = (ColorOption) second;
            return (firstColor.isSet() == secondColor.isSet()) && (firstColor.argb() == secondColor.argb());
        }
        return Objects.equals(first.getValue(), second.getValue());
    }

    @NotNull
    private static String describe(@NotNull MarkdownStyleOption<?> option) {
        if (option instanceof ColorOption color) return color.isSet() ? Integer.toHexString(color.argb()) : "no color";
        return String.valueOf(option.getValue());
    }

    @NotNull
    private static String toHex(DrawableColor color) {
        return (color != null) ? color.getHex() : Property.ColorProperty.NO_COLOR;
    }

    /**
     * What an element is to its Markdown settings: something that holds properties and has a renderer.
     */
    static final class TestElement implements PropertyHolder {

        private final Map<String, Property<?>> properties = new LinkedHashMap<>();
        final MarkdownTestRenderer renderer = new MarkdownTestRenderer();
        final MarkdownSettings settings = new MarkdownSettings(this, this.renderer);

        @Override
        public @NotNull Map<String, Property<?>> getPropertyMap() {
            return this.properties;
        }

        @NotNull
        MarkdownStyle style() {
            return this.renderer.getStyle();
        }

        @NotNull
        Property<?> property(@NotNull MarkdownStyleOption<?> option) {
            return this.settings.getProperty(option);
        }

    }

}
