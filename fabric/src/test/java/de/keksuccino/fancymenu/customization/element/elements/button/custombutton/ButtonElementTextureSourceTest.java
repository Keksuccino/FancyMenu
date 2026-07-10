package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ButtonElementTextureSourceTest {

    @Test
    void textureUpdateResolvesOnePropertySourceForEveryTexturePath() {
        CountingButtonElement element = new CountingButtonElement();
        element.setWidget(null);

        element.updateWidgetTexture();

        assertEquals(1, element.propertySourceResolutions);
        assertEquals(8, element.textureResolutions);
    }

    @Test
    void legacySliderTextureUpdateResolvesOnePropertySourceForEveryTexturePath() {
        CountingButtonElement element = new CountingButtonElement();
        element.setWidget(new RangeSlider(0, 0, 100, 20, Component.empty(), 0.0D, 1.0D, 0.5D));

        element.updateWidgetTexture();

        assertEquals(1, element.propertySourceResolutions);
        assertEquals(8, element.textureResolutions);
    }

    private static final class CountingButtonElement extends ButtonElement {

        private int propertySourceResolutions;
        private int textureResolutions;

        private CountingButtonElement() {
            super(new ButtonElementBuilder());
            CountingTextureSupplier supplier = new CountingTextureSupplier(this);
            this.backgroundTextureNormal = supplier;
            this.backgroundTextureHover = supplier;
            this.backgroundTextureInactive = supplier;
            this.iconTextureNormal = supplier;
            this.iconTextureHover = supplier;
            this.iconTextureInactive = supplier;
            this.sliderBackgroundTextureNormal = supplier;
            this.sliderBackgroundTextureHighlighted = supplier;
        }

        @Override
        public ButtonElement getPropertySource() {
            this.propertySourceResolutions++;
            return this;
        }

    }

    private static final class CountingTextureSupplier extends ResourceSupplier<ITexture> {

        private final CountingButtonElement element;

        private CountingTextureSupplier(CountingButtonElement element) {
            super(ITexture.class, FileMediaType.IMAGE, "");
            this.element = element;
        }

        @Override
        public ITexture get() {
            this.element.textureResolutions++;
            return null;
        }

    }

}
