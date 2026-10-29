package de.keksuccino.fancymenu.util.rendering.ui.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPickerStateTest {

    private static final ColorPickerState.Channel[] CHANNELS = ColorPickerState.Channel.values();

    @Test
    void presetColorIsKeptExactly() {
        int[] presets = {0xFFFFFFFF, 0x00000000, 0x80FF0000, 0xFF3366CC, 0x7F010203, 0xC8FEFDFC};
        for (int preset : presets) {
            ColorPickerState state = new ColorPickerState(preset);
            assertEquals(preset, state.getArgb());
            assertEquals((preset >> 16) & 0xFF, state.getChannel(ColorPickerState.Channel.RED));
            assertEquals((preset >> 8) & 0xFF, state.getChannel(ColorPickerState.Channel.GREEN));
            assertEquals(preset & 0xFF, state.getChannel(ColorPickerState.Channel.BLUE));
            assertEquals((preset >>> 24) & 0xFF, state.getChannel(ColorPickerState.Channel.ALPHA));
        }
    }

    @Test
    void everyChannelValueIsKeptExactlyWithoutTouchingOtherChannels() {
        int[] bases = {0xFF000000, 0xFFFFFFFF, 0x80C83264, 0x00808080};
        for (int base : bases) {
            for (ColorPickerState.Channel channel : CHANNELS) {
                for (int channelValue = 0; channelValue <= ColorPickerState.CHANNEL_MAX_VALUE; channelValue++) {
                    ColorPickerState state = new ColorPickerState(base);
                    state.setChannel(channel, channelValue);
                    for (ColorPickerState.Channel other : CHANNELS) {
                        int expected = (other == channel) ? channelValue : new ColorPickerState(base).getChannel(other);
                        assertEquals(expected, state.getChannel(other), "base " + Integer.toHexString(base) + ", " + channel + " = " + channelValue + ", checking " + other);
                    }
                }
            }
        }
    }

    @Test
    void sequentialChannelEditsProduceTheTypedColor() {
        ColorPickerState state = new ColorPickerState(0xFFFFFFFF);
        state.setChannel(ColorPickerState.Channel.RED, 18);
        state.setChannel(ColorPickerState.Channel.GREEN, 52);
        state.setChannel(ColorPickerState.Channel.BLUE, 86);
        state.setChannel(ColorPickerState.Channel.ALPHA, 120);
        assertEquals(0x78123456, state.getArgb());
    }

    @Test
    void channelValuesGetClamped() {
        ColorPickerState state = new ColorPickerState(0xFF808080);
        state.setChannel(ColorPickerState.Channel.RED, 300);
        state.setChannel(ColorPickerState.Channel.GREEN, -20);
        state.setChannel(ColorPickerState.Channel.ALPHA, Integer.MAX_VALUE);
        assertEquals(255, state.getChannel(ColorPickerState.Channel.RED));
        assertEquals(0, state.getChannel(ColorPickerState.Channel.GREEN));
        assertEquals(0x80, state.getChannel(ColorPickerState.Channel.BLUE));
        assertEquals(255, state.getChannel(ColorPickerState.Channel.ALPHA));
    }

    @Test
    void channelEditsReportWhetherTheColorChanged() {
        ColorPickerState state = new ColorPickerState(0xFF102030);
        assertFalse(state.setChannel(ColorPickerState.Channel.RED, 0x10));
        assertFalse(state.setChannel(ColorPickerState.Channel.ALPHA, 999));
        assertTrue(state.setChannel(ColorPickerState.Channel.RED, 0x11));
        assertTrue(state.setChannel(ColorPickerState.Channel.ALPHA, 0));
        assertFalse(state.setChannel(ColorPickerState.Channel.ALPHA, -1));
    }

    @Test
    void unchangedChannelValueDoesNotMovePickerMarkers() {
        ColorPickerState state = new ColorPickerState(0xFFFF0000);
        state.setHue(1.0F);
        state.setSaturationAndValue(0.3333F, 0.6667F);
        float hue = state.getHue();
        float saturation = state.getSaturation();
        float value = state.getValue();
        state.setChannel(ColorPickerState.Channel.GREEN, state.getChannel(ColorPickerState.Channel.GREEN));
        assertEquals(hue, state.getHue());
        assertEquals(saturation, state.getSaturation());
        assertEquals(value, state.getValue());
    }

    @Test
    void alphaEditsKeepHsvValues() {
        ColorPickerState state = new ColorPickerState(0xFFFFFFFF);
        state.setHue(0.4321F);
        state.setSaturationAndValue(0.3337F, 0.777F);
        int rgb = state.getArgb() & 0xFFFFFF;
        assertTrue(state.setChannel(ColorPickerState.Channel.ALPHA, 128));
        assertEquals(0.4321F, state.getHue());
        assertEquals(0.3337F, state.getSaturation());
        assertEquals(0.777F, state.getValue());
        assertEquals(128, state.getChannel(ColorPickerState.Channel.ALPHA));
        assertEquals(rgb, state.getArgb() & 0xFFFFFF);
    }

    @Test
    void grayKeepsPreviousHue() {
        ColorPickerState state = new ColorPickerState(0xFF6464C8);
        float hue = state.getHue();
        assertTrue(state.setChannel(ColorPickerState.Channel.BLUE, 100));
        assertEquals(0xFF646464, state.getArgb());
        assertEquals(hue, state.getHue());
        assertEquals(0.0F, state.getSaturation());
    }

    @Test
    void blackKeepsPreviousHueAndSaturation() {
        ColorPickerState state = new ColorPickerState(0xFF0000C8);
        float hue = state.getHue();
        float saturation = state.getSaturation();
        assertTrue(state.setChannel(ColorPickerState.Channel.BLUE, 0));
        assertEquals(0xFF000000, state.getArgb());
        assertEquals(hue, state.getHue());
        assertEquals(saturation, state.getSaturation());
        assertEquals(0.0F, state.getValue());
        assertTrue(state.setChannel(ColorPickerState.Channel.BLUE, 100));
        assertEquals(0xFF000064, state.getArgb());
        assertEquals(hue, state.getHue());
    }

    @Test
    void redHueAtRightSliderEndStaysThere() {
        ColorPickerState state = new ColorPickerState(0xFFFF0000);
        state.setHue(1.0F);
        assertTrue(state.setChannel(ColorPickerState.Channel.RED, 200));
        assertEquals(0xFFC80000, state.getArgb());
        assertEquals(1.0F, state.getHue());
        assertTrue(state.setChannel(ColorPickerState.Channel.GREEN, 50));
        assertTrue(state.getHue() < 1.0F);
        assertEquals(0xFFC83200, state.getArgb());
    }

    @Test
    void pickerSettersClampAndReportColorChanges() {
        ColorPickerState state = new ColorPickerState(0xFFFF0000);
        assertFalse(state.setHue(-0.5F));
        assertEquals(0.0F, state.getHue());
        assertFalse(state.setHue(1.5F));
        assertEquals(1.0F, state.getHue());
        assertTrue(state.setSaturationAndValue(-1.0F, 2.0F));
        assertEquals(0.0F, state.getSaturation());
        assertEquals(1.0F, state.getValue());
        assertEquals(0xFFFFFFFF, state.getArgb());
        assertFalse(state.setSaturationAndValue(0.0F, 1.0F));
        assertTrue(state.setAlpha(0.5F));
        assertEquals(128, state.getChannel(ColorPickerState.Channel.ALPHA));
        assertTrue(state.setAlpha(-3.0F));
        assertEquals(0, state.getChannel(ColorPickerState.Channel.ALPHA));
    }

    @Test
    void hueChangeOfUnsaturatedColorDoesNotChangeTheColor() {
        ColorPickerState state = new ColorPickerState(0xFF808080);
        assertFalse(state.setHue(0.75F));
        assertEquals(0.75F, state.getHue());
        assertEquals(0xFF808080, state.getArgb());
    }

    @Test
    void channelTextGetsParsedAndClamped() {
        assertEquals(0, ColorPickerState.parseChannelValue("0"));
        assertEquals(7, ColorPickerState.parseChannelValue("007"));
        assertEquals(128, ColorPickerState.parseChannelValue("128"));
        assertEquals(255, ColorPickerState.parseChannelValue("255"));
        assertEquals(255, ColorPickerState.parseChannelValue("256"));
        assertEquals(255, ColorPickerState.parseChannelValue("999"));
        assertEquals(255, ColorPickerState.parseChannelValue("99999999999999999999"));
    }

    @Test
    void nonNumericChannelTextIsRejected() {
        assertEquals(-1, ColorPickerState.parseChannelValue(""));
        assertEquals(-1, ColorPickerState.parseChannelValue("-1"));
        assertEquals(-1, ColorPickerState.parseChannelValue("+5"));
        assertEquals(-1, ColorPickerState.parseChannelValue("1a"));
        assertEquals(-1, ColorPickerState.parseChannelValue(" 1"));
    }

}
