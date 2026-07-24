package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRadius;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("PlaceholderParser global state")
class ShapeBlurRadiusTest {

    private static final String PLACEHOLDER_INPUT = "shape_blur_radius_test_placeholder";

    @ParameterizedTest
    @MethodSource("radiusCases")
    void typedPropertyIngestionSanitizesEveryBoundary(float input, float expected) {
        Property.FloatProperty property = ShapeBlurRadius.createProperty();

        property.set(input);

        assertEquals(expected, property.getFloat());
        assertEquals(expected, ShapeBlurRadius.resolve(property));
    }

    @Test
    void importedTypedValueIsClampedDuringDeserialization() {
        PropertyContainer serialized = new PropertyContainer("shape_test");
        serialized.putProperty("blur_radius", Float.toString(Float.MAX_VALUE));
        Property.FloatProperty property = ShapeBlurRadius.createProperty();

        property.deserialize(serialized);

        assertEquals(GuiBlurRadius.MAX_RADIUS, property.getFloat());
        assertEquals(GuiBlurRadius.MAX_RADIUS, ShapeBlurRadius.resolve(property));
    }

    @Test
    void resolutionProtectsSnapshotPathsThatIntentionallyBypassSetProcessors() {
        Property.FloatProperty property = ShapeBlurRadius.createProperty();
        property.applyValueSnapshot(new Property.ManualInputProperty.ManualInputSnapshot<>(null, Float.MAX_VALUE));

        assertEquals(Float.MAX_VALUE, property.getFloat());
        assertEquals(GuiBlurRadius.MAX_RADIUS, ShapeBlurRadius.resolve(property));
    }

    @Test
    void placeholderValuesAreSanitizedAfterEveryResolution() {
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        AtomicReference<String> resolvedValue = new AtomicReference<>("4.0");
        long processorId = PlaceholderParser.addParsingProcessor(PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, input -> PLACEHOLDER_INPUT.equals(input) ? resolvedValue.get() : input);
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            Property.FloatProperty property = ShapeBlurRadius.createProperty();
            property.setManualInput(PLACEHOLDER_INPUT);

            assertEquals(4.0F, ShapeBlurRadius.resolve(property));
            resolvedValue.set(Float.toString(GuiBlurRadius.MAX_RADIUS));
            assertEquals(GuiBlurRadius.MAX_RADIUS, ShapeBlurRadius.resolve(property));
            resolvedValue.set(Float.toString(Float.MAX_VALUE));
            assertEquals(GuiBlurRadius.MAX_RADIUS, ShapeBlurRadius.resolve(property));
            resolvedValue.set("-1.0");
            assertEquals(0.0F, ShapeBlurRadius.resolve(property));
            resolvedValue.set("NaN");
            assertEquals(0.0F, ShapeBlurRadius.resolve(property));
            resolvedValue.set("Infinity");
            assertEquals(0.0F, ShapeBlurRadius.resolve(property));
            resolvedValue.set("-Infinity");
            assertEquals(0.0F, ShapeBlurRadius.resolve(property));
        } finally {
            PlaceholderParser.removeParsingProcessor(processorId);
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

    private static Stream<Arguments> radiusCases() {
        return Stream.of(
                Arguments.of(-1.0F, 0.0F),
                Arguments.of(0.0F, 0.0F),
                Arguments.of(4.0F, 4.0F),
                Arguments.of(GuiBlurRadius.MAX_RADIUS, GuiBlurRadius.MAX_RADIUS),
                Arguments.of(GuiBlurRadius.MAX_RADIUS + 1.0F, GuiBlurRadius.MAX_RADIUS),
                Arguments.of(Float.MAX_VALUE, GuiBlurRadius.MAX_RADIUS),
                Arguments.of(Float.NaN, 0.0F),
                Arguments.of(Float.POSITIVE_INFINITY, 0.0F),
                Arguments.of(Float.NEGATIVE_INFINITY, 0.0F)
        );
    }

}
