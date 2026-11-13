package de.keksuccino.fancymenu.util.properties;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumPropertyTest {

    @Test
    void everyValueSurvivesSerializationByItsLowercaseName() {
        for (Direction direction : Direction.values()) {
            Property<Direction> property = newProperty(Direction.LEFT, Direction.LEFT);
            property.set(direction);
            PropertyContainer container = new PropertyContainer("test");

            property.serialize(container);
            Property<Direction> loaded = newProperty(Direction.LEFT, Direction.LEFT);
            loaded.deserialize(container);

            assertEquals(direction.name().toLowerCase(Locale.ROOT), container.getValue("direction"));
            assertEquals(direction, loaded.get());
        }
    }

    @Test
    void namesGetReadInAnyCase() {
        PropertyContainer container = new PropertyContainer("test");
        container.putProperty("direction", "Top_Right");
        Property<Direction> property = newProperty(Direction.LEFT, Direction.LEFT);

        property.deserialize(container);

        assertEquals(Direction.TOP_RIGHT, property.get());
    }

    @Test
    void unknownOrMissingNamesFallBackToTheDefault() {
        PropertyContainer unknown = new PropertyContainer("test");
        unknown.putProperty("direction", "not_a_direction");
        Property<Direction> fromUnknown = newProperty(Direction.TOP_RIGHT, Direction.LEFT);
        fromUnknown.deserialize(unknown);

        Property<Direction> fromMissing = newProperty(Direction.TOP_RIGHT, Direction.LEFT);
        fromMissing.deserialize(new PropertyContainer("test"));

        assertEquals(Direction.TOP_RIGHT, fromUnknown.get());
        assertEquals(Direction.TOP_RIGHT, fromMissing.get());
        assertTrue(fromMissing.isDefault());
    }

    @Test
    void currentValueStartsAsGivenAndDefaultStaysSeparate() {
        Property<Direction> property = newProperty(Direction.LEFT, Direction.TOP_RIGHT);

        assertEquals(Direction.TOP_RIGHT, property.get());
        assertEquals(Direction.LEFT, property.getDefault());
        assertFalse(property.isDefault());
    }

    private static Property<Direction> newProperty(Direction defaultValue, Direction currentValue) {
        return Property.enumProperty("direction", defaultValue, currentValue, "test.direction", value -> "test.direction." + value.name());
    }

    private enum Direction {

        LEFT,
        TOP_RIGHT,
        DOWN

    }

}
