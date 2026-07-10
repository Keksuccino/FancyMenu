package de.keksuccino.fancymenu.customization.element.elements.slider.v2;

import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElementBuilder;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SliderElementTextureSourceTest {

    @Test
    void textureUpdateResolvesOnePropertySourceForEveryTexturePath() {
        ButtonElement template = new ButtonElement(new ButtonElementBuilder());
        CountingTextureSupplier templateSupplier = new CountingTextureSupplier();
        setTemplateTextureSuppliers(template, templateSupplier);
        CountingSliderElement element = new CountingSliderElement(template);
        CountingTextureSupplier localSupplier = new CountingTextureSupplier();
        element.setLocalTextureSuppliers(localSupplier);
        element.propertySourceResolutions = 0;

        element.updateWidgetTexture();

        assertEquals(1, element.propertySourceResolutions);
        assertEquals(5, templateSupplier.resolutions);
        assertEquals(0, localSupplier.resolutions);
    }

    @Test
    void textureUpdateFallsBackToLocalTextureSuppliersWithoutTemplate() {
        CountingSliderElement element = new CountingSliderElement(null);
        CountingTextureSupplier localSupplier = new CountingTextureSupplier();
        element.setLocalTextureSuppliers(localSupplier);
        element.propertySourceResolutions = 0;

        element.updateWidgetTexture();

        assertEquals(1, element.propertySourceResolutions);
        assertEquals(5, localSupplier.resolutions);
    }

    private static final class CountingSliderElement extends SliderElement {

        private final ButtonElement propertySource;
        private int propertySourceResolutions;

        private CountingSliderElement(ButtonElement propertySource) {
            super(new SliderElementBuilder());
            this.propertySource = propertySource;
        }

        private void setLocalTextureSuppliers(ResourceSupplier<ITexture> supplier) {
            this.sliderBackgroundTextureNormal = supplier;
            this.sliderBackgroundTextureHighlighted = supplier;
            this.handleTextureNormal = supplier;
            this.handleTextureHover = supplier;
            this.handleTextureInactive = supplier;
        }

        @Override
        public ButtonElement getPropertySource() {
            this.propertySourceResolutions++;
            return this.propertySource;
        }

        @Override
        public int getPositioningScreenWidth() {
            return 320;
        }

        @Override
        public int getPositioningScreenHeight() {
            return 240;
        }

        @Override
        public int getAbsoluteX() {
            return 0;
        }

        @Override
        public int getAbsoluteY() {
            return 0;
        }

        @Override
        public int getAbsoluteWidth() {
            return 100;
        }

        @Override
        public int getAbsoluteHeight() {
            return 20;
        }

    }

    private static final class CountingTextureSupplier extends ResourceSupplier<ITexture> {

        private int resolutions;

        private CountingTextureSupplier() {
            super(ITexture.class, FileMediaType.IMAGE, "");
        }

        @Override
        public ITexture get() {
            this.resolutions++;
            return null;
        }

    }

    private static void setTemplateTextureSuppliers(ButtonElement template, ResourceSupplier<ITexture> supplier) {
        template.sliderBackgroundTextureNormal = supplier;
        template.sliderBackgroundTextureHighlighted = supplier;
        template.backgroundTextureNormal = supplier;
        template.backgroundTextureHover = supplier;
        template.backgroundTextureInactive = supplier;
    }

}
