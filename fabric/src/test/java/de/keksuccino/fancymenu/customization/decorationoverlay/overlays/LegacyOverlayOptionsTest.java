package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LegacyOverlayOptionsTest {

    @Test
    void legacyMultipliersBecomeAmountPercentagesWithinTheSliderRange() {
        assertEquals(100, LegacyOverlayOptions.toAmountPercent(1.0F));
        assertEquals(130, LegacyOverlayOptions.toAmountPercent(1.3F));
        assertEquals(DecorationOverlayOptions.Amount.MIN_PERCENT, LegacyOverlayOptions.toAmountPercent(0.0F));
        assertEquals(DecorationOverlayOptions.Amount.MIN_PERCENT, LegacyOverlayOptions.toAmountPercent(-2.0F));
        assertEquals(DecorationOverlayOptions.Amount.MAX_PERCENT, LegacyOverlayOptions.toAmountPercent(5.0F));
    }

    @Test
    void legacyScalesBecomeTheNearestWholeSize() {
        assertEquals(1, LegacyOverlayOptions.toSize(1.0F));
        assertEquals(1, LegacyOverlayOptions.toSize(0.25F));
        assertEquals(1, LegacyOverlayOptions.toSize(1.4F));
        assertEquals(2, LegacyOverlayOptions.toSize(1.5F));
        assertEquals(3, LegacyOverlayOptions.toSize(2.6F));
        assertEquals(3, LegacyOverlayOptions.toSize(6.0F));
    }

    @Test
    void readFloatOnlyAcceptsFiniteNumbers() {
        PropertyContainer from = new PropertyContainer("decoration_overlay");
        from.putProperty("number", " 1.75 ");
        from.putProperty("text", "much");
        from.putProperty("infinite", "Infinity");

        assertEquals(1.75F, LegacyOverlayOptions.readFloat(from, "number"));
        assertNull(LegacyOverlayOptions.readFloat(from, "text"));
        assertNull(LegacyOverlayOptions.readFloat(from, "infinite"));
        assertNull(LegacyOverlayOptions.readFloat(from, "missing"));
    }

    @Test
    void legacyValuesOnlyCountWhileTheNewOptionHasNoSavedValue() {
        TestHolder holder = new TestHolder();
        DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(holder);
        DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(holder);

        PropertyContainer old = new PropertyContainer("decoration_overlay");
        old.putProperty("old_intensity", "2.0");
        old.putProperty("old_scale", "3.0");
        LegacyOverlayOptions.migrateAmount(old, amount, "old_intensity");
        LegacyOverlayOptions.migrateSize(old, size, "old_scale");
        assertEquals(200, amount.getPercent());
        assertEquals(3, size.get());

        PropertyContainer both = new PropertyContainer("decoration_overlay");
        both.putProperty("old_intensity", "2.0");
        both.putProperty("old_scale", "3.0");
        both.putProperty(DecorationOverlayOptions.Amount.DEFAULT_KEY, "75");
        both.putProperty(DecorationOverlayOptions.Size.DEFAULT_KEY, "1");
        amount.property.deserialize(both);
        size.property.deserialize(both);
        LegacyOverlayOptions.migrateAmount(both, amount, "old_intensity");
        LegacyOverlayOptions.migrateSize(both, size, "old_scale");
        assertEquals(75, amount.getPercent());
        assertEquals(1, size.get());
    }

    private static final class TestHolder implements PropertyHolder {

        private final Map<String, Property<?>> properties = new LinkedHashMap<>();

        @Override
        public Map<String, Property<?>> getPropertyMap() {
            return this.properties;
        }

    }

}
