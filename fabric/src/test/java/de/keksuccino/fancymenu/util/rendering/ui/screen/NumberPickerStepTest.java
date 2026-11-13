package de.keksuccino.fancymenu.util.rendering.ui.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberPickerStepTest {

    @Test
    void wholeStepsKeepEveryDecimalOfTheValue() {
        assertEquals(14.467D, NumberPickerWindowBody.stepValue(13.467D, 1, 1.0D, 2));
        assertEquals(12.467D, NumberPickerWindowBody.stepValue(13.467D, -1, 1.0D, 2));
        assertEquals(8.0D, NumberPickerWindowBody.stepValue(3.0D, 1, 5.0D, 0));
    }

    @Test
    void stepsWithDecimalsLeaveNoRoundingNoiseBehind() {
        // 1.1F is 1.100000023841858 as a double, so adding 0.1 to it is not 1.2
        double value = (double) 1.1F;
        for (int i = 0; i < 9; i++) {
            value = NumberPickerWindowBody.stepValue((double) (float) value, 1, 0.1D, 2);
        }

        assertEquals(2.0D, value);
        assertEquals(1.5D, NumberPickerWindowBody.stepValue(2.0D, -1, 0.5D, 2));
        assertEquals(-0.1D, NumberPickerWindowBody.stepValue(0.0D, -1, 0.1D, 2));
    }

    @Test
    void stepsWithDecimalsRoundToTheDecimalPlacesOfThePicker() {
        assertEquals(1.33D, NumberPickerWindowBody.stepValue(1.234D, 1, 0.1D, 2));
        assertEquals(1.0D, NumberPickerWindowBody.stepValue(1.234D, 1, 0.1D, 0));
    }

}
