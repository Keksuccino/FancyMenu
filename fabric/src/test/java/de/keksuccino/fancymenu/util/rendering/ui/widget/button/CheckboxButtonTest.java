package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckboxButtonTest {

    @Test
    void textureRenderColorCarriesWidgetFadeOpacity() {
        assertEquals(0x00FFFFFF, CheckboxButton.getTextureRenderColor(0.0F));
        assertEquals(0x05FFFFFF, CheckboxButton.getTextureRenderColor(0.02F));
        assertEquals(0x7FFFFFFF, CheckboxButton.getTextureRenderColor(0.5F));
        assertEquals(0xFFFFFFFF, CheckboxButton.getTextureRenderColor(1.0F));
    }

}
