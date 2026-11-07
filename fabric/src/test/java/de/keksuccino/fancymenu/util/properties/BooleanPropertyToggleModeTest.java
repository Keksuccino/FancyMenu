package de.keksuccino.fancymenu.util.properties;

import de.keksuccino.fancymenu.util.properties.Property.BooleanProperty.ToggleMode;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder.StackValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BooleanPropertyToggleModeTest {

    @Test
    void modeFollowsTheValueUntilManualInputIsSet() {
        Property.BooleanProperty property = Property.booleanProperty("toggle", false, "test.toggle");
        assertEquals(ToggleMode.DISABLED, ToggleMode.of(property, false));

        property.set(true);
        assertEquals(ToggleMode.ENABLED, ToggleMode.of(property, false));

        property.setManualInput("true");
        assertEquals(ToggleMode.MANUAL, ToggleMode.of(property, false));

        property.set(false);
        assertEquals(ToggleMode.DISABLED, ToggleMode.of(property, false));
    }

    @Test
    void holdersWithoutThePropertyUseTheDefaultValue() {
        assertEquals(ToggleMode.ENABLED, ToggleMode.of(null, true));
        assertEquals(ToggleMode.DISABLED, ToggleMode.of(null, false));
        assertEquals(ToggleMode.DISABLED, ToggleMode.of(null, null));
    }

    @Test
    void togglingCyclesThroughEveryModeInOrder() {
        assertEquals(ToggleMode.ENABLED, ToggleMode.DISABLED.next());
        assertEquals(ToggleMode.MANUAL, ToggleMode.ENABLED.next());
        assertEquals(ToggleMode.DISABLED, ToggleMode.MANUAL.next());
    }

    @Test
    void stacksWithACommonModeKeepIt() {
        for (ToggleMode mode : ToggleMode.values()) {
            assertEquals(mode, ToggleMode.ofStack(StackValue.of(List.of(mode, mode, mode))));
            assertEquals(mode, ToggleMode.ofStack(StackValue.of(List.of(mode))));
        }
    }

    @Test
    void mixedStacksCountAsDisabledSoTogglingEnablesAllOfThem() {
        ToggleMode enabledAndDisabled = ToggleMode.ofStack(StackValue.of(List.of(ToggleMode.ENABLED, ToggleMode.DISABLED)));
        ToggleMode partlyManual = ToggleMode.ofStack(StackValue.of(List.of(ToggleMode.MANUAL, ToggleMode.MANUAL, ToggleMode.ENABLED)));

        assertEquals(ToggleMode.DISABLED, enabledAndDisabled);
        assertEquals(ToggleMode.DISABLED, partlyManual);
        assertEquals(ToggleMode.ENABLED, partlyManual.next());
    }

    @Test
    void emptyStacksHaveNoMode() {
        assertNull(ToggleMode.ofStack(StackValue.empty()));
    }

}
