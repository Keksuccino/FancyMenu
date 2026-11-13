package de.keksuccino.fancymenu.util.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalColorPropertyTest {

    @Test
    void propertyWithoutAColorKeepsThatThroughALayoutFile() {
        Property.ColorProperty property = Property.optionalHexColorProperty("background", "#112233", true, "test.background");
        property.set(Property.ColorProperty.NO_COLOR);
        PropertyContainer container = new PropertyContainer("element");
        property.serialize(container);
        PropertyContainerSet set = new PropertyContainerSet("layout");
        set.putContainer(container);

        // A value that is missing in a file becomes the default value, so "no color" has to be a value that gets written
        PropertyContainerSet readSet = PropertiesParser.deserializeSetFromFancyString(PropertiesParser.serializeSetToFancyString(set));
        assertNotNull(readSet);
        PropertyContainer readContainer = readSet.getFirstContainerOfType("element");
        assertNotNull(readContainer);
        Property.ColorProperty loaded = Property.optionalHexColorProperty("background", "#112233", true, "test.background");
        loaded.deserialize(readContainer);

        assertTrue(readContainer.hasProperty("background"));
        assertEquals(Property.ColorProperty.NO_COLOR, loaded.get());
        assertFalse(loaded.isDefault());
    }

    @Test
    void colorsSurviveSavingLikeTheOnesOfOtherColorProperties() {
        Property.ColorProperty property = Property.optionalHexColorProperty("background", null, true, "test.background");
        property.set("#FFC94D17");
        PropertyContainer container = new PropertyContainer("element");

        property.serialize(container);
        Property.ColorProperty loaded = Property.optionalHexColorProperty("background", null, true, "test.background");
        loaded.deserialize(container);

        assertEquals("#FFC94D17", loaded.get());
        assertEquals("#FFC94D17", loaded.getHex());
    }

    @Test
    void currentValueStartsAsGivenAndDefaultStaysSeparate() {
        Property.ColorProperty property = Property.optionalHexColorProperty("background", "#112233", Property.ColorProperty.NO_COLOR, true, "test.background");

        assertEquals(Property.ColorProperty.NO_COLOR, property.get());
        assertEquals("#112233", property.getDefault());
    }

}
