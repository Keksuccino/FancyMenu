package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.elements.MarkdownSettings;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownTestRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollGrabberDesignTest {

    @Test
    void themeBarHasNoTexturesAndThePixelHandleHasOneForEveryGrabberAndState() throws IOException {
        Set<ResourceLocation> textures = new HashSet<>();
        for (boolean vertical : new boolean[]{true, false}) {
            for (boolean hovered : new boolean[]{true, false}) {
                assertNull(ScrollGrabberDesign.THEME_BAR.getTexture(vertical, hovered));
                ResourceLocation texture = ScrollGrabberDesign.PIXEL_HANDLE.getTexture(vertical, hovered);
                assertNotNull(texture);
                assertTrue(textures.add(texture), texture::toString);
                assertTrue(ScrollGrabberDesign.isDesignTexture(texture));
                try (InputStream stream = ScrollGrabberDesignTest.class.getResourceAsStream("/assets/" + texture.getNamespace() + "/" + texture.getPath())) {
                    assertNotNull(stream, () -> "missing texture: " + texture);
                }
            }
        }
        assertFalse(ScrollGrabberDesign.isDesignTexture(null));
        assertFalse(ScrollGrabberDesign.isDesignTexture(ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/some_grabber_of_the_user.png")));
    }

    @Test
    void whatTheUserSetForTheGrabbersWinsOverTheDesign() {
        assertEquals(ScrollGrabberDesign.PIXEL_HANDLE, ScrollGrabberDesign.PIXEL_HANDLE.unlessCustomized(false));
        assertEquals(ScrollGrabberDesign.THEME_BAR, ScrollGrabberDesign.PIXEL_HANDLE.unlessCustomized(true));
        assertEquals(ScrollGrabberDesign.THEME_BAR, ScrollGrabberDesign.THEME_BAR.unlessCustomized(false));
    }

    @Test
    void onlyThePixelLookComesWithThePixelHandle() {
        assertEquals(ScrollGrabberDesign.PIXEL_HANDLE, ScrollGrabberDesign.ofLook(MarkdownLook.PIXEL));
        assertEquals(ScrollGrabberDesign.THEME_BAR, ScrollGrabberDesign.ofLook(MarkdownLook.CLASSIC));
        assertEquals(ScrollGrabberDesign.THEME_BAR, ScrollGrabberDesign.ofLook(MarkdownLook.NEUTRAL));
    }

    @Test
    void designSurvivesSavingAndLoading() {
        for (ScrollGrabberDesign design : ScrollGrabberDesign.values()) {
            TestElement element = new TestElement();
            element.design.set(design);
            SerializedElement saved = new SerializedElement();
            element.getPropertyMap().forEach((key, property) -> property.serialize(saved));

            TestElement loaded = new TestElement();
            loaded.getPropertyMap().forEach((key, property) -> property.deserialize(saved));

            assertTrue(saved.hasProperty(ScrollGrabberDesign.PROPERTY_KEY));
            assertEquals(design, loaded.design.get());
        }
    }

    @Test
    void elementsSavedWithoutADesignKeepTheThemeBar() {
        TestElement loaded = new TestElement();
        loaded.design.set(ScrollGrabberDesign.PIXEL_HANDLE);

        loaded.getPropertyMap().forEach((key, property) -> property.deserialize(new SerializedElement()));

        assertEquals(ScrollGrabberDesign.THEME_BAR, loaded.design.get());
    }

    @Test
    void applyingALookPicksItsDesignAndTheDesignStaysChangeableAfterwards() {
        TestElement element = new TestElement();
        assertEquals(ScrollGrabberDesign.THEME_BAR, element.design.get());

        element.settings.applyLook(MarkdownLook.PIXEL);
        assertEquals(ScrollGrabberDesign.PIXEL_HANDLE, element.design.get());

        element.settings.applyLook(MarkdownLook.NEUTRAL);
        assertEquals(ScrollGrabberDesign.THEME_BAR, element.design.get());

        element.settings.applyLook(MarkdownLook.PIXEL);
        element.settings.applyLook(MarkdownLook.CLASSIC);
        assertEquals(ScrollGrabberDesign.THEME_BAR, element.design.get());

        element.design.set(ScrollGrabberDesign.PIXEL_HANDLE);
        element.settings.tick();
        assertEquals(ScrollGrabberDesign.PIXEL_HANDLE, element.design.get());
        assertEquals(MarkdownLook.CLASSIC, element.settings.getLook());
    }

    @Test
    void newElementsStartWithThePixelHandle() {
        TestElement element = new TestElement();

        element.settings.applyDefaultsOfNewElements();

        assertEquals(ScrollGrabberDesign.PIXEL_HANDLE, element.design.get());
    }

    /**
     * The properties a text element has for its grabber design and its Markdown settings, wired together the way the element does it.
     */
    private static final class TestElement implements PropertyHolder {

        private final Map<String, Property<?>> properties = new LinkedHashMap<>();
        private final Property<ScrollGrabberDesign> design = this.putProperty(ScrollGrabberDesign.buildProperty());
        private final MarkdownSettings settings = new MarkdownSettings(this, new MarkdownTestRenderer());

        private TestElement() {
            ScrollGrabberDesign.followAppliedLooks(this.design, this.settings);
        }

        @Override
        public @NotNull Map<String, Property<?>> getPropertyMap() {
            return this.properties;
        }

    }

}
