package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetLabelAlignmentTest {

    @Test
    void leftAndRightLabelsKeepTheSidePaddingToTheirSide() {
        assertEquals(14, WidgetLabelAlignment.LEFT.getLabelX(12, 208, 31));
        assertEquals(175, WidgetLabelAlignment.RIGHT.getLabelX(12, 208, 31));
        assertEquals(14.0F, WidgetLabelAlignment.LEFT.getLabelX(12.0F, 208.0F, 31.5F, 1.0F));
        assertEquals(174.5F, WidgetLabelAlignment.RIGHT.getLabelX(12.0F, 208.0F, 31.5F, 1.0F));
    }

    @Test
    void sidePaddingStaysTheSameOnScreenForScaledLabels() {
        // The room of a label rendered at scale 2 is in the space of that scale, so 1 unit there is 2 pixels on the screen
        assertEquals(13.0F, WidgetLabelAlignment.LEFT.getLabelX(12.0F, 208.0F, 31.5F, 2.0F));
        assertEquals(175.5F, WidgetLabelAlignment.RIGHT.getLabelX(12.0F, 208.0F, 31.5F, 2.0F));
        assertEquals(94.5F, WidgetLabelAlignment.CENTERED.getLabelX(12.0F, 208.0F, 31.0F, 2.0F));
    }

    @Test
    void labelsThatOnlyFitWithoutTheFullPaddingStayInsideTheirRoom() {
        assertEquals(3, WidgetLabelAlignment.LEFT.getLabelX(2, 12, 9));
        assertEquals(2, WidgetLabelAlignment.RIGHT.getLabelX(2, 12, 9));
        assertEquals(3.0F, WidgetLabelAlignment.LEFT.getLabelX(2.0F, 12.0F, 9.0F, 1.0F));
        assertEquals(2.0F, WidgetLabelAlignment.RIGHT.getLabelX(2.0F, 12.0F, 9.0F, 1.0F));
    }

    @Test
    void centeredLabelsUseTheCenteringMathOfVanillaWidgetLabels() {
        // Vanilla: clamp((left + right) / 2, left + width / 2, right - width / 2) - width / 2, all in whole pixels
        assertEquals(95, WidgetLabelAlignment.CENTERED.getLabelX(12, 208, 31));
        assertEquals(95, WidgetLabelAlignment.CENTERED.getLabelX(12, 208, 30));
        assertEquals(96, WidgetLabelAlignment.CENTERED.getLabelX(13, 209, 30));
        assertEquals(94.5F, WidgetLabelAlignment.CENTERED.getLabelX(12.0F, 208.0F, 31.0F, 1.0F));
    }

    @Test
    void labelsThatFillTheirWholeRoomStartAtItsLeftEdgeWithEveryAlignment() {
        for (WidgetLabelAlignment alignment : WidgetLabelAlignment.values()) {
            assertEquals(2, alignment.getLabelX(2, 12, 10), alignment.name());
            assertEquals(2, alignment.getLabelX(2, 13, 11), alignment.name());
            assertEquals(2.0F, alignment.getLabelX(2.0F, 12.0F, 10.0F, 1.0F), alignment.name());
        }
    }

    @Test
    void ownAlignmentWinsOverTheGlobalOneUnlessItIsCentered() {
        assertEquals(WidgetLabelAlignment.LEFT, WidgetLabelAlignment.resolve(WidgetLabelAlignment.LEFT, WidgetLabelAlignment.RIGHT));
        assertEquals(WidgetLabelAlignment.RIGHT, WidgetLabelAlignment.resolve(WidgetLabelAlignment.RIGHT, WidgetLabelAlignment.CENTERED));
        assertEquals(WidgetLabelAlignment.RIGHT, WidgetLabelAlignment.resolve(WidgetLabelAlignment.CENTERED, WidgetLabelAlignment.RIGHT));
        assertEquals(WidgetLabelAlignment.CENTERED, WidgetLabelAlignment.resolve(WidgetLabelAlignment.CENTERED, WidgetLabelAlignment.CENTERED));
    }

    @Test
    void serializedNamesGetReadInAnyCaseAndUnknownNamesFallBackToCentered() {
        for (WidgetLabelAlignment alignment : WidgetLabelAlignment.values()) {
            assertEquals(alignment, WidgetLabelAlignment.getBySerializedName(alignment.getSerializedName()));
        }
        assertEquals(WidgetLabelAlignment.RIGHT, WidgetLabelAlignment.getBySerializedName(" Right "));
        assertEquals(WidgetLabelAlignment.CENTERED, WidgetLabelAlignment.getBySerializedName("middle"));
        assertEquals(WidgetLabelAlignment.CENTERED, WidgetLabelAlignment.getBySerializedName(""));
        assertEquals(WidgetLabelAlignment.CENTERED, WidgetLabelAlignment.getBySerializedName(null));
    }

    @Test
    void propertySavesTheAlignmentByItsSerializedName() {
        Property<WidgetLabelAlignment> property = WidgetLabelAlignment.buildProperty();
        property.set(WidgetLabelAlignment.LEFT);
        PropertyContainer container = new PropertyContainer("test");

        property.serialize(container);
        Property<WidgetLabelAlignment> loaded = WidgetLabelAlignment.buildProperty();
        loaded.deserialize(container);

        assertEquals(WidgetLabelAlignment.LEFT.getSerializedName(), container.getValue(WidgetLabelAlignment.PROPERTY_KEY));
        assertEquals(WidgetLabelAlignment.LEFT, loaded.get());
    }

    @Test
    void elementsSavedWithoutAnAlignmentKeepCenteredLabels() {
        Property<WidgetLabelAlignment> property = WidgetLabelAlignment.buildProperty();

        property.deserialize(new PropertyContainer("test"));

        assertEquals(WidgetLabelAlignment.CENTERED, property.get());
        assertTrue(property.isDefault());
    }

}
