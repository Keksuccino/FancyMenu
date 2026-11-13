package de.keksuccino.fancymenu.util.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NumericInputBehaviorTest {

    @Test
    void behaviorHasNoStepUnlessOneGetsSet() {
        assertNull(Property.NumericInputBehavior.<Float>builder().freeInput().build().getStep());
        assertNull(Property.NumericInputBehavior.<Integer>builder().rangeInput(0, 10).build().getStep());
    }

    @Test
    void stepStaysWhateverInputModeGetsChosen() {
        assertEquals(0.1F, Property.NumericInputBehavior.<Float>builder().step(0.1F).freeInput().build().getStep());
        assertEquals(0.5F, Property.NumericInputBehavior.<Float>builder().rangeInput(0.0F, 4.0F).step(0.5F).build().getStep());
        assertEquals(2, Property.NumericInputBehavior.<Integer>builder().step(2).cycleInput(List.of(0, 2, 4)).build().getStep());
    }

}
