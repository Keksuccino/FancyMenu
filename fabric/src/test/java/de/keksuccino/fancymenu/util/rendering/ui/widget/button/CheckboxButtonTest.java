package de.keksuccino.fancymenu.util.rendering.ui.widget.button;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckboxButtonTest {

    @Test
    void textureRenderAlphaCarriesWidgetFadeOpacity() {
        assertEquals(0.0F, CheckboxButton.getTextureRenderAlpha(0.0F));
        assertEquals(0.02F, CheckboxButton.getTextureRenderAlpha(0.02F));
        assertEquals(0.5F, CheckboxButton.getTextureRenderAlpha(0.5F));
        assertEquals(1.0F, CheckboxButton.getTextureRenderAlpha(1.0F));
    }

}
