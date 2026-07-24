package de.keksuccino.fancymenu.customization.element.elements.shape;

import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRadius;
import org.jetbrains.annotations.NotNull;

/**
 * Shared construction and resolution policy for shape-element blur radius properties.
 */
public final class ShapeBlurRadius {

    private static final String PROPERTY_KEY = "blur_radius";
    private static final String LOCALIZATION_KEY = "fancymenu.elements.shape.blur.radius";
    private static final float DEFAULT_RADIUS = 3.0F;

    private ShapeBlurRadius() {
    }

    /**
     * Creates the bounded property used by every shape type. The set processor protects typed
     * editor, import, and direct-set paths; {@link #resolve(Property.FloatProperty)} separately
     * protects manual placeholder values because those bypass the property's set processor.
     */
    @NotNull
    public static Property.FloatProperty createProperty() {
        Property.FloatProperty property = Property.floatProperty(PROPERTY_KEY, DEFAULT_RADIUS, LOCALIZATION_KEY, Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, GuiBlurRadius.MAX_RADIUS).build());
        property.setValueSetProcessor(GuiBlurRadius::sanitize);
        return property;
    }

    public static float resolve(@NotNull Property.FloatProperty property) {
        return GuiBlurRadius.sanitize(property.getFloat());
    }

}
